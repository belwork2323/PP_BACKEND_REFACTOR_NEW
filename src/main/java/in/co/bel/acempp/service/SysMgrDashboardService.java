package in.co.bel.acempp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.bel.acempp.dto.batch.StatMetric;
import in.co.bel.acempp.dto.batch.batchdetails.*;
import in.co.bel.acempp.dto.dashboard.*;
import in.co.bel.acempp.dto.dashboard.systemmanagerdashboard.*;
import in.co.bel.acempp.dto.dashboard.systemmanagerdashboard.ManagerActiveBatchesRequest;
import in.co.bel.acempp.dto.dashboard.systemmanagerdashboard.StageProcessedDto;
import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.dispatch.DispatchMotor;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads.DispatchDetails;
import in.co.bel.acempp.dto.statictestingfacility.StaticMotorData;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.StaticTestingDetails;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.batch.BatchAlert;
import in.co.bel.acempp.entity.batch.BlockchainEvent;
import in.co.bel.acempp.entity.qualitycontrol.DivisionDetail;
import in.co.bel.acempp.entity.qualitycontrol.QcDivision;
import in.co.bel.acempp.entity.qualitycontrol.StaticTesting;
import in.co.bel.acempp.entity.DepartmentMaster;
import in.co.bel.acempp.entity.SubDepartmentMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.*;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionRepository;
import in.co.bel.acempp.repository.qualitycontrol.StaticTestingRepository;
import in.co.bel.acempp.service.batch.StageProgressDisplayEnricher;
import in.co.bel.acempp.service.systemmanager.SubDepartmentFormProvider;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMgrDashboardService {

    private final SystemManagerDashboardRepository dashboardRepo;
    private final SysMgrBatchRepository sysMgrBatchRepository;
    private final DepartmentRepository departmentRepository;
    private final SysMgrBlockchainEventRepository blockchainEventRepo;
    private final SysMgrAlertsRepository alertRepo;
    private final BatchRepository batchRepo;
    private final BatchStageSequence stageSequence;
    private final QcDivisionRepository qcDivisionRepo;
    private final StaticTestingRepository staticTestingRepo;
    private final Map<Long, SubDepartmentFormProvider> formProviders;
    private final BatchProgressCalculator batchProgressCalculator;
    private final StageProgressDisplayEnricher stageProgressDisplayEnricher;
    private final BatchFormResolver batchFormResolver;
    private final ObjectMapper objectMapper;

    private LocalDateTime parseNullableDate(String date) {
        if (date == null || date.isBlank()) return null;
        return DateTimeConversionUtil.parseLocalDate(date).atStartOfDay();
    }

//1. stats
    public ManagerDashboardStatsResponse getStats(
            ManagerDashboardStatsRequest request) {

        DateRange range = resolveDateRange(
                request.getFilterType(),
                request.getStartDate(),
                request.getEndDate()
        );

        LocalDateTime start = range.start();
        LocalDateTime end = range.end();

        long totalBatches = dashboardRepo.countTotalBatches(start, end);

        long inProgressBatches = dashboardRepo.countInProgressBatches(start, end);

        long completedBatches = dashboardRepo.countCompletedBatches(start, end);

        long pendingApprovals = dashboardRepo.countPendingApprovals(start, end);

        long rejectedBatches = dashboardRepo.countRejectedBatches(start, end);

        return ManagerDashboardStatsResponse.builder()
                .totalBatches(
                        StatMetric.builder()
                                .count(totalBatches)
                                .subValue(totalBatches)
                                .build()
                )
                .inProgressBatches(
                        StatMetric.builder()
                                .count(inProgressBatches)
                                .subValue(inProgressBatches)
                                .build()
                )
                .completedBatches(
                        StatMetric.builder()
                                .count(completedBatches)
                                .subValue(completedBatches)
                                .build()
                )
                .pendingApprovals(
                        StatMetric.builder()
                                .count(pendingApprovals)
                                .subValue(pendingApprovals)
                                .build()
                )
                .rejectedBatches(
                        StatMetric.builder()
                                .count(rejectedBatches)
                                .subValue(rejectedBatches)
                                .build()
                )
                .build();
    }

    private record DateRange(
            LocalDateTime start,
            LocalDateTime end
    ) {}

    private DateRange resolveDateRange(
            String filterType,
            String startDate,
            String endDate) {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime start;
        LocalDateTime end = now;

        start = switch (filterType.toLowerCase()) {

            case "day" ->
                    now.toLocalDate().atStartOfDay();

            case "week" ->
                    now.toLocalDate()
                            .with(DayOfWeek.MONDAY)
                            .atStartOfDay();

            case "month" ->
                    now.toLocalDate()
                            .withDayOfMonth(1)
                            .atStartOfDay();

            case "custom" -> {

                if (startDate == null || startDate.isBlank()) {
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "startDate is required for custom filter"
                    );
                }

                yield DateTimeConversionUtil.parseLocalDate(startDate).atStartOfDay();
            }

            default ->
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "Invalid filter type"
                    );
        };

        if (startDate != null && !startDate.isBlank()) {
            start = DateTimeConversionUtil.parseLocalDate(startDate).atStartOfDay();
        }

        if (endDate != null && !endDate.isBlank()) {
            end = DateTimeConversionUtil.parseLocalDate(endDate).atTime(23, 59, 59);
        }

        if (start.isAfter(end)) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "startDate cannot be greater than endDate"
            );
        }

        return new DateRange(start, end);
    }

    //2. chart data
    public ManagerDashboardChartResponse getChartData(
            ManagerDashboardChartRequest request) {

        DateRange range = resolveDateRange(
                request.getFilterType(),
                request.getStartDate(),
                request.getEndDate()
        );

        List<Batch> batches = dashboardRepo.findBatchesBetween(
                range.start(),
                range.end()
        );

        long totalActiveBatches = batches.stream()
                .filter(batch ->
                        batch.getStageProgress() != null &&
                                batch.getStageProgress().stream()
                                        .anyMatch(stage ->
                                                stage.getSubDepartmentId() >= 3
                                                        && stage.getStatus() != Status.TO_BE_INITIATED))
                .count();

        List<ChartPointDto> weeklyActivity;
        List<ChartPointDto> motorsProcessed;

        switch (request.getFilterType().toLowerCase()) {

            case "day", "custom" -> {
                weeklyActivity = buildHourlyBatchChart(batches);
                motorsProcessed = buildHourlyMotorChart(batches);
            }
            case "week" -> {
                weeklyActivity = buildDailyBatchChart(batches);
                motorsProcessed = buildDailyMotorChart(batches);
            }

            case "month" -> {
                weeklyActivity = buildWeeklyBatchChart(batches);
                motorsProcessed = buildWeeklyMotorChart(batches);
            }

            default -> throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid filter type. Allowed values are day, week, or month"
            );
        }

        return ManagerDashboardChartResponse.builder()
                .filterType(request.getFilterType())
                .timeUnit(getTimeUnit(request.getFilterType()))
                .totalActiveBatches(totalActiveBatches)
                .weeklyActivity(weeklyActivity)
                .motorsProcessed(motorsProcessed)
                .stageProcessed(buildStageProcessed(batches))
                .build();
    }

    private String getTimeUnit(String filterType) {

        return switch (filterType.toLowerCase()) {

            case "day" -> "hour";

            case "week" -> "day";

            case "month" -> "week";

            default -> "";
        };
    }
    private List<StageProcessedDto> buildStageProcessed(List<Batch> batches) {

        long totalBatches = batches.size();

        List<DepartmentMaster> departments = departmentRepository.findAll().stream()
                .filter(dept -> !"Sourcing".equalsIgnoreCase(dept.getDepartmentName()))
                .toList();

        List<StageProcessedDto> stageProcessed = new ArrayList<>();

        for (DepartmentMaster dept : departments) {

            List<Long> subDeptIds = dept.getSubDepartments() == null
                    ? Collections.emptyList()
                    : dept.getSubDepartments().stream()
                    .map(SubDepartmentMaster::getSubDepartmentId)
                    .toList();

            int subDeptCount = subDeptIds.size();

            // Distinct batches that have entered this department
            long batchCount = batches.stream()
                    .filter(batch -> batch.getStageProgress() != null)
                    .filter(batch -> batch.getStageProgress().stream()
                            .anyMatch(progress ->
                                    progress.getSubDepartmentId() != null
                                            && subDeptIds.contains(progress.getSubDepartmentId())))
                    .count();

            // Distinct approved (batch + subdepartment) processes
            long completedProcesses = batches.stream()
                    .flatMap(batch -> batch.getStageProgress().stream()
                            .filter(progress ->
                                    progress.getStatus() == Status.COMPLETELY_APPROVED
                                            && progress.getSubDepartmentId() != null
                                            && subDeptIds.contains(progress.getSubDepartmentId()))
                            .map(progress -> batch.getBatchId() + "_" + progress.getSubDepartmentId()))
                    .distinct()
                    .count();

            long denominator = totalBatches * subDeptCount;

            int percentage = denominator > 0
                    ? (int) Math.round((completedProcesses * 100.0) / denominator)
                    : 0;

            stageProcessed.add(
                    StageProcessedDto.builder()
                            .stage(dept.getDepartmentName())
                            .batchCount(batchCount)
                            .percentage(percentage)
                            .build()
            );
        }

        return stageProcessed;
    }

    private List<ChartPointDto> buildHourlyBatchChart(
            List<Batch> batches) {

        Map<String, Long> map = new TreeMap<>();

        for (Batch batch : batches) {

            String hour =
                    String.format("%02d:00",
                            batch.getCreatedAt().getHour());

            map.merge(hour, 1L, Long::sum);
        }

        return map.entrySet()
                .stream()
                .map(entry -> ChartPointDto.builder()
                        .label(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }
    private List<ChartPointDto> buildHourlyMotorChart(
            List<Batch> batches) {

        Map<String, Long> map = new TreeMap<>();

        for (Batch batch : batches) {

            String hour =
                    String.format("%02d:00",
                            batch.getCreatedAt().getHour());

            long motors =
                    batch.getMotorIds() == null
                            ? 0
                            : batch.getMotorIds().size();

            map.merge(hour, motors, Long::sum);
        }

        return map.entrySet()
                .stream()
                .map(entry -> ChartPointDto.builder()
                        .label(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }
    private List<ChartPointDto> buildDailyBatchChart(
            List<Batch> batches) {

        Map<String, Long> map = new LinkedHashMap<>();

        map.put("Mon", 0L);
        map.put("Tue", 0L);
        map.put("Wed", 0L);
        map.put("Thu", 0L);
        map.put("Fri", 0L);
        map.put("Sat", 0L);
        map.put("Sun", 0L);

        for (Batch batch : batches) {

            String day = switch (
                    batch.getCreatedAt().getDayOfWeek()) {

                case MONDAY -> "Mon";
                case TUESDAY -> "Tue";
                case WEDNESDAY -> "Wed";
                case THURSDAY -> "Thu";
                case FRIDAY -> "Fri";
                case SATURDAY -> "Sat";
                case SUNDAY -> "Sun";
            };

            map.merge(day, 1L, Long::sum);
        }

        return map.entrySet()
                .stream()
                .map(entry -> ChartPointDto.builder()
                        .label(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }

    private List<ChartPointDto> buildDailyMotorChart(
            List<Batch> batches) {

        Map<String, Long> map = new LinkedHashMap<>();

        map.put("Mon", 0L);
        map.put("Tue", 0L);
        map.put("Wed", 0L);
        map.put("Thu", 0L);
        map.put("Fri", 0L);
        map.put("Sat", 0L);
        map.put("Sun", 0L);

        for (Batch batch : batches) {

            String day = switch (
                    batch.getCreatedAt().getDayOfWeek()) {

                case MONDAY -> "Mon";
                case TUESDAY -> "Tue";
                case WEDNESDAY -> "Wed";
                case THURSDAY -> "Thu";
                case FRIDAY -> "Fri";
                case SATURDAY -> "Sat";
                case SUNDAY -> "Sun";
            };

            long motors =
                    batch.getMotorIds() == null
                            ? 0
                            : batch.getMotorIds().size();

            map.merge(day, motors, Long::sum);
        }

        return map.entrySet()
                .stream()
                .map(entry -> ChartPointDto.builder()
                        .label(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }

    private List<ChartPointDto> buildWeeklyBatchChart(
            List<Batch> batches) {

        Map<String, Long> map = new LinkedHashMap<>();

        map.put("Week 1", 0L);
        map.put("Week 2", 0L);
        map.put("Week 3", 0L);
        map.put("Week 4", 0L);
        map.put("Week 5", 0L);

        for (Batch batch : batches) {

            int week =
                    ((batch.getCreatedAt().getDayOfMonth() - 1) / 7) + 1;

            map.merge(
                    "Week " + week,
                    1L,
                    Long::sum
            );
        }

        return map.entrySet()
                .stream()
                .map(entry -> ChartPointDto.builder()
                        .label(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }
    private List<ChartPointDto> buildWeeklyMotorChart(
            List<Batch> batches) {

        Map<String, Long> map = new LinkedHashMap<>();

        map.put("Week 1", 0L);
        map.put("Week 2", 0L);
        map.put("Week 3", 0L);
        map.put("Week 4", 0L);
        map.put("Week 5", 0L);

        for (Batch batch : batches) {

            int week =
                    ((batch.getCreatedAt().getDayOfMonth() - 1) / 7) + 1;

            long motors =
                    batch.getMotorIds() == null
                            ? 0
                            : batch.getMotorIds().size();

            map.merge(
                    "Week " + week,
                    motors,
                    Long::sum
            );
        }

        return map.entrySet()
                .stream()
                .map(entry -> ChartPointDto.builder()
                        .label(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }
    //3.ACTIVE BATCHES

    public SystemManagerActiveBatchesResponse getManagerActiveBatches(
            ManagerActiveBatchesRequest request,
            String managerId) {

        int page = request.getPage() == null || request.getPage() < 1
                ? 1
                : request.getPage();

        int limit = request.getLimit() == null || request.getLimit() < 1
                ? 10
                : request.getLimit();

        Pageable pageable = PageRequest.of(page - 1, limit);

        DateRange range = resolveDateRange(
                request.getFilterType(),
                request.getStartDate(),
                request.getEndDate());


        Page<Batch> batchPage =
                dashboardRepo.findActiveBatches(
                        managerId,
                        request.getSearch(),
                        request.getPriority(),
                        request.getStatus(),
                        request.getListType(),
                        request.getDepartmentId(),
                        request.getSubDepartmentId(),
                        range.start(),
                        range.end(),
                        pageable
                );

        List<SystemManagerActiveBatchDto> batches =
                batchPage.getContent()
                        .stream()
                        .map(this::mapToBatchDto)
                        .toList();

        return SystemManagerActiveBatchesResponse.builder()
                .totalRecords(batchPage.getTotalElements())
                .totalPages(batchPage.getTotalPages())
                .currentPage(page)
                .pageSize(limit)
                .batches(batches)
                .build();
    }

    private SystemManagerActiveBatchDto mapToBatchDto(Batch batch) {

        return SystemManagerActiveBatchDto.builder()

                .batchId(batch.getBatchId())

                .batchType(batch.getBatchType())

                .projectName(batch.getProjectName())

                .motorId(
                        batch.getMotorIds() == null
                                || batch.getMotorIds().isEmpty()
                                ? null
                                : batch.getMotorIds().get(0)
                )

                .motorType(
                        MotorTypeDto.builder()
                                .typeId(
                                        batch.getMotorStage() == null
                                                ? null
                                                : batch.getMotorStage().longValue())
                                .typeName("Motor")
                                .build()
                )

                .currentStage(stageProgressDisplayEnricher.enrichCopies(
                        batch,
                        batch.getCurrentStage() != null
                                ? batch.getCurrentStage()
                                : Collections.emptyList()))

                .stageProgress(stageProgressDisplayEnricher.enrichCopies(
                        batch,
                        batch.getStageProgress() != null
                                ? batch.getStageProgress()
                                : Collections.emptyList()))

                .progressPercentage(
                        calculateProgress(batch)
                )

                .createdDate(batch.getCreatedAt())

                .lastUpdatedOn(batch.getUpdatedAt())

                .build();
    }
    private Integer calculateProgress(Batch batch) {
        return batchProgressCalculator.calculate(batch);
    }

    public BatchStatusListResponse getBatchStatusList(BatchStatusListRequest request) {

        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(request.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "No batch found for batchId " + request.getBatchId()));

        List<Long> batchStages = stageSequence.getProgressStages(
                batch.getBatchType(),
                in.co.bel.acempp.service.batch.MainBatchStageProgressHelper.isParallelMainFlow(batch));
        List<Long> deptSubDeptIds =
                stageSequence.getDisplaySubDepartmentsForDepartment(request.getDepartmentId())
                        .stream()
                        .filter(batchStages::contains)
                        .toList();

        // History only for requested department
        List<Batch.StageProgress> history = batch.getStageProgress() == null
                ? Collections.emptyList()
                : batch.getStageProgress().stream()
                .filter(sp -> deptSubDeptIds.contains(sp.getSubDepartmentId()))
                .sorted(Comparator.comparing(Batch.StageProgress::getEnteredAt))
                .toList();

        String batchId = batch.getBatchId();

        // History DTOs
        List<StageEntryDto> historyDtos = history.stream()
                .map(sp -> mapStageEntry(sp, batchId))
                .toList();

        // Last updated stage in this department
        StageEntryDto lastUpdatedStage = history.stream()
                .filter(sp ->
                        sp.getStatus() == Status.COMPLETELY_APPROVED ||
                                sp.getStatus() == Status.REJECTED)
                .max(Comparator.comparing(Batch.StageProgress::getUpdatedAt))
                .map(sp -> mapStageEntry(sp, batchId))
                .orElse(null);

        List<StageEntryDto> currentStages = Collections.emptyList();

        if (batch.getCurrentStage() != null) {
            currentStages = batch.getCurrentStage().stream()
                    .filter(sp -> Objects.equals(
                            sp.getDepartmentId(),
                            request.getDepartmentId()))
                    .map(sp -> mapStageEntry(sp, batchId))
                    .toList();
        }

        int progress = batchProgressCalculator.calculate(history, deptSubDeptIds);

        String departmentName = stageSequence.getDepartmentName(request.getDepartmentId());

        return BatchStatusListResponse.builder()
                .batchId(batch.getBatchId())
                .department(
                        DepartmentInfoDto.builder()
                                .departmentId(request.getDepartmentId())
                                .departmentName(departmentName)
                                .build()
                )
                .progressPercentage(progress)
                .createdOn(batch.getCreatedAt())
                .lastUpdatedOn(batch.getUpdatedAt())
                .stage(
                        BatchStageInfoDto.builder()
                                .lastUpdatedStage(lastUpdatedStage)
                                .currentStage(currentStages)
                                .stageHistory(historyDtos)
                                .build()
                )
                .build();
    }

    private StageEntryDto mapStageEntry(Batch.StageProgress stage, String batchId) {
        String formId = null;
        if (batchId != null && stage.getSubDepartmentId() != null) {
            formId = batchFormResolver.resolveFormId(batchId, stage.getSubDepartmentId());
        }

        return StageEntryDto.builder()
                .subDepartmentId(stage.getSubDepartmentId())
                .subDepartmentName(stage.getSubDepartmentName())
                .status(stage.getStatus().name())
                .formId(formId)

                .submittedBy(stage.getSubmittedBy() == null
                        ? null
                        : PersonInfo.builder()
                        .id(stage.getSubmittedBy().getId())
                        .fullName(stage.getSubmittedBy().getFullName())
                        .build())

                .submittedOn(stage.getSubmittedOn())

                .approvedBy(stage.getApprovedBy() == null
                        ? null
                        : PersonInfo.builder()
                        .id(stage.getApprovedBy().getId())
                        .fullName(stage.getApprovedBy().getFullName())
                        .build())

                .approvedOn(stage.getApprovedOn())

                .remarks(stage.getRemarks())

                .rejectionReason(stage.getRejectionReason())

                .build();
    }

    //5.BLOCKCHAIN EVENTS
    public BlockchainEventsResponse getManagerBlockchainEvents(
            ManagerBlockchainEventsRequest request) {

        LocalDateTime start = parseNullableDate(request.getStartDate());
        LocalDateTime end = parseNullableDate(request.getEndDate());

        // validation
        if (start != null && end != null && start.isAfter(end)) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "startDate cannot be greater than endDate"
            );
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage() - 1),
                request.getPageSize()
        );

        Page<BlockchainEvent> page =
                blockchainEventRepo.findManagerEventsWithFilters(
                        request.getSystemManagerId(),
                        request.getSearch(),
                        request.getEventType(),
                        request.getDepartment(),
                        request.getSubDepartment(),
                        start,
                        end,
                        pageable
                );

        List<BlockchainEventDto> dtos = page.getContent()
                .stream()
                .map(this::mapBlockchainEventToDto)
                .toList();

        return BlockchainEventsResponse.builder()
                .events(dtos)
                .pagination(PaginationDto.builder()
                        .page(request.getPage())
                        .pageSize(request.getPageSize())
                        .totalRecords(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    private BlockchainEventDto mapBlockchainEventToDto(BlockchainEvent e) {

        return BlockchainEventDto.builder()
                .transactionId(e.getTransactionId())
                .batchId(e.getBatchId())
                .eventType(e.getEventType())
                .eventStatusMessage(e.getEventStatusMessage())
                .department(e.getDepartment())
                .subDepartment(e.getSubDepartment())
                .performedBy(e.getPerformedBy())
                .timestamp(e.getTimestamp() != null
                        ? DateTimeConversionUtil.formatLocalDateTime(e.getTimestamp())
                        : null)
                .build();
    }
    //6. ALERTS
    public AlertsResponse getAlerts(AlertsRequest request) {

        LocalDateTime start = parseAlertDate(
                request.getDateRange() != null
                        ? request.getDateRange().getFrom()
                        : null);

        LocalDateTime end = parseAlertDate(
                request.getDateRange() != null
                        ? request.getDateRange().getTo()
                        : null);

        if (start != null && end != null && start.isAfter(end)) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "startDate cannot be greater than endDate");
        }

        List<String> types =
                request.getType() == null || request.getType().isEmpty()
                        ? null
                        : request.getType();

        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage() - 1),
                request.getLimit());

        Page<BatchAlert> page =
                alertRepo.findManagerAlertsWithFilters(
                        request.getSystemManagerId(),
                        request.getSearch(),
                        types,
                        start,
                        end,
                        pageable);

        long total =
                alertRepo.countBySystemManagerId(
                        request.getSystemManagerId());

        long errors =
                alertRepo.countBySystemManagerIdAndType(
                        request.getSystemManagerId(),
                        "error");

        long warnings =
                alertRepo.countBySystemManagerIdAndType(
                        request.getSystemManagerId(),
                        "warning");

        long infos =
                alertRepo.countBySystemManagerIdAndType(
                        request.getSystemManagerId(),
                        "info");

        List<AlertDto> alerts = page.getContent()
                .stream()
                .map(this::mapAlert)
                .toList();

        return AlertsResponse.builder()
                .systemManagerId(request.getSystemManagerId())
                .alerts(alerts)
                .summary(AlertSummaryDto.builder()
                        .total(total)
                        .error(errors)
                        .warning(warnings)
                        .info(infos)
                        .build())
                .page(request.getPage())
                .limit(request.getLimit())
                .build();
    }

    private AlertDto mapAlert(BatchAlert alert) {

        return AlertDto.builder()
                .id(alert.getId())
                .type(alert.getType())
                .msg(alert.getMsg())
                .time(formatRelativeTime(alert.getCreatedAt()))
                .batchId(alert.getBatchId())
                .motorId(alert.getMotorId())
                .stage(alert.getStage())
                .build();
    }
    private String formatRelativeTime(LocalDateTime createdAt) {

        if (createdAt == null) {
            return "";
        }

        Duration duration =
                Duration.between(createdAt, LocalDateTime.now());

        long minutes = duration.toMinutes();

        if (minutes < 60) {
            return minutes + "m ago";
        }

        long hours = duration.toHours();

        if (hours < 24) {
            return hours + "h ago";
        }

        long days = duration.toDays();

        return days + "d ago";
    }
    private LocalDateTime parseAlertDate(String date) {

        if (date == null || date.isBlank()) {
            return null;
        }

        return DateTimeConversionUtil.parseLocalDate(date).atStartOfDay();
    }

    public SystemManagerBatchDetailsResponse getBatchDetails(
            SystemManagerBatchDetailsRequest request) {

        Long subDeptId = request.getSubDepartmentId();
        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(request.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch not found"));

        Batch.StageProgress stageProg = batch.getStageProgress().stream()
                .filter(sp -> sp.getSubDepartmentId().equals(subDeptId))
                .findFirst()
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                        "Batch has not reached this stage"));

        // Provider & form data
        SubDepartmentFormProvider provider = formProviders.get(subDeptId);
        Map<String, Object> processData = null;
        Status submissionType = null;
        PersonInfo submittedBy = null;
        LocalDateTime submittedAt = null;

        if (provider != null) {
            processData = provider.getFormData(batch.getBatchId());
            submissionType = provider.getSubmissionType(batch.getBatchId());
            submittedBy = provider.getSubmittedBy(batch.getBatchId());
            submittedAt = provider.getSubmittedAt(batch.getBatchId());
        }
        if (stageProg.getSubmittedBy() != null) {
            submittedBy = stageProg.getSubmittedBy();
            submittedAt = stageProg.getSubmittedOn();
        }

        // Metadata
        String[] meta = stageSequence.getMeta(subDeptId);
        DepartmentInfo deptInfo = DepartmentInfo.builder()
                .departmentId(stageSequence.getDepartmentId(subDeptId))
                .departmentName(meta[0])
                .build();

        // Process type & mock trial
        String processType = computeProcessType(processData);
        String mockTrialStatus = computeMockTrialStatus(processData);

        BatchDetailsInfo batchDetails = BatchDetailsInfo.builder()
                .batchId(batch.getBatchId())
                .batchType(batch.getBatchType())
                .projectId(batch.getProjectId())
                .subDepartmentStage(meta[1].replace(" ", "_").toUpperCase())
                .submissionType(submissionType)
                .mockTrialStatus(mockTrialStatus)
                .processType(processType)
                .build();

        // Progress (generic)
        ProgressInsights progress = computeProgress(processData);
        progress.setCompletionPercentage(
                batchProgressCalculator.calculateStagePercentage(stageProg));
        progress.setReadyForApproval(isApprovalPending(stageProg.getStatus()));

        // Stage summary – specific to Mixing (but works generically if "stages" key exists)
        List<StageSummary> stageSummaries = buildStageSummary(processData);
        progress.setStageSummary(stageSummaries);   // add to existing progress

        // Quality (placeholder)
        QualityInsights qualityInsights = QualityInsights.builder().build();

        // Workflow
        WorkflowInsights workflow = WorkflowInsights.builder()
                .currentStatus(stageProg.getStatus().name())
                .rejectionReason(stageProg.getRejectionReason())
                .approvalPending(isApprovalPending(stageProg.getStatus()))
                .reworkRequired(Status.REJECTED.equals(stageProg.getStatus()))
                .resubmissionCount(0)
                .build();

        // Risk, delay, alerts (generic)
        RiskInsights risk = buildRisk(workflow.isApprovalPending(), workflow.isReworkRequired());
        DelayInsights delay = buildDelay(workflow.isApprovalPending());
        List<Alert> alerts = buildAlerts(workflow.isApprovalPending(), meta[1]);


        AuditDetails audit = AuditDetails.builder()
                .submittedBy(submittedBy)
                .submittedOn(submittedAt)
                .approvedBy(!Status.REJECTED.equals(stageProg.getStatus()) ? stageProg.getApprovedBy() : null)
                .approvedOn(!Status.REJECTED.equals(stageProg.getStatus())
                        ? stageProg.getApprovedOn()
                        : null)
                .rejectedBy(Status.REJECTED.equals(stageProg.getStatus()) ? stageProg.getApprovedBy() : null)
                .rejectedOn(Status.REJECTED.equals(stageProg.getStatus())
                        ? stageProg.getApprovedOn()
                        : null)
                .build();

        // Timeline (generic)
        List<TimelineEvent> timeline = buildTimeline(batch, submittedBy, submittedAt, stageProg);

        // Build response and set sub‑department‑specific data fields
        SystemManagerBatchDetailsResponse response = SystemManagerBatchDetailsResponse.builder()
                .subDepartmentId(subDeptId)
                .subDepartmentName(meta[1])
                .department(deptInfo)
                .batchDetails(batchDetails)
                .progressInsights(progress)
                .qualityInsights(qualityInsights)
                .workflowInsights(workflow)
                .riskInsights(risk)
                .delayInsights(delay)
                .alerts(alerts)
                .auditDetails(audit)
                .timeline(timeline)
                .documents(List.of())
                .build();

        // Set the correct processing-data field based on sub‑department
        if (subDeptId == 3) {
            response.setRawMaterialProcessingData(processData);
        } else if (subDeptId == 4) {
            response.setCasePreparationProcessingData(processData);
        } else if (subDeptId == 5) {
            response.setMixingProcessingData(processData);
        } else if (subDeptId == 6) {
            response.setCastingCuringProcessingData(processData);
        } else if (subDeptId == 7) {
            response.setPostCureProcessingData(processData);
        } else if (subDeptId == 8) {
            response.setSubscaleProcessingData(processData);
        }else if (subDeptId == 9) {
            response.setTrimmingProcessingData(processData);
        } else if (subDeptId == 10) {
            response.setNdtProcessingData(processData);
        } else if (subDeptId == 11) {
            return buildQcDivisionDetails(batch, request.getBatchId(), subDeptId);
        } else if(subDeptId == 12L) {
            return buildStaticTestingDetails(batch, request.getBatchId(), subDeptId);
        } else if (subDeptId == 13) {
            return buildDispatchDetails(batch, request.getBatchId(), subDeptId);
        }

        return response;
    }
    private SystemManagerBatchDetailsResponse buildStaticTestingDetails(
            Batch batch, String batchId, Long subDeptId) {

        // Fetch all static testing forms for this batch
        List<StaticTesting> forms = staticTestingRepo.findAllByBatchIdAndIsDeletedFalse(batchId);

        // Map each form to a summary map
        List<Map<String, Object>> formMaps = forms.stream().map(form -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("formId", form.getFormId());
            map.put("formStatus", form.getStatus());

            // Keep motor identity, subtype and partial-approval status in the summary.
            map.put("motors", form.getMotors() != null ? form.getMotors() : List.of());
            return map;
        }).collect(Collectors.toList());

        // Overall batch status from stageProgress
        Batch.StageProgress stageProg = batch.getStageProgress().stream()
                .filter(sp -> sp.getSubDepartmentId().equals(subDeptId))
                .findFirst().orElseThrow();
        String batchStatus = stageProg.getStatus().name();

        // Department metadata
        String[] meta = stageSequence.getMeta(subDeptId);
        DepartmentInfo deptInfo = DepartmentInfo.builder()
                .departmentId(stageSequence.getDepartmentId(subDeptId))
                .departmentName(meta[0])
                .build();
        SubDepartmentFormProvider provider = formProviders.get(subDeptId);
        PersonInfo submittedBy = null;
        LocalDateTime submittedAt = null;

        if (provider != null) {

            submittedBy = provider.getSubmittedBy(batch.getBatchId());
            submittedAt = provider.getSubmittedAt(batch.getBatchId());
        }
        if (stageProg.getSubmittedBy() != null) {
            submittedBy = stageProg.getSubmittedBy();
            submittedAt = stageProg.getSubmittedOn();
        }
        AuditDetails audit = AuditDetails.builder()
                .submittedBy(submittedBy)
                .submittedOn(submittedAt)
                .approvedBy(!Status.REJECTED.equals(stageProg.getStatus()) ? stageProg.getApprovedBy() : null)
                .approvedOn(!Status.REJECTED.equals(stageProg.getStatus())
                        ? stageProg.getApprovedOn()
                        : null)
                .rejectedBy(Status.REJECTED.equals(stageProg.getStatus()) ? stageProg.getApprovedBy() : null)
                .rejectedOn(Status.REJECTED.equals(stageProg.getStatus())
                        ? stageProg.getApprovedOn()
                        : null)
                .build();
        // Build response
        return SystemManagerBatchDetailsResponse.builder()
                .subDepartmentId(subDeptId)
                .subDepartmentName(meta[1])
                .department(deptInfo)
//                .batchId(batch.getBatchId())
//                .batchStatus(batchStatus)
                .forms(formMaps)
                .progressInsights(ProgressInsights.builder()
                        .completionPercentage(
                                batchProgressCalculator.calculateStagePercentage(stageProg))
                        .isReadyForApproval(isApprovalPending(stageProg.getStatus()))
                        .build())
                .qualityInsights(QualityInsights.builder().build())
                .workflowInsights(WorkflowInsights.builder()
                        .currentStatus(batchStatus).build())
                .riskInsights(RiskInsights.builder().build())
                .delayInsights(DelayInsights.builder().build())
                .alerts(List.of())
                .auditDetails(audit)
                .timeline(List.of())
                .documents(List.of())
                .build();
    }
    private SystemManagerBatchDetailsResponse buildQcDivisionDetails(
            Batch batch, String batchId, Long subDeptId) {

        // 1. Fetch QC Division form
        QcDivision form = qcDivisionRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElse(null);

        // 2. Build qcDetails map
        Map<String, Object> qcDetails = new LinkedHashMap<>();
        if (form != null && form.getDivisionDetails() != null) {
            for (DivisionDetail dd : form.getDivisionDetails()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("status", dd.getStatus() != null
                        ? dd.getStatus().name()
                        : computeDivisionStatus(dd.getData()));
                entry.put("data", dd.getData());
                String key = dd.getSubType() == null
                        ? dd.getDivision()
                        : dd.getDivision() + ":" + dd.getSubType();
                qcDetails.put(key, entry);
            }
        }

        // 3. Batch status from stageProgress
        Batch.StageProgress stageProg = batch.getStageProgress().stream()
                .filter(sp -> sp.getSubDepartmentId().equals(subDeptId))
                .findFirst().orElseThrow();
        String batchStatus = stageProg.getStatus().name();

        // 4. Metadata
        String[] meta = stageSequence.getMeta(subDeptId);
        DepartmentInfo deptInfo = DepartmentInfo.builder()
                .departmentId(stageSequence.getDepartmentId(subDeptId))
                .departmentName(meta[0])
                .build();

        // 5. Progress insights (overall completion across divisions)
        int totalDivisions = qcDetails.size();
        long completedDivisions = qcDetails.values().stream()
                .filter(v -> {
                    Object status = ((Map<String, Object>) v).get("status");
                    return "COMPLETED".equals(status) || "APPROVED".equals(status);
                })
                .count();
        int completionPct = batchProgressCalculator.calculateStagePercentage(stageProg);

        ProgressInsights progress = ProgressInsights.builder()
                .totalSpecs(totalDivisions)
                .filledSpecs((int) completedDivisions)
                .completionPercentage(completionPct)
                .isReadyForApproval(isApprovalPending(stageProg.getStatus()))
                .build();

        // 6. Workflow insights
        WorkflowInsights workflow = WorkflowInsights.builder()
                .currentStatus(batchStatus)
                .rejectionReason(stageProg.getRejectionReason())
                .approvalPending(isApprovalPending(stageProg.getStatus()))
                .reworkRequired(Status.REJECTED.equals(stageProg.getStatus()))
                .resubmissionCount(0)
                .build();

        // 7. Risk, delay, alerts
        RiskInsights risk = buildRisk(workflow.isApprovalPending(), workflow.isReworkRequired());
        DelayInsights delay = buildDelay(workflow.isApprovalPending());
        List<Alert> alerts = buildAlerts(workflow.isApprovalPending(), meta[1]);

        // 8. Audit
        PersonInfo submittedBy = form != null && form.getSubmittedBy() != null
                ? form.getSubmittedBy()
                : stageProg.getSubmittedBy();
        LocalDateTime submittedAt = form != null && form.getSubmittedOn() != null
                ? form.getSubmittedOn()
                : stageProg.getSubmittedOn();
        AuditDetails audit = AuditDetails.builder()
                .submittedBy(submittedBy)
                .submittedOn(submittedAt)
                .approvedBy(!Status.REJECTED.equals(stageProg.getStatus()) ? stageProg.getApprovedBy() : null)
                .approvedOn(!Status.REJECTED.equals(stageProg.getStatus())
                        ? stageProg.getApprovedOn()
                        : null)
                .rejectedBy(Status.REJECTED.equals(stageProg.getStatus()) ? stageProg.getApprovedBy() : null)
                .rejectedOn(Status.REJECTED.equals(stageProg.getStatus())
                        ? stageProg.getApprovedOn()
                        : null)
                .build();

        // 9. Timeline
        List<TimelineEvent> timeline = buildTimeline(batch, submittedBy, submittedAt, stageProg);

        // 10. Build response
        return SystemManagerBatchDetailsResponse.builder()
                .subDepartmentId(subDeptId)
                .subDepartmentName(meta[1])
                .department(deptInfo)
//                .batchDetails(batch.g())
//                .batchStatus(batchStatus)
                .qcDetails(qcDetails)
                .progressInsights(progress)
                .qualityInsights(QualityInsights.builder().build())   // placeholder
                .workflowInsights(workflow)
                .riskInsights(risk)
                .delayInsights(delay)
                .alerts(alerts)
                .auditDetails(audit)
                .timeline(timeline)
                .documents(List.of())
                .build();
    }
    private SystemManagerBatchDetailsResponse buildDispatchDetails(
            Batch batch, String batchId, Long subDeptId) {

        // 1. Fetch dispatch form via provider
        Map<String, Object> dispatchData = formProviders.get(subDeptId).getFormData(batchId);

        // 2. Fetch static testing forms
        List<StaticTesting> staticTestings = staticTestingRepo.findAllByBatchIdAndIsDeletedFalse(batchId);

        // 3. Build motor list (same as before, but using dispatchData)
        List<Map<String, Object>> motors = buildMotorsForDispatch(batch, dispatchData, staticTestings);

        // 4. Department metadata
        String[] meta = stageSequence.getMeta(subDeptId);
        DepartmentInfo deptInfo = DepartmentInfo.builder()
                .departmentId(stageSequence.getDepartmentId(subDeptId))
                .departmentName(meta[0])
                .build();

        // 5. Batch details
        Map<String, Object> batchDetails = buildBatchDetailsForDispatch(batch, dispatchData);

        SubDepartmentFormProvider provider = formProviders.get(subDeptId);

        PersonInfo submittedBy = null;
        LocalDateTime submittedAt = null;

        if (provider != null) {

            submittedBy = provider.getSubmittedBy(batch.getBatchId());
            submittedAt = provider.getSubmittedAt(batch.getBatchId());
        }
        // 3. Batch status from stageProgress
        Batch.StageProgress stageProg = batch.getStageProgress().stream()
                .filter(sp -> sp.getSubDepartmentId().equals(subDeptId))
                .findFirst().orElseThrow();
        if (stageProg.getSubmittedBy() != null) {
            submittedBy = stageProg.getSubmittedBy();
            submittedAt = stageProg.getSubmittedOn();
        }
        String batchStatus = stageProg.getStatus().name();
        AuditDetails audit = AuditDetails.builder()
                .submittedBy(submittedBy)
                .submittedOn(submittedAt)
                .approvedBy(!Status.REJECTED.equals(stageProg.getStatus()) ? stageProg.getApprovedBy() : null)
                .approvedOn(!Status.REJECTED.equals(stageProg.getStatus())
                        ? stageProg.getApprovedOn()
                        : null)
                .rejectedBy(Status.REJECTED.equals(stageProg.getStatus()) ? stageProg.getApprovedBy() : null)
                .rejectedOn(Status.REJECTED.equals(stageProg.getStatus())
                        ? stageProg.getApprovedOn()
                        : null)
                .build();
        // 6. Response
        return SystemManagerBatchDetailsResponse.builder()
                .subDepartmentId(subDeptId)
                .subDepartmentName(meta[1])
                .department(deptInfo)
                .auditDetails(audit)
//                .batchId(batch.getBatchId())
                .dispatchBatchDetails(batchDetails)
                .motors(motors)
                .build();
    }
    private List<Map<String, Object>> buildMotorsForDispatch(
            Batch batch,
            Map<String, Object> dispatchData,
            List<StaticTesting> staticTestings) {

        List<Map<String, Object>> motors = new ArrayList<>();
        if (batch.getMotorIds() == null) return motors;

        for (String motorId : batch.getMotorIds()) {
            Map<String, Object> motor = new LinkedHashMap<>();
            motor.put("motorId", motorId);
            motor.put("motorNumber", motorId);
            motor.put("motorType", "MAIN_MOTOR");   // default

            // ---- Static testing details ----
            Map<String, Object> stDetails = null;

            if (staticTestings != null) {
                for (StaticTesting st : staticTestings) {

                    if (st.getMotors() == null) {
                        continue;
                    }

                    StaticMotorData md = st.getMotors().stream()
                            .filter(Objects::nonNull)
                            .filter(m -> motorId.equals(m.getMotorId()))
                            .findFirst()
                            .orElse(null);

                    if (md == null) {
                        continue;
                    }

                    StaticTestingDetails testingDetails = md.getStaticTestingDetails();
                    Map<String, Object> stMap = testingDetails == null
                            ? new LinkedHashMap<>()
                            : new LinkedHashMap<>(objectMapper.convertValue(
                                    testingDetails,
                                    new TypeReference<Map<String, Object>>() { }));
                    stMap.put("formId", st.getFormId());
                    stMap.put("status", st.getStatus());

                    // Set actual motor subtype if available
                    if (md.getSubType() != null) {
                        motor.put("motorType", md.getSubType());
                    }

                    stDetails = stMap;
                    break;
                }
            }

            motor.put(
                    "staticTestingDetails",
                    stDetails != null ? stDetails : Collections.emptyMap()
            );
            // ---- Dispatch data from the form ----
            boolean dispatchExists = dispatchData != null;
            motor.put("dispatchStatus", Map.of(
                    "eligible", dispatchExists,
                    "status", dispatchExists ? "READY_FOR_DISPATCH" : "PENDING",
                    "reason", dispatchExists ? "" : "Dispatch form not created"
            ));

            // Find the specific motor data inside the dispatch form
            DispatchMotor dispatchMotor = null;
            if (dispatchExists && dispatchData.get("motors") instanceof List<?> rawMotors) {
                List<DispatchMotor> dispMotors = rawMotors.stream()
                        .filter(DispatchMotor.class::isInstance)
                        .map(DispatchMotor.class::cast)
                        .toList();
                dispatchMotor = dispMotors.stream()
                        .filter(m -> motorId.equals(m.getMotorId()))
                        .findFirst().orElse(null);
            }

            DispatchDetails details = dispatchMotor != null
                    ? dispatchMotor.getDispatchDetails()
                    : null;

            // Extract each section
            motor.put("ndtClearance",
                    details != null && details.getNdtClearance() != null
                            ? details.getNdtClearance()
                            : Map.of("accorded", "PENDING", "momNo", "", "clearanceDate", "", "documents", List.of()));
            motor.put("finalAcceptanceCommitteeClearance",
                    details != null && details.getFinalAcceptanceCommitteeClearance() != null
                            ? details.getFinalAcceptanceCommitteeClearance()
                            : Map.of("accorded", "PENDING", "momNo", "", "approvalDate", "", "documents", List.of()));
            motor.put("propellantProperties", dispatchListStatus(
                    details != null ? details.getPropellantProperties() : null,
                    "properties"));
            motor.put("rocketMotorInspection", dispatchListStatus(
                    details != null ? details.getRocketMotorInspection() : null,
                    "inspectionDetails"));
            motor.put("vehicleInspection", dispatchListStatus(
                    details != null ? details.getVehicleDetails() : null,
                    "inspectionDetails"));
            motor.put("rocketMotorPackingDetails", dispatchListStatus(
                    details != null ? details.getRocketMotorPackingDetails() : null,
                    "packingDetails"));
            motor.put("waiverDetails",
                    details != null && details.getWaiverDetails() != null
                            ? details.getWaiverDetails()
                            : Map.of("available", false, "details", "", "uploadedDocuments", List.of()));
            motor.put("safetyClearance",
                    details != null && details.getSafetyClearance() != null
                            ? details.getSafetyClearance()
                            : Map.of("accorded", "PENDING", "clearanceCertificate", ""));
            motor.put("dispatchDetails", Map.of(
                    "dispatchFormExists", dispatchExists,
                    "formId", dispatchExists ? dispatchData.get("formId") : "",
                    "status", dispatchExists ? dispatchData.get("status") : ""
            ));

            motors.add(motor);
        }
        return motors;
    }
    private Map<String, Object> buildBatchDetailsForDispatch(Batch batch, Map<String, Object> dispatchData) {

        String castingDate = "";
        if (dispatchData != null && dispatchData.get("motors") instanceof List<?> rawMotors) {
            DispatchMotor firstMotor = rawMotors.stream()
                    .filter(DispatchMotor.class::isInstance)
                    .map(DispatchMotor.class::cast)
                    .findFirst()
                    .orElse(null);
            if (firstMotor != null && firstMotor.getDispatchDetails() != null
                    && firstMotor.getDispatchDetails().getCastingDate() != null) {
                castingDate = DateTimeConversionUtil.formatLocalDateTime(
                        firstMotor.getDispatchDetails().getCastingDate());
            }
        }

        Map<String, Object> batchDetails = new LinkedHashMap<>();
        batchDetails.put("batchType", batch.getBatchType());
        batchDetails.put("projectName", batch.getProjectName());
        batchDetails.put("stage", batch.getMotorStage() != null ? batch.getMotorStage().toString() : "");
        batchDetails.put("castingDate", castingDate);
        batchDetails.put("batchStatus", "READY_FOR_DISPATCH");
        batchDetails.put("manufacturingStatus", "COMPLETED");
        batchDetails.put("qualityStatus", "CLEARED");
        batchDetails.put("createdDate", batch.getCreatedAt() != null
                ? batch.getCreatedAt().toLocalDate().toString() : "");
        batchDetails.put("remarks", "");
        return batchDetails;
    }
    private Map<String, Object> dispatchListStatus(List<?> rows, String dataKey) {
        if (rows == null || rows.isEmpty()) {
            return Map.of("status", "PENDING", dataKey, List.of());
        }
        return Map.of("status", "COMPLETED", dataKey, rows);
    }
    private String computeDivisionStatus(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return "PENDING";
        boolean anyFilled = data.values().stream().anyMatch(v -> {
            if (v instanceof Map) return !((Map<?, ?>) v).isEmpty();
            if (v instanceof Collection) return !((Collection<?>) v).isEmpty();
            return v != null && !v.toString().isEmpty();
        });
        return anyFilled ? "IN_PROGRESS" : "PENDING";
    }
    private ProgressInsights computeProgress(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return ProgressInsights.builder()
                    .totalSpecs(0).filledSpecs(0)
                    .completionPercentage(0).isReadyForApproval(false).build();
        }
        int total = countTotalFields(data);
        int filled = countFilledFields(data);
        int pct = total > 0 ? (filled * 100 / total) : 0;
        return ProgressInsights.builder()
                .totalSpecs(total).filledSpecs(filled)
                .completionPercentage(pct).isReadyForApproval(pct == 100).build();
    }

    private int countTotalFields(Map<String, Object> map) {
        int count = 0;
        for (Object value : map.values()) {
            if (value instanceof Map) count += countTotalFields((Map<String, Object>) value);
            else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) count += countTotalFields((Map<String, Object>) item);
                    else count++;
                }
            } else count++;
        }
        return count;
    }

    private int countFilledFields(Map<String, Object> map) {
        int count = 0;
        for (Object value : map.values()) {
            if (value instanceof Map) count += countFilledFields((Map<String, Object>) value);
            else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) count += countFilledFields((Map<String, Object>) item);
                    else if (item != null && !item.toString().isEmpty()) count++;
                }
            } else if (value != null && !value.toString().isEmpty()) count++;
        }
        return count;
    }
    private boolean isApprovalPending(Status status) {
        return status == Status.WAITING_FOR_APPROVAL
                || status == Status.WAITING_FOR_PARTIAL_APPROVAL;
    }

    private RiskInsights buildRisk(boolean approvalPending, boolean reworkRequired) {
        String level = "Low";
        int score = 0;
        List<String> factors = new ArrayList<>();
        if (approvalPending) {
            level = "Medium";
            score = 42;
            factors.add("Approval pending");
        }
        if (reworkRequired) {
            level = "High";
            score = 70;
            factors.add("Rework required due to rejection");
        }
        return RiskInsights.builder()
                .riskLevel(level).riskScore(score).riskFactors(factors).build();
    }

    private DelayInsights buildDelay(boolean approvalPending) {
        return DelayInsights.builder()
                .isDelayed(approvalPending)
                .delayInHours(approvalPending ? 18 : 0)
                .delayReason(approvalPending ? "Approval pending beyond expected turnaround time" : null)
                .build();
    }

    private List<Alert> buildAlerts(boolean approvalPending, String subDeptName) {
        if (!approvalPending) return List.of();
        return List.of(Alert.builder()
                .type("warning")
                .message(subDeptName + " is waiting for approval")
                .time(DateTimeConversionUtil.formatLocalDateTime(LocalDateTime.now()))
                .build());
    }
    private List<TimelineEvent> buildTimeline(Batch batch,
                                              PersonInfo submittedBy,
                                              LocalDateTime submittedAt,
                                              Batch.StageProgress stageProg) {
        List<TimelineEvent> timeline = new ArrayList<>();

        timeline.add(TimelineEvent.builder()
                .event("Created")
                .by(batch.getCreatedBy())
                .time(DateTimeConversionUtil.formatLocalDateTime(batch.getCreatedAt()))
                .build());

        if (submittedAt != null) {
            timeline.add(TimelineEvent.builder()
                    .event("Submitted")
                    .by(submittedBy)
                    .time(DateTimeConversionUtil.formatLocalDateTime(submittedAt))
                    .build());
        }

        if (stageProg.getStatus() == Status.COMPLETELY_APPROVED || stageProg.getStatus() == Status.REJECTED) {
            PersonInfo actor = batch.getUpdatedBy() != null ? batch.getUpdatedBy()
                    : PersonInfo.builder().id("SYSTEM").fullName("System").build();
            timeline.add(TimelineEvent.builder()
                    .event(stageProg.getStatus() == Status.COMPLETELY_APPROVED ? "Approved" : "Rejected")
                    .by(actor)
                    .time(DateTimeConversionUtil.formatLocalDateTime(stageProg.getUpdatedAt()))
                    .build());
        }
        return timeline;
    }
    private String computeMockTrialStatus(Map<String, Object> processData) {
        if (processData == null) return "NOT_APPLICABLE";
        // If a form contains a "mockTrial" key, derive the status from it
        if (processData.containsKey("mockTrial")) {
            // Example logic: check a nested field like "status"
            Map<String, Object> mockTrial = (Map<String, Object>) processData.get("mockTrial");
            if (mockTrial.get("completed") instanceof Boolean completed && completed) {
                return "COMPLETED";
            }
            return "IN_PROGRESS";
        }
        return "NOT_APPLICABLE";
    }

    private String computeProcessType(Map<String, Object> processData) {
        if (processData == null) return "NOT_STARTED";

        // Raw Material Preparation → check premixes
        if (processData.containsKey("premixes")) {
            List<Map<String, Object>> premixes = (List<Map<String, Object>>) processData.get("premixes");
            boolean hasSolid = false;
            boolean hasLiquid = false;

            for (Map<String, Object> premix : premixes) {
                String type = (String) premix.get("materialType");
                if ("SOLID".equalsIgnoreCase(type)) hasSolid = true;
                else if ("LIQUID".equalsIgnoreCase(type)) hasLiquid = true;
                else if ("BOTH".equalsIgnoreCase(type)) {
                    hasSolid = true;
                    hasLiquid = true;
                }
            }

            if (hasSolid && hasLiquid) return "SOLID_LIQUID_MIX";
            if (hasSolid) return "SOLID_ONLY";
            if (hasLiquid) return "LIQUID_ONLY";
            return "NOT_STARTED";
        }

        // Default for other sub‑departments (can be overridden later)
        return "FULL_PROCESS";
    }
    private List<StageSummary> buildStageSummary(Map<String, Object> processData) {
        if (processData == null || !processData.containsKey("stages")) {
            return null;
        }

        List<Map<String, Object>> stages = (List<Map<String, Object>>) processData.get("stages");
        List<StageSummary> summaries = new ArrayList<>();

        for (Map<String, Object> stage : stages) {
            String stageType = (String) stage.getOrDefault("stageType", "UNKNOWN");
            List<Map<String, Object>> premixes = (List<Map<String, Object>>) stage.get("premixes");
            int totalPremixes = premixes != null ? premixes.size() : 0;

            int totalFields = 0;
            int filledFields = 0;

            if (premixes != null) {
                for (Map<String, Object> premix : premixes) {
                    // processParticulars is a List of sections (each section is a Map)
                    List<Map<String, Object>> sections =
                            (List<Map<String, Object>>) premix.get("processParticulars");
                    if (sections != null) {
                        for (Map<String, Object> section : sections) {
                            List<Map<String, Object>> rows =
                                    (List<Map<String, Object>>) section.get("rows");
                            if (rows != null) {
                                totalFields += rows.size();
                                filledFields += rows.stream()
                                        .filter(row -> {
                                            Object val = row.get("value");
                                            return val != null && !val.toString().isEmpty();
                                        })
                                        .count();
                            }
                        }
                    }
                }
            }

            int completion = totalFields > 0 ? (filledFields * 100 / totalFields) : 0;
            summaries.add(StageSummary.builder()
                    .stageType(stageType)
                    .totalPremixes(totalPremixes)
                    .totalFields(totalFields)
                    .filledFields(filledFields)
                    .completionPercentage(completion)
                    .build());
        }
        return summaries;
    }
    public BatchStagesResponse getBatchStages(String batchId) {

        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "No batch found for batchId " + batchId));

        int ageInDays = batch.getCreatedAt() != null
                ? (int) Duration.between(batch.getCreatedAt(), LocalDateTime.now()).toDays()
                : 0;

        int overallProgress = batchProgressCalculator.calculate(batch);
        List<Long> applicableBatchStages = stageSequence.getProgressStages(
                batch.getBatchType(),
                in.co.bel.acempp.service.batch.MainBatchStageProgressHelper.isParallelMainFlow(batch));

        List<DepartmentStageDto> departmentStages = new ArrayList<>();

        for (Long deptId : stageSequence.getDisplayDepartmentIds()) {

            String deptName = stageSequence.getDepartmentName(deptId);
            List<Long> subDeptIds =
                    stageSequence.getDisplaySubDepartmentsForDepartment(deptId)
                            .stream()
                            .filter(applicableBatchStages::contains)
                            .toList();

            List<Batch.StageProgress> deptProgress = batch.getStageProgress().stream()
                    .filter(sp -> subDeptIds.contains(sp.getSubDepartmentId()))
                    .toList();

            long startedCount = deptProgress.stream()
                    .filter(sp -> sp.getStatus() != Status.TO_BE_INITIATED)
                    .count();

            int completionPercentage = batchProgressCalculator.calculate(
                    deptProgress,
                    subDeptIds);

            String status;

            if (subDeptIds.isEmpty()) {
                status = "NOT_APPLICABLE";
                completionPercentage = 0;
            } else if (completionPercentage == 100) {
                status = "COMPLETED";
                completionPercentage = 100;
            } else if (startedCount > 0) {
                status = "IN_PROGRESS";
            } else {
                status = "NOT_STARTED";
                completionPercentage = 0;
            }

            departmentStages.add(
                    DepartmentStageDto.builder()
                            .departmentId(deptId)
                            .departmentName(deptName)
                            .status(status)
                            .completionPercentage(completionPercentage)
                            .build()
            );
        }

        BatchDeptStagesDto batchDto = BatchDeptStagesDto.builder()
                .batchId(batch.getBatchId())
                .createdOn(batch.getCreatedAt())
                .ageInDays(ageInDays)
                .overallProgress(overallProgress)
                .stages(departmentStages)
                .build();

        return BatchStagesResponse.builder()
                .batch(batchDto)
                .build();
    }

    // Helper to build sub‑department stage DTO from a StageProgress entry (or null)
    private SubDepartmentStageDto buildSubDepartmentStage(Long subDeptId, Batch.StageProgress sp, boolean isSourcing) {
        String[] meta = stageSequence.getMeta(subDeptId);
        String name = meta != null ? meta[1] : "Unknown";

        if (isSourcing) {
            // Sourcing stages are always approved when a batch exists
            return SubDepartmentStageDto.builder()
                    .subDepartmentId(subDeptId)
                    .name(name)
                    .status("Approved")
                    .completionPercentage(100)
                    .riskLevel("Low")
                    .isDelayed(false)
                    .hasFailures(false)
                    .approvalPending(false)
                    .build();
        }

        if (sp == null) {
            return SubDepartmentStageDto.builder()
                    .subDepartmentId(subDeptId)
                    .name(name)
                    .status("Not Started")
                    .completionPercentage(0)
                    .riskLevel("Low")
                    .isDelayed(false)
                    .hasFailures(false)
                    .approvalPending(false)
                    .build();
        }

        String status = sp.getStatus().name();
        int completion = batchProgressCalculator.calculateStagePercentage(sp);
        boolean approvalPending = sp.getStatus() == Status.WAITING_FOR_APPROVAL
                || sp.getStatus() == Status.WAITING_FOR_PARTIAL_APPROVAL;

        String risk = (sp.getStatus() == Status.REJECTED) ? "High"
                : approvalPending ? "Medium" : "Low";

        boolean isDelayed = approvalPending
                && sp.getEnteredAt() != null
                && Duration.between(sp.getEnteredAt(), LocalDateTime.now()).toHours() > 24;

        boolean hasFailures = sp.getStatus() == Status.REJECTED;

        return SubDepartmentStageDto.builder()
                .subDepartmentId(subDeptId)
                .name(name)
                .status(status)
                .completionPercentage(completion)
                .riskLevel(risk)
                .isDelayed(isDelayed)
                .hasFailures(hasFailures)
                .approvalPending(approvalPending)
                .build();
    }
}
