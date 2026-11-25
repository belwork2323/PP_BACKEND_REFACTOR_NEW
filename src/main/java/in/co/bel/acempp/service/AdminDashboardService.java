package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.batch.*;
import in.co.bel.acempp.dto.dashboard.*;
import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.batch.BlockchainEvent;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.BlockchainEventRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final UserRepository userRepo;
    private final BatchRepository batchRepo;
    private final BatchProgressCalculator batchProgressCalculator;
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yy");
    private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    // ─────────────────────────────────────────────────────────
    // 1. DASHBOARD STATS
    // ─────────────────────────────────────────────────────────
    public DashboardStatsResponse getStats(DashboardStatsRequest request) {

        DateRange range = resolveDateRange(
                request.getFilterType(),
                request.getStartDate(),
                request.getEndDate());

        LocalDateTime start = range.start();
        LocalDateTime end = range.end();

        // Users
        long totalUsers = userRepo.countByCreatedAtBetween(start, end);
        long activeUsers = userRepo.countByStatusAndCreatedAtBetween(
                Status.ACTIVE, start, end);

        // Completed batches
        long completedBatches = batchRepo.countDispatchedBetween(start, end);

        // Open batches
        long openBatches = batchRepo.countOpenBatchesBetween(start, end);

        // Motors dispatched
//        long motorsDispatched = batchRepo.sumMotorsDispatchedBetween(start, end);

        return DashboardStatsResponse.builder()
                .totalUsers(
                        StatMetric.builder()
                                .count(totalUsers)
                                .subValue(totalUsers)
                                .build())
                .activeUsers(
                        StatMetric.builder()
                                .count(activeUsers)
                                .subValue(activeUsers)
                                .build())
                .completedBatches(
                        StatMetric.builder()
                                .count(completedBatches)
                                .subValue(completedBatches)
                                .build())
                .openBatches(
                        StatMetric.builder()
                                .count(openBatches)
                                .subValue(openBatches)
                                .build())
//                .motorsDispatched(
//                        StatMetric.builder()
//                                .count(motorsDispatched)
//                                .subValue(motorsDispatched)
//                                .build())
                .build();
    }

    public ChartDataResponse getChartData(DashboardStatsRequest request) {
        if (!"custom".equalsIgnoreCase(request.getFilterType())) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Invalid filter type. Only 'custom' is supported.");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "startDate and endDate are required for custom filter.");
        }

        LocalDate startDate = LocalDate.parse(request.getStartDate(), INPUT_DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), INPUT_DATE_FORMATTER);

        if (endDate.isBefore(startDate)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "endDate cannot be before startDate.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Batch> batches = batchRepo.findByCreatedAtBetweenAndIsDeletedFalse(
                startDateTime, endDateTime);

        long spanDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        if (spanDays <= 31) {
            return buildDailyChart(batches, startDate, endDate);
        } else if (spanDays <= 90) {
            return buildWeeklyChart(batches, startDate, endDate);
        } else {
            return buildMonthlyChart(batches, startDate, endDate);
        }
    }

    public ChartDataResponse buildDailyChart(
            List<Batch> batches, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<Batch>> batchesByDay = batches.stream()
                .filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.groupingBy(b -> b.getCreatedAt().toLocalDate()));

        List<ChartPoint> activity = new ArrayList<>();
        List<ChartPoint> motors = new ArrayList<>();
        List<ChartPoint> qcRate = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Batch> dayBatches = batchesByDay.getOrDefault(date, List.of());
            appendMetrics(activity, motors, qcRate, date.format(DAY_LABEL_FORMATTER), dayBatches);
        }

        return ChartDataResponse.builder()
                .filterType("custom")
                .timeUnit("day")
                .weeklyActivity(activity)
                .motorsProcessed(motors)
                .qcPassRate(qcRate)
                .build();
    }

    public ChartDataResponse buildWeeklyChart(
            List<Batch> batches, LocalDate startDate, LocalDate endDate) {
        List<ChartPoint> activity = new ArrayList<>();
        List<ChartPoint> motors = new ArrayList<>();
        List<ChartPoint> qcRate = new ArrayList<>();

        LocalDate bucketStart = startDate;
        while (!bucketStart.isAfter(endDate)) {
            LocalDate bucketEnd = bucketStart.plusDays(6);
            if (bucketEnd.isAfter(endDate)) {
                bucketEnd = endDate;
            }

            final LocalDate from = bucketStart;
            final LocalDate to = bucketEnd;

            List<Batch> weekBatches = batches.stream()
                    .filter(b -> b.getCreatedAt() != null)
                    .filter(b -> {
                        LocalDate created = b.getCreatedAt().toLocalDate();
                        return !created.isBefore(from) && !created.isAfter(to);
                    })
                    .toList();

            String label = from.format(DAY_LABEL_FORMATTER) + " - " + to.format(DAY_LABEL_FORMATTER);
            appendMetrics(activity, motors, qcRate, label, weekBatches);

            bucketStart = bucketEnd.plusDays(1);
        }

        return ChartDataResponse.builder()
                .filterType("custom")
                .timeUnit("week")
                .weeklyActivity(activity)
                .motorsProcessed(motors)
                .qcPassRate(qcRate)
                .build();
    }

    public ChartDataResponse buildMonthlyChart(
            List<Batch> batches, LocalDate startDate, LocalDate endDate) {
        Map<YearMonth, List<Batch>> batchesByMonth = batches.stream()
                .filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.groupingBy(b -> YearMonth.from(b.getCreatedAt())));

        List<ChartPoint> activity = new ArrayList<>();
        List<ChartPoint> motors = new ArrayList<>();
        List<ChartPoint> qcRate = new ArrayList<>();

        YearMonth current = YearMonth.from(startDate);
        YearMonth last = YearMonth.from(endDate);

        while (!current.isAfter(last)) {
            List<Batch> monthBatches = batchesByMonth.getOrDefault(current, List.of());
            appendMetrics(activity, motors, qcRate, current.atDay(1).format(MONTH_LABEL_FORMATTER), monthBatches);
            current = current.plusMonths(1);
        }

        return ChartDataResponse.builder()
                .filterType("custom")
                .timeUnit("month")
                .weeklyActivity(activity)
                .motorsProcessed(motors)
                .qcPassRate(qcRate)
                .build();
    }

    public void appendMetrics(
            List<ChartPoint> activity,
            List<ChartPoint> motors,
            List<ChartPoint> qcRate,
            String label,
            List<Batch> bucketBatches) {
        long activityCount = bucketBatches.size();
        long motorCount = bucketBatches.stream()
                .filter(this::isDispatched)
                .mapToLong(b -> b.getMotorIds() != null ? b.getMotorIds().size() : 0)
                .sum();
        long qcTotal = bucketBatches.size();
        long qcPass = bucketBatches.stream().filter(this::isQcPassed).count();
        long qcRateVal = qcTotal > 0 ? (qcPass * 100 / qcTotal) : 0;

        activity.add(ChartPoint.builder().label(label).value(activityCount).build());
        motors.add(ChartPoint.builder().label(label).value(motorCount).build());
        qcRate.add(ChartPoint.builder().label(label).value(qcRateVal).build());
    }


    // ─────────────────────────────────────────────────────────
    // 3. ACTIVE BATCHES
    // ─────────────────────────────────────────────────────────
    public ActiveBatchesResponse getActiveBatches(ActiveBatchesRequest request) {

        LocalDateTime start = null;
        LocalDateTime end = null;

        if (request.isCurrentMonth()) {

            LocalDate today = LocalDate.now();

            start = today.withDayOfMonth(1).atStartOfDay();
            end = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

        } else {
            start = parseNullableDate(request.getStartDate());
            LocalDateTime parsedEnd = parseNullableDate(request.getEndDate());
            end = (parsedEnd != null) ? parsedEnd.toLocalDate().atTime(LocalTime.MAX) : null;

            if ((start == null) != (end == null)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Both startDate and endDate must be provided."
                );
            }

            if (start != null && end != null && end.isBefore(start)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "End date cannot be before start date."
                );
            }
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage() - 1),
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Batch> page = batchRepo.findActiveBatches(
                request.getSearch(),
                request.getStage(),
                request.getType(),
                request.getStatus(),
                request.getListType(),
                start,
                end,
                request.isCurrentMonth(),
                pageable);

        List<ActiveBatchItemDto> dtos = page.getContent().stream()
                .map(batch -> {

                    String motorId = (batch.getMotorIds() != null && !batch.getMotorIds().isEmpty())
                            ? batch.getMotorIds().get(0)
                            : null;

                    String systemManagerId = batch.getSystemManager() != null
                            ? batch.getSystemManager().getId()
                            : null;

                    int completion = batchProgressCalculator.calculate(batch);

                    String createdDate = batch.getCreatedAt() != null
                            ? batch.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            : null;

                    return ActiveBatchItemDto.builder()
                            .batchId(batch.getBatchId())
                            .batchType(batch.getBatchType())
                            .motorId(motorId)
                            .projectName(batch.getProjectName())
                            .currentStage(batch.getCurrentStage() != null
                                    ? batch.getCurrentStage()
                                    : List.of())
                            .stageProgress(batch.getStageProgress() != null
                                    ? batch.getStageProgress()
                                    : List.of())
                            .systemManager(batch.getSystemManager())
                            .date(createdDate)
                            .completion(completion)
                            .build();
                })
                .collect(Collectors.toList());

        return ActiveBatchesResponse.builder()
                .batches(dtos)
                .pagination(PaginationDto.builder()
                        .page(request.getPage())
                        .pageSize(request.getPageSize())
                        .totalRecords(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // 4. BLOCKCHAIN EVENTS
    // ─────────────────────────────────────────────────────────

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }

    private DateRange resolveDateRange(String filterType,
                                       String startDate, String endDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end = now;

        start = switch (filterType) {
            case "day" -> now.toLocalDate().atStartOfDay();
            case "week" -> now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay();
            case "month" -> now.toLocalDate().withDayOfMonth(1).atStartOfDay();
            case "custom" -> {
                if (startDate == null || startDate.isBlank())
                    throw new AcemException(ErrorCode.INVALID_REQUEST,
                            "startDate is required for custom filter type");
                yield DateTimeConversionUtil.parseLocalDate(startDate).atStartOfDay();
            }
            default -> throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Invalid filter type. Allowed values are day, week, month, or custom");
        };

        // For non-custom types, still allow optional date override
        if (!"custom".equals(filterType)) {
            if (startDate != null && !startDate.isBlank())
                start = DateTimeConversionUtil.parseLocalDate(startDate).atStartOfDay();
        }

        if (endDate != null && !endDate.isBlank())
            end = DateTimeConversionUtil.parseLocalDate(endDate).atTime(23, 59, 59);

        if (start.isAfter(end))
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "startDate cannot be greater than endDate");

        return new DateRange(start, end);
    }

    private boolean isDispatched(Batch batch) {
        return batch.getStageProgress() != null && batch.getStageProgress().stream()
                .anyMatch(sp -> sp.getSubDepartmentId().equals(13L) &&
                        sp.getStatus() == Status.COMPLETELY_APPROVED);
    }

    private boolean isQcPassed(Batch batch) {
        return batch.getStageProgress() != null && batch.getStageProgress().stream()
                .anyMatch(sp -> sp.getSubDepartmentId().equals(12L) &&
                        sp.getStatus() == Status.COMPLETELY_APPROVED);
    }

    private ChartDataResponse buildDayChart(List<Batch> batches, DateRange range) {
        int[] hours = {8, 10, 12, 14, 16, 18};
        List<ChartPoint> activity = new ArrayList<>();
        List<ChartPoint> motors = new ArrayList<>();
        List<ChartPoint> qcRate = new ArrayList<>();

        for (int hour : hours) {
            String label = String.format("%02d:00", hour);
            List<Batch> hourBatches = batches.stream()
                    .filter(b -> b.getCreatedAt().getHour() == hour)
                    .toList();

            long activityCount = hourBatches.size();
            long motorCount = hourBatches.stream()
                    .filter(this::isDispatched)
                    .mapToLong(b -> b.getMotorIds().size())
                    .sum();
            long qcTotal = hourBatches.size();
            long qcPass = hourBatches.stream().filter(this::isQcPassed).count();
            long qcRateVal = qcTotal > 0 ? (qcPass * 100 / qcTotal) : 0;

            activity.add(ChartPoint.builder().label(label).value(activityCount).build());
            motors.add(ChartPoint.builder().label(label).value(motorCount).build());
            qcRate.add(ChartPoint.builder().label(label).value(qcRateVal).build());
        }

        return ChartDataResponse.builder()
                .filterType("day").timeUnit("hour")
                .weeklyActivity(activity)
                .motorsProcessed(motors)
                .qcPassRate(qcRate)
                .build();
    }

    private ChartDataResponse buildWeekChart(List<Batch> batches, DateRange range) {
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        DayOfWeek[] dowList = {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY};
        List<ChartPoint> activity = new ArrayList<>();
        List<ChartPoint> motors = new ArrayList<>();
        List<ChartPoint> qcRate = new ArrayList<>();

        for (int i = 0; i < days.length; i++) {
            DayOfWeek dow = dowList[i];
            String label = days[i];
            List<Batch> dowBatches = batches.stream()
                    .filter(b -> b.getCreatedAt().getDayOfWeek() == dow)
                    .toList();

            long activityCount = dowBatches.size();
            long motorCount = dowBatches.stream()
                    .filter(this::isDispatched)
                    .mapToLong(b -> b.getMotorIds().size())
                    .sum();
            long qcTotal = dowBatches.size();
            long qcPass = dowBatches.stream().filter(this::isQcPassed).count();
            long qcRateVal = qcTotal > 0 ? (qcPass * 100 / qcTotal) : 0;

            activity.add(ChartPoint.builder().label(label).value(activityCount).build());
            motors.add(ChartPoint.builder().label(label).value(motorCount).build());
            qcRate.add(ChartPoint.builder().label(label).value(qcRateVal).build());
        }

        return ChartDataResponse.builder()
                .filterType("week").timeUnit("day")
                .weeklyActivity(activity)
                .motorsProcessed(motors)
                .qcPassRate(qcRate)
                .build();
    }

    private ChartDataResponse buildMonthChart(List<Batch> batches, DateRange range) {
        List<ChartPoint> activity = new ArrayList<>();
        List<ChartPoint> motors = new ArrayList<>();
        List<ChartPoint> qcRate = new ArrayList<>();

        for (int week = 1; week <= 4; week++) {
            String label = "Week " + week;
            int w = week;
            List<Batch> weekBatches = batches.stream()
                    .filter(b -> weekOfMonth(b.getCreatedAt().getDayOfMonth()) == w)
                    .toList();

            long activityCount = weekBatches.size();
            long motorCount = weekBatches.stream()
                    .filter(this::isDispatched)
                    .mapToLong(b -> b.getMotorIds().size())
                    .sum();
            long qcTotal = weekBatches.size();
            long qcPass = weekBatches.stream().filter(this::isQcPassed).count();
            long qcRateVal = qcTotal > 0 ? (qcPass * 100 / qcTotal) : 0;

            activity.add(ChartPoint.builder().label(label).value(activityCount).build());
            motors.add(ChartPoint.builder().label(label).value(motorCount).build());
            qcRate.add(ChartPoint.builder().label(label).value(qcRateVal).build());
        }

        return ChartDataResponse.builder()
                .filterType("month").timeUnit("week")
                .weeklyActivity(activity)
                .motorsProcessed(motors)
                .qcPassRate(qcRate)
                .build();
    }

    // existing weekOfMonth helper (unchanged)
    private int weekOfMonth(int dayOfMonth) {
        if (dayOfMonth <= 7) return 1;
        if (dayOfMonth <= 14) return 2;
        if (dayOfMonth <= 21) return 3;
        return 4;
    }

    private LocalDateTime parseNullableDate(String date) {
        if (date == null || date.isBlank()) return null;
        return DateTimeConversionUtil.parseLocalDate(date).atStartOfDay();
    }

//    private ChartDataResponse buildCustomChart(List<Batch> batches, DateRange range) {
//
//        List<ChartPoint> activity = new ArrayList<>();
//        List<ChartPoint> motors = new ArrayList<>();
//        List<ChartPoint> qcRate = new ArrayList<>();
//
//        LocalDate start = range.start().toLocalDate();
//        LocalDate end = range.end().toLocalDate();
//
//        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
//
//            LocalDate current = date;
//
//            List<Batch> dayBatches = batches.stream()
//                    .filter(b -> b.getCreatedAt().toLocalDate().equals(current))
//                    .toList();
//
//            long activityCount = dayBatches.size();
//
//            long motorCount = dayBatches.stream()
//                    .filter(this::isDispatched)
//                    .mapToLong(b -> b.getMotorIds().size())
//                    .sum();
//
//            long qcPass = dayBatches.stream()
//                    .filter(this::isQcPassed)
//                    .count();
//
//            long qcRateValue = activityCount > 0
//                    ? (qcPass * 100 / activityCount)
//                    : 0;
//
//            String label = current.format(DateTimeFormatter.ofPattern("dd-MM"));
//
//            activity.add(ChartPoint.builder()
//                    .label(label)
//                    .value(activityCount)
//                    .build());
//
//            motors.add(ChartPoint.builder()
//                    .label(label)
//                    .value(motorCount)
//                    .build());
//
//            qcRate.add(ChartPoint.builder()
//                    .label(label)
//                    .value(qcRateValue)
//                    .build());
//        }
//
//        return ChartDataResponse.builder()
//                .filterType("custom")
//                .timeUnit("day")
//                .weeklyActivity(activity)
//                .motorsProcessed(motors)
//                .qcPassRate(qcRate)
//                .build();
//    }


//    private UserListItemDto mapToUserListItem(UserCredentials user) {
//
//        List<UserDeptRole> deptRoles = userDeptRoleRepo.findAllByUserId(user);
//
//        // Role — from first row
//        RolesDto roleDto = null;
//        if (!deptRoles.isEmpty() && deptRoles.get(0).getRole() != null) {
//            Roles r = deptRoles.get(0).getRole();
//            roleDto = new RolesDto(r.getId(), r.getRole());
//        }
//
//        // Group sub-departments by parent department (reuse same logic everywhere)
//        List<SubDepartment> subDepts = deptRoles.stream()
//                .map(UserDeptRole::getSubDepartmentId)
//                .filter(sd -> sd != null)
//                .toList();
//
//        List<DepartmentWithSubDeptsDto> groupedDepts = groupByDepartment(subDepts);
//
//        return UserListItemDto.builder()
//                .userId(user.getUserId())
//                .username(user.getUsername())
//                .role(roleDto)
//                .department(groupedDepts)   // ← nested grouped
//                .status(user.isActive() ? "Active" : "Inactive")
//                .build();
//    }
//
//    // Same grouping helper used in AuthService — copy here too
//    private List<DepartmentWithSubDeptsDto> groupByDepartment(List<SubDepartment> subDepts) {
//        Map<Long, DepartmentWithSubDeptsDto> map = new LinkedHashMap<>();
//
//        for (SubDepartment sd : subDepts) {
//            Department dept  = sd.getDepartment();
//            Long       deptId = dept.getId();
//
//            map.putIfAbsent(deptId, DepartmentWithSubDeptsDto.builder()
//                    .departmentId(deptId)
//                    .departmentName(dept.getDepartment())
//                    .subDepartments(new ArrayList<>())
//                    .build());
//
//            map.get(deptId).getSubDepartments().add(
//                    SubDeptDto.builder()
//                            .subDepartmentId(sd.getId())
//                            .subDepartmentName(sd.getSubDepartment())
//                            .build());
//        }
//
//        return new ArrayList<>(map.values());
//    }
}
