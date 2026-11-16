package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.admin.motor.*;
import in.co.bel.acempp.dto.batch.StatMetric;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.sourcing.RocketMotorCasing;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.repository.sourcing.RocketMotorCasingRepository;
import in.co.bel.acempp.service.batch.BatchMotorDiscardHelper;
import in.co.bel.acempp.service.batch.MotorBatchLifecycleHelper;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMotorService {

    private static final String SYSTEM_MANAGER_AUTHORITY = "System Manager";

    private final MongoTemplate mongoTemplate;
    private final RocketMotorCasingRepository rocketMotorCasingRepository;
    private final UserRepository userRepository;

    public MotorListResponse getMotorList(MotorListRequest request, Authentication authentication) {
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 10;

        MotorListFiltersDto filters = request.getFilters() != null ? request.getFilters() : new MotorListFiltersDto();
        String search = firstNonBlank(request.getSearch(), filters.getSearch());
        String systemManagerId = resolveSystemManagerScope(authentication);

        List<MotorSummaryDto> all = buildMotorSummaries(filters, search, systemManagerId);

        long total = all.size();
        int from = Math.min((page - 1) * limit, (int) total);
        int to = Math.min(from + limit, (int) total);
        List<MotorSummaryDto> pageItems = all.subList(from, to);

        int totalPages = limit <= 0 ? 0 : (int) Math.ceil(total / (double) limit);

        return MotorListResponse.builder()
                .motors(pageItems)
                .pagination(PaginationDto.builder()
                        .page(page)
                        .pageSize(limit)
                        .totalRecords(total)
                        .totalPages(totalPages)
                        .build())
                .build();
    }

    public MotorStatsResponse getMotorStats(MotorStatsRequest request, Authentication authentication) {
        MotorListFiltersDto filters = new MotorListFiltersDto();
        if (request != null) {
            filters.setFilterType(request.getFilterType());
            filters.setStartDate(request.getStartDate());
            filters.setEndDate(request.getEndDate());
        }

        List<Batch> batches = loadBatches(filters, resolveSystemManagerScope(authentication));
        Map<String, List<Batch>> byMotor = new LinkedHashMap<>();
        for (Batch batch : batches) {
            List<String> motorIds = batch.getMotorIds();
            if (motorIds == null || motorIds.isEmpty()) {
                continue;
            }
            for (String rawMotorId : motorIds) {
                String motorId = String.valueOf(rawMotorId == null ? "" : rawMotorId).trim();
                if (motorId.isEmpty()) {
                    continue;
                }
                byMotor.computeIfAbsent(motorId, ignored -> new ArrayList<>()).add(batch);
            }
        }

        long total = byMotor.size();
        long discarded = 0;
        long dispatched = 0;
        long inProgress = 0;

        for (Map.Entry<String, List<Batch>> entry : byMotor.entrySet()) {
            String motorId = entry.getKey();
            List<Batch> linked = entry.getValue();
            boolean isDiscarded = linked.stream()
                    .anyMatch(batch -> BatchMotorDiscardHelper.isDiscarded(batch, motorId));
            if (isDiscarded) {
                discarded++;
                continue;
            }
            boolean isDispatched = linked.stream()
                    .anyMatch(batch -> MotorBatchLifecycleHelper.isDispatched(batch, motorId));
            if (isDispatched) {
                dispatched++;
                continue;
            }
            boolean started = linked.stream()
                    .anyMatch(batch -> MotorBatchLifecycleHelper.hasStarted(batch, motorId));
            if (started) {
                inProgress++;
            }
        }

        return MotorStatsResponse.builder()
                .totalMotors(metric(total))
                .inProgress(metric(inProgress))
                .dispatched(metric(dispatched))
                .discarded(metric(discarded))
                .build();
    }

    public MotorDetailsResponse getMotorDetails(MotorDetailsRequest request, Authentication authentication) {
        String motorId = String.valueOf(request.getMotorId()).trim();
        if (motorId.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "motorId is required");
        }

        List<MotorSummaryDto> matches = buildMotorSummaries(
                        new MotorListFiltersDto(), null, resolveSystemManagerScope(authentication))
                .stream()
                .filter(m -> motorId.equalsIgnoreCase(m.getMotorId()))
                .toList();

        if (matches.isEmpty()) {
            throw new AcemException(ErrorCode.MOTORS_NOT_FOUND, "Motor not found: " + motorId);
        }

        MotorSummaryDto summary = matches.get(0);
        return MotorDetailsResponse.builder()
                .motorId(summary.getMotorId())
                .motorCasingId(summary.getMotorCasingId())
                .casingStatus(summary.getCasingStatus())
                .projectId(summary.getProjectId())
                .motorStage(summary.getMotorStage())
                .workflowStatus(summary.getWorkflowStatus())
                .linkedBatches(summary.getLinkedBatches())
                .discarded(summary.getDiscarded())
                .discardReason(summary.getDiscardReason())
                .build();
    }

    public MotorDiscardResponse discardMotor(MotorDiscardRequest request, String userId) {
        String motorId = String.valueOf(request.getMotorId() == null ? "" : request.getMotorId()).trim();
        String reason = String.valueOf(request.getReason() == null ? "" : request.getReason()).trim();
        if (motorId.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "motorId is required");
        }
        if (reason.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "reason is required");
        }

        User admin = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));

                Query query = new Query(new Criteria().andOperator(
                Criteria.where("isDeleted").ne(true),
                Criteria.where("motorIds").is(motorId)));
        List<Batch> batches = mongoTemplate.find(query, Batch.class);
        if (batches.isEmpty()) {
            // Also try case-insensitive match via loaded summaries
            batches = loadBatches(new MotorListFiltersDto(), null).stream()
                    .filter(b -> b.getMotorIds() != null
                            && b.getMotorIds().stream()
                            .anyMatch(id -> id != null && motorId.equalsIgnoreCase(id.trim())))
                    .toList();
        }
        if (batches.isEmpty()) {
            throw new AcemException(ErrorCode.MOTORS_NOT_FOUND, "Motor not found: " + motorId);
        }

        boolean alreadyDiscardedEverywhere = batches.stream()
                .allMatch(b -> BatchMotorDiscardHelper.isDiscarded(b, motorId));
        if (alreadyDiscardedEverywhere) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Motor " + motorId + " is already discarded");
        }

        PersonInfo discardedBy = PersonInfo.builder()
                .id(userId)
                .fullName(admin.getUsername())
                .build();
        LocalDateTime now = LocalDateTime.now();
        List<String> affected = new ArrayList<>();

        for (Batch batch : batches) {
            BatchMotorDiscardHelper.markDiscarded(batch, motorId, reason, discardedBy, now);
            batch.setUpdatedAt(now);
            batch.setUpdatedBy(discardedBy);
            mongoTemplate.save(batch);
            affected.add(batch.getBatchId());
        }

        return MotorDiscardResponse.builder()
                .motorId(motorId)
                .reason(reason)
                .affectedBatchCount(affected.size())
                .affectedBatchIds(affected)
                .build();
    }

    private List<MotorSummaryDto> buildMotorSummaries(
            MotorListFiltersDto filters, String search, String systemManagerId) {
        List<Batch> batches = loadBatches(filters, systemManagerId);
        Map<String, List<Batch>> batchesByMotor = new LinkedHashMap<>();

        for (Batch batch : batches) {
            List<String> motorIds = batch.getMotorIds();
            if (motorIds == null || motorIds.isEmpty()) continue;
            for (String rawMotorId : motorIds) {
                String motorId = String.valueOf(rawMotorId == null ? "" : rawMotorId).trim();
                if (motorId.isEmpty()) continue;
                batchesByMotor.computeIfAbsent(motorId, ignored -> new ArrayList<>()).add(batch);
            }
        }

        List<String> motorIds = new ArrayList<>(batchesByMotor.keySet());
        Map<String, RocketMotorCasing> casingByMotorId = loadCasings(motorIds);

        List<MotorSummaryDto> rows = new ArrayList<>();
        for (Map.Entry<String, List<Batch>> entry : batchesByMotor.entrySet()) {
            String motorId = entry.getKey();
            List<Batch> linkedBatchesRaw = entry.getValue();

            List<BatchLink> links = linkedBatchesRaw.stream()
                    .map(batch -> toBatchLink(batch, motorId))
                    .sorted(Comparator
                            .comparing((BatchLink l) -> l.updatedAt() == null ? LocalDateTime.MIN : l.updatedAt())
                            .reversed())
                    .collect(Collectors.toList());

            BatchLink primary = links.get(0);
            RocketMotorCasing casing = resolveCasing(casingByMotorId, motorId);

            List<MotorLinkedBatchDto> linkedBatches = links.stream()
                    .map(BatchLink::toDto)
                    .collect(Collectors.toList());

            boolean discarded = links.stream().anyMatch(BatchLink::discarded);
            String discardReason = links.stream()
                    .map(BatchLink::discardReason)
                    .filter(AdminMotorService::isNotBlank)
                    .findFirst()
                    .orElse(null);

            String lifecycleStatus = resolveLifecycleStatus(linkedBatchesRaw, motorId);

            PersonInfo createdBy = casingCreatedBy(casing);
            LocalDateTime createdAt = casing != null ? casing.getCreatedAt() : null;
            // Fallback to primary linked batch audit when casing row is missing/incomplete
            if (createdBy == null) {
                createdBy = primary.createdBy();
            }
            if (createdAt == null) {
                createdAt = primary.createdAt();
            }

            rows.add(MotorSummaryDto.builder()
                    .motorId(motorId)
                    .batchIds(links.stream().map(BatchLink::batchId).distinct().collect(Collectors.toList()))
                    .linkedBatchCount(linkedBatches.size())
                    .linkedBatches(linkedBatches)
                    .primaryBatchId(primary.batchId())
                    .projectId(primary.projectId())
                    .motorStage(primary.motorStage())
                    .batchType(primary.batchType())
                    .workflowStatus(lifecycleStatus)
                    .workflowSubmissionType(primary.workflowSubmissionType())
                    .motorCasingId(casing != null ? casing.getMotorCasingId() : null)
                    .casingStatus(casing != null && casing.getStatus() != null ? casing.getStatus().name() : null)
                    .discarded(discarded)
                    .discardReason(discardReason)
                    .createdBy(createdBy)
                    .createdAt(createdAt)
                    .build());
        }

        return rows.stream()
                .filter(row -> matchesFilters(row, filters, search))
                .sorted(Comparator.comparing(MotorSummaryDto::getMotorId, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Motor Management list/status: DISCARDED, DISPATCHED, or IN_PROGRESS (same rules as stats).
     * Returns null when the motor has not started work yet (still TO_BE_INITIATED everywhere).
     */
    private static String resolveLifecycleStatus(List<Batch> linkedBatches, String motorId) {
        if (linkedBatches == null || linkedBatches.isEmpty()) {
            return null;
        }
        boolean discarded = linkedBatches.stream()
                .anyMatch(batch -> BatchMotorDiscardHelper.isDiscarded(batch, motorId));
        if (discarded) {
            return Status.DISCARDED.name();
        }
        boolean dispatched = linkedBatches.stream()
                .anyMatch(batch -> MotorBatchLifecycleHelper.isDispatched(batch, motorId));
        if (dispatched) {
            return "DISPATCHED";
        }
        boolean started = linkedBatches.stream()
                .anyMatch(batch -> MotorBatchLifecycleHelper.hasStarted(batch, motorId));
        return started ? Status.IN_PROGRESS.name() : null;
    }

    private List<Batch> loadBatches(MotorListFiltersDto filters, String systemManagerId) {
        List<Criteria> and = new ArrayList<>();
        and.add(Criteria.where("isDeleted").ne(true));
        and.add(Criteria.where("motorIds.0").exists(true));

        DateRange range = resolveDateRange(
                filters != null ? filters.getFilterType() : null,
                filters != null ? filters.getStartDate() : null,
                filters != null ? filters.getEndDate() : null);
        if (range != null) {
            and.add(Criteria.where("createdAt").gte(range.start()).lte(range.end()));
        }

        if (filters != null && isNotBlank(filters.getBatchId())) {
            and.add(Criteria.where("batchId").regex(escapeRegex(filters.getBatchId().trim()), "i"));
        }
        if (filters != null && isNotBlank(filters.getProjectId())) {
            and.add(Criteria.where("projectId").is(filters.getProjectId().trim()));
        }
        if (filters != null && filters.getMotorStage() != null) {
            and.add(Criteria.where("motorStage").is(filters.getMotorStage()));
        }
        if (isNotBlank(systemManagerId)) {
            String managerId = systemManagerId.trim();
            and.add(new Criteria().orOperator(
                    Criteria.where("systemManager.id").is(managerId),
                    Criteria.where("systemManager._id").is(managerId)));
        }

        Query query = new Query(new Criteria().andOperator(and.toArray(new Criteria[0])));
        return mongoTemplate.find(query, Batch.class);
    }

    /** When the caller is a System Manager, scope motors to batches assigned to them. */
    private static String resolveSystemManagerScope(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }
        boolean isSystemManager = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(SYSTEM_MANAGER_AUTHORITY::equals);
        if (!isSystemManager) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details == null) {
            return null;
        }
        String userId = String.valueOf(details).trim();
        return userId.isEmpty() ? null : userId;
    }

    private Map<String, RocketMotorCasing> loadCasings(List<String> motorIds) {
        if (motorIds == null || motorIds.isEmpty()) return Map.of();
        Map<String, RocketMotorCasing> map = new HashMap<>();

        List<RocketMotorCasing> found = new ArrayList<>();
        List<RocketMotorCasing> active =
                rocketMotorCasingRepository.findByMotorIdInAndActiveTrue(motorIds);
        if (active != null) {
            found.addAll(active);
        }
        if (found.isEmpty()) {
            // Fallback: include inactive / null-active rows for audit fields
            found.addAll(mongoTemplate.find(
                    new Query(Criteria.where("motorId").in(motorIds)),
                    RocketMotorCasing.class));
        }
        // Case-insensitive fill for any still-missing ids
        Set<String> foundKeys = found.stream()
                .map(RocketMotorCasing::getMotorId)
                .filter(AdminMotorService::isNotBlank)
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<String> missing = motorIds.stream()
                .filter(AdminMotorService::isNotBlank)
                .map(String::trim)
                .filter(id -> !foundKeys.contains(id.toLowerCase(Locale.ROOT)))
                .toList();
        if (!missing.isEmpty()) {
            List<Criteria> or = new ArrayList<>();
            for (String id : missing) {
                or.add(Criteria.where("motorId").regex("^" + escapeRegex(id) + "$", "i"));
            }
            found.addAll(mongoTemplate.find(
                    new Query(new Criteria().orOperator(or.toArray(new Criteria[0]))),
                    RocketMotorCasing.class));
        }

        for (RocketMotorCasing casing : found) {
            if (casing.getMotorId() == null || casing.getMotorId().isBlank()) continue;
            String key = casing.getMotorId().trim();
            map.putIfAbsent(key, casing);
            map.putIfAbsent(key.toLowerCase(Locale.ROOT), casing);
        }
        return map;
    }

    private static RocketMotorCasing resolveCasing(Map<String, RocketMotorCasing> byMotorId, String motorId) {
        if (byMotorId == null || !isNotBlank(motorId)) {
            return null;
        }
        String key = motorId.trim();
        RocketMotorCasing casing = byMotorId.get(key);
        if (casing != null) {
            return casing;
        }
        return byMotorId.get(key.toLowerCase(Locale.ROOT));
    }

    private boolean matchesFilters(MotorSummaryDto row, MotorListFiltersDto filters, String search) {
        if (isNotBlank(search)) {
            String q = search.trim().toLowerCase(Locale.ROOT);
            boolean hit =
                    containsIgnoreCase(row.getMotorId(), q)
                            || containsIgnoreCase(row.getPrimaryBatchId(), q)
                            || (row.getBatchIds() != null
                            && row.getBatchIds().stream().anyMatch(id -> containsIgnoreCase(id, q)))
                            || containsIgnoreCase(row.getProjectId(), q)
                            || containsIgnoreCase(row.getMotorCasingId(), q);
            if (!hit) return false;
        }
        if (filters == null) return true;
        if (isNotBlank(filters.getWorkflowStatus())) {
            String wanted = filters.getWorkflowStatus().trim().toUpperCase(Locale.ROOT);
            if (!wanted.equalsIgnoreCase(String.valueOf(row.getWorkflowStatus()))) {
                return false;
            }
        }
        return true;
    }

    private BatchLink toBatchLink(Batch batch, String motorId) {
        boolean discarded = BatchMotorDiscardHelper.isDiscarded(batch, motorId);
        Batch.DiscardedMotor discardedEntry = BatchMotorDiscardHelper.findDiscarded(batch, motorId);
        StatusPair stageStatus = discarded
                ? new StatusPair(Status.DISCARDED.name(), null)
                : resolveMotorStatus(batch, motorId);
        String lifecycleStatus = resolveLifecycleStatus(List.of(batch), motorId);
        LocalDateTime updated = batch.getUpdatedAt() != null ? batch.getUpdatedAt() : batch.getCreatedAt();
        return new BatchLink(
                batch.getBatchId(),
                batch.getBatchType(),
                batch.getProjectId(),
                batch.getMotorStage(),
                lifecycleStatus,
                stageStatus.submissionType(),
                updated,
                discarded,
                discardedEntry != null ? discardedEntry.getReason() : null,
                batch.getCreatedAt(),
                batch.getCreatedBy());
    }

    private static PersonInfo casingCreatedBy(RocketMotorCasing casing) {
        if (casing == null) {
            return null;
        }
        String id = casing.getCreatedByUserId();
        String name = casing.getCreatedByUsername();
        if (!isNotBlank(id) && !isNotBlank(name)) {
            return null;
        }
        return PersonInfo.builder()
                .id(isNotBlank(id) ? id.trim() : null)
                .fullName(isNotBlank(name) ? name.trim() : null)
                .build();
    }

    private StatusPair resolveMotorStatus(Batch batch, String motorId) {
        if (BatchMotorDiscardHelper.isDiscarded(batch, motorId)) {
            return new StatusPair(Status.DISCARDED.name(), null);
        }
        List<Batch.StageProgress> stages = batch.getCurrentStage();
        if (stages == null || stages.isEmpty()) {
            stages = batch.getStageProgress();
        }
        if (stages != null) {
            for (int i = stages.size() - 1; i >= 0; i--) {
                Batch.StageProgress stage = stages.get(i);
                if (stage == null || stage.getMotorStatuses() == null) continue;
                for (MotorStatusResponse motorStatus : stage.getMotorStatuses()) {
                    if (motorStatus == null) continue;
                    if (!motorId.equalsIgnoreCase(String.valueOf(motorStatus.getMotorId()).trim())) continue;
                    String status = motorStatus.getMotorSubmissionStatus() != null
                            ? motorStatus.getMotorSubmissionStatus().name()
                            : Status.TO_BE_INITIATED.name();
                    String submission = motorStatus.getMotorSubmissionType() != null
                            ? motorStatus.getMotorSubmissionType().name()
                            : null;
                    return new StatusPair(status, submission);
                }
            }
        }
        return new StatusPair(Status.TO_BE_INITIATED.name(), null);
    }

    private DateRange resolveDateRange(String filterType, String startDate, String endDate) {
        String type = String.valueOf(filterType == null ? "" : filterType).trim().toLowerCase(Locale.ROOT);
        if (type.isEmpty() || "all".equals(type)) {
            return null;
        }
        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        LocalDate today = LocalDate.now();
        switch (type) {
            case "day", "today" -> start = today.atStartOfDay();
            case "week" -> start = today.minusDays(6).atStartOfDay();
            case "month" -> start = today.withDayOfMonth(1).atStartOfDay();
            case "custom" -> {
                if (!isNotBlank(startDate) || !isNotBlank(endDate)) return null;
                start = DateTimeConversionUtil.parseLocalDate(startDate).atStartOfDay();
                end = DateTimeConversionUtil.parseLocalDate(endDate).atTime(23, 59, 59);
            }
            default -> {
                return null;
            }
        }
        return new DateRange(start, end);
    }

    private static StatMetric metric(long count) {
        return StatMetric.builder().count(count).subValue(count).build();
    }

    private static boolean containsIgnoreCase(String value, String needleLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needleLower);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String firstNonBlank(String a, String b) {
        if (isNotBlank(a)) return a.trim();
        if (isNotBlank(b)) return b.trim();
        return null;
    }

    private static String escapeRegex(String value) {
        return value.replaceAll("([\\\\.*+?^${}()|\\[\\]])", "\\\\$1");
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }

    private record StatusPair(String status, String submissionType) {
    }

    private record BatchLink(
            String batchId,
            String batchType,
            String projectId,
            Integer motorStage,
            String workflowStatus,
            String workflowSubmissionType,
            LocalDateTime updatedAt,
            boolean discarded,
            String discardReason,
            LocalDateTime createdAt,
            PersonInfo createdBy) {
        MotorLinkedBatchDto toDto() {
            return MotorLinkedBatchDto.builder()
                    .batchId(batchId)
                    .batchType(batchType)
                    .projectId(projectId)
                    .motorStage(motorStage)
                    .workflowStatus(workflowStatus)
                    .workflowSubmissionType(workflowSubmissionType)
                    .createdAt(createdAt)
                    .createdBy(createdBy)
                    .build();
        }
    }
}
