package in.co.bel.acempp.repository;

import in.co.bel.acempp.dto.batch.*;
import in.co.bel.acempp.dto.general.StatusCountsDto;
import in.co.bel.acempp.dto.project.ProjectIdProjection;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
//import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public interface BatchRepository extends MongoRepository<Batch, String> {
    Optional<Batch> findByBatchId(String batchId);

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
            "{ '$match' : { 'isDeleted' : false, 'projectId' : { '$nin' : [null, ''] } } }",
            "{ '$group' : { '_id' : '$projectId' } }",
            "{ '$project' : { '_id' : 0, 'projectId' : '$_id' } }"
    })
    List<ProjectIdProjection> findDistinctProjectIds();

    boolean existsByProjectIdAndIsDeletedFalse(String projectId);

    Optional<Batch> findByBatchIdAndIsDeletedFalse(String batchId);

    default long countByStageProgressSubDepartmentIdAndStatusIn(Long subDeptId, Collection<Status> statuses) {
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("isDeleted").ne(true)),
                Aggregation.unwind("stageProgress"),
                Aggregation.match(
                        Criteria.where("stageProgress.subDepartmentId").is(subDeptId)
                                .and("stageProgress.status").in(statuses)
                ),
                Aggregation.count().as("count")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, Batch.class, Map.class);
        List<Map> mapped = results.getMappedResults();
        if (mapped.isEmpty()) {
            return 0L;
        }
        Object raw = mapped.get(0).get("count");
        return raw instanceof Number ? ((Number) raw).longValue() : 0L;
    }

    default Page<Batch> findBatchesWithFilters(BatchListRequest request) {
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);

        BatchFiltersDto filters = request.getFilters();
        List<Criteria> andCriteria = new ArrayList<>();
        andCriteria.add(Criteria.where("isDeleted").ne(true));
        // search – matches batchId or motorIds (case‑insensitive)
        if (filters != null && isNotBlank(filters.getSearch())) {
            String regex = filters.getSearch().trim();
            andCriteria.add(new Criteria().orOperator(
                    Criteria.where("batchId").regex(regex, "i"),
                    Criteria.where("motorIds").regex(regex, "i")       // any motorId contains text
            ));
        }

        Criteria stageCriteria = new Criteria();
        boolean hasStageCriteria = false;
        if (request.getSubDepartmentId() != null) {
            stageCriteria.and("subDepartmentId").is(request.getSubDepartmentId());
            hasStageCriteria = true;
        }

        if (filters != null) {
            if (filters.getStatus() != null) {
                stageCriteria.and("status").is(filters.getStatus());
                hasStageCriteria = true;
            }

            if (isNotBlank(filters.getPriority())) {
                andCriteria.add(
                        Criteria.where("priority")
                                .is(filters.getPriority())
                );
            }

            if (filters.getMotorStage() != null) {
                andCriteria.add(
                        Criteria.where("motorStage")
                                .is(filters.getMotorStage())
                );
            }

            if (isNotBlank(filters.getDepartment())) {
                stageCriteria.and("departmentName").is(filters.getDepartment());
                hasStageCriteria = true;
            }

            if (isNotBlank(filters.getSubDepartment())) {
                stageCriteria.and("subDepartmentName").is(filters.getSubDepartment());
                hasStageCriteria = true;
            }
            if (filters.getMotorIds() != null && !filters.getMotorIds().isEmpty()) {
                andCriteria.add(
                        Criteria.where("motorIds")
                                .in(filters.getMotorIds())
                );
            }

            if (filters.getLotIds() != null && !filters.getLotIds().isEmpty()) {
                andCriteria.add(
                        Criteria.where("identificationSheet.materials")
                                .elemMatch(
                                        Criteria.where("lotId")
                                                .in(filters.getLotIds())
                                )
                );
            }
            if (filters != null) {

                LocalDateTime startDateTime = null;
                LocalDateTime endDateTime = null;

                String filterType = filters.getFilterType();

                if (filterType != null) {

                    switch (filterType.toLowerCase()) {

                        case "day":
                            startDateTime = LocalDate.now().atStartOfDay();
                            endDateTime = LocalDate.now().atTime(LocalTime.MAX);
                            break;

                        case "week":
                            startDateTime = LocalDate.now()
                                    .with(java.time.DayOfWeek.MONDAY)
                                    .atStartOfDay();
                            endDateTime = LocalDate.now()
                                    .with(java.time.DayOfWeek.SUNDAY)
                                    .atTime(LocalTime.MAX);
                            break;

                        case "month":
                            startDateTime = LocalDate.now()
                                    .withDayOfMonth(1)
                                    .atStartOfDay();
                            endDateTime = LocalDate.now()
                                    .withDayOfMonth(LocalDate.now().lengthOfMonth())
                                    .atTime(LocalTime.MAX);
                            break;

                        case "custom":
                            if (isNotBlank(filters.getStartDate())
                                    && isNotBlank(filters.getEndDate())) {

                                startDateTime = DateTimeConversionUtil.parseLocalDate(filters.getStartDate())
                                        .atStartOfDay();

                                endDateTime = DateTimeConversionUtil.parseLocalDate(filters.getEndDate())
                                        .atTime(LocalTime.MAX);
                            }
                            break;
                    }
                } else if (isNotBlank(filters.getStartDate())
                        && isNotBlank(filters.getEndDate())) {

                    startDateTime = DateTimeConversionUtil.parseLocalDate(filters.getStartDate())
                            .atStartOfDay();

                    endDateTime = DateTimeConversionUtil.parseLocalDate(filters.getEndDate())
                            .atTime(LocalTime.MAX);
                }

                if (startDateTime != null && endDateTime != null) {
                    andCriteria.add(
                            Criteria.where("createdAt")
                                    .gte(startDateTime)
                                    .lte(endDateTime)
                    );
                }
            }
        }
        if (hasStageCriteria) {
            andCriteria.add(Criteria.where(
                            request.getSubDepartmentId() != null
                                    ? "stageProgress"
                                    : "currentStage")
                    .elemMatch(stageCriteria));
        }

        Criteria finalCriteria = andCriteria.isEmpty() ? new Criteria()
                : new Criteria().andOperator(andCriteria.toArray(new Criteria[0]));

        // Sort
        BatchSortDto sortInfo = request.getSort();
        String sortField = "createdAt";  // default
        if (sortInfo != null && "createdOn".equalsIgnoreCase(sortInfo.getField())) {
            sortField = "createdAt";
        }
        Sort.Direction direction = (sortInfo != null && "asc".equalsIgnoreCase(sortInfo.getOrder()))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(request.getPagination().getPage() - 1,
                request.getPagination().getLimit(),
                Sort.by(direction, sortField));

        Query query = Query.query(finalCriteria).with(pageable);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Batch.class);
        List<Batch> list = mongoTemplate.find(query, Batch.class);

        return PageableExecutionUtils.getPage(list, pageable, () -> count);
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    default Page<Batch> findUserSubDeptBatches(
            Long subDeptId,
            String userId,
            String search,
            List<String> statuses,
            List<String> priorities,
            List<String> batchIds,
            List<String> batchTypes,
            List<Object> motorStages,
            List<String> motorIds,
            List<String> formSubmittedBy,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        List<Criteria> andCriteria = new ArrayList<>();
        andCriteria.add(Criteria.where("isDeleted").ne(true));

        // Single elemMatch that combines subDeptId and optional status filter
        Criteria stageCrit = Criteria.where("subDepartmentId").is(subDeptId);
        if (statuses != null && !statuses.isEmpty()) {
            List<String> mapped = statuses.stream()
                    .map(s -> s.replace(" ", "_").toUpperCase()) // e.g. "In Progress" → "IN_PROGRESS"
                    .collect(Collectors.toList());
            stageCrit.and("status").in(mapped);
        }
        if (formSubmittedBy != null && !formSubmittedBy.isEmpty()) {
            stageCrit.and("submittedBy.id").in(formSubmittedBy);
        }
        andCriteria.add(Criteria.where("stageProgress").elemMatch(stageCrit));

        if (search != null && !search.isBlank()) {
            String regex = search.trim();
            andCriteria.add(new Criteria().orOperator(
                    Criteria.where("batchId").regex(regex, "i"),
                    Criteria.where("motorIds").regex(regex, "i")
            ));
        }

        if (priorities != null && !priorities.isEmpty()) {
            andCriteria.add(Criteria.where("priority").in(priorities));
        }
        // Batch IDs
        if (batchIds != null && !batchIds.isEmpty()) {
            andCriteria.add(Criteria.where("batchId").in(batchIds));
        }

        // Batch Types
        if (batchTypes != null && !batchTypes.isEmpty()) {
            andCriteria.add(Criteria.where("batchType").in(batchTypes));
        }

        // Motor Stages
        if (motorStages != null && !motorStages.isEmpty()) {
            andCriteria.add(Criteria.where("motorStage").in(motorStages));
        }

        // Motor IDs
        if (motorIds != null && !motorIds.isEmpty()) {
            andCriteria.add(Criteria.where("motorIds").in(motorIds));
        }

        // Date filter
        if (fromDate != null || toDate != null) {
            Criteria createdDateCriteria = Criteria.where("createdAt");
            if (fromDate != null) {
                createdDateCriteria.gte(fromDate.atStartOfDay());
            }
            if (toDate != null) {
                createdDateCriteria.lt(toDate.plusDays(1).atStartOfDay());
            }
            andCriteria.add(createdDateCriteria);
        }

        Criteria finalCriteria = new Criteria().andOperator(andCriteria.toArray(new Criteria[0]));
        Query query = Query.query(finalCriteria).with(pageable);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Batch.class);
        List<Batch> list = mongoTemplate.find(query, Batch.class);
        return PageableExecutionUtils.getPage(list, pageable, () -> count);
    }

    default StatusCountsDto getStatusCounts(Long subDeptId) {
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("isDeleted").ne(true)),
                Aggregation.unwind("stageProgress"),
                Aggregation.match(Criteria.where("stageProgress.subDepartmentId").is(subDeptId)),
                Aggregation.group("stageProgress.status").count().as("count")
        );

        AggregationResults<Map> results =
                mongoTemplate.aggregate(agg, Batch.class, Map.class);

        Map<String, Long> map = new HashMap<>();

        results.forEach(entry -> {
            String status = (String) entry.get("_id");
            Object rawCount = entry.get("count");

            Long count = rawCount instanceof Number
                    ? ((Number) rawCount).longValue()
                    : 0L;

            if (status != null) {
                map.put(status, count);
            }
        });

        return StatusCountsDto.builder()
                .initiated(map.getOrDefault("TO_BE_INITIATED", 0L))
                .inProgress(map.getOrDefault("IN_PROGRESS", 0L))
                .waitingForPartialApproval(
                        map.getOrDefault("WAITING_FOR_APPROVAL", 0L)
                                + map.getOrDefault("WAITING_FOR_PARTIAL_APPROVAL", 0L)
                )
                .approved(map.getOrDefault("APPROVED", 0L))
                .finalApprovalCompleted(map.getOrDefault("COMPLETELY_APPROVED", 0L))
                .rejected(map.getOrDefault("REJECTED", 0L))
                .build();
    }

    // All non‑deleted
    long countByIsDeletedFalse();

    long countByCreatedAtBetweenAndIsDeletedFalse(LocalDateTime start, LocalDateTime end);

    // By status, excluding deleted

    // ---- stage progress specific ----
    long countByStageProgressSubDepartmentIdAndIsDeletedFalse(Long subDepartmentId);

    default long countByStageProgressSubDepartmentIdAndStatus(Long subDeptId, Status status) {
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("isDeleted").ne(true)),
                Aggregation.unwind("stageProgress"),
                Aggregation.match(Criteria.where("stageProgress.subDepartmentId").is(subDeptId)
                        .and("stageProgress.status").is(status)),
                Aggregation.count().as("count")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, Batch.class, Map.class);
        List<Map> mapped = results.getMappedResults();
        if (mapped.isEmpty()) return 0L;
        Object raw = mapped.get(0).get("count");
        return raw instanceof Long ? (Long) raw
                : raw instanceof Integer ? ((Integer) raw).longValue() : 0L;
    }


    List<Batch> findByCreatedAtBetweenAndIsDeletedFalse(LocalDateTime start, LocalDateTime end);

    long countByIsDeletedFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    default long countDispatchedBatches() {
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("isDeleted").ne(true),
                completedBatchCriteria());
        return mongoTemplate.count(Query.query(criteria), Batch.class);
    }

    // count batches with dispatch stage approved + created between
    default long countDispatchedBetween(LocalDateTime start, LocalDateTime end) {
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("isDeleted").ne(true),
                Criteria.where("createdAt").gte(start).lte(end),
                completedBatchCriteria());
        return mongoTemplate.count(Query.query(criteria), Batch.class);
    }

    // sum of motorIds sizes across dispatched batches
    default long sumMotorsDispatched() {
        // use aggregation
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("isDeleted").ne(true)
                        .and("stageProgress").elemMatch(
                                Criteria.where("subDepartmentId").is(13L)
                                        .and("status").is(Status.COMPLETELY_APPROVED))),
                Aggregation.project().and("motorIds").size().as("motorCount"),
                Aggregation.group().sum("motorCount").as("total")
        );
        AggregationResults<Map> result = mongoTemplate.aggregate(agg, Batch.class, Map.class);
        Map<String, Object> map = result.getUniqueMappedResult();
        if (map == null) return 0L;
        Object total = map.get("total");
        return total instanceof Number ? ((Number) total).longValue() : 0L;
    }

    // count open batches (not dispatched)
    default long countOpenBatches() {
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("isDeleted").ne(true),
                new Criteria().norOperator(completedBatchCriteria()));
        return mongoTemplate.count(Query.query(criteria), Batch.class);
    }

    // count open batches in range
    default long countOpenBatchesBetween(LocalDateTime start, LocalDateTime end) {
        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("isDeleted").ne(true),
                Criteria.where("createdAt").gte(start).lte(end),
                new Criteria().norOperator(completedBatchCriteria()));
        return mongoTemplate.count(Query.query(criteria), Batch.class);
    }

    //    long sumMotorsDispatchedBetween(LocalDateTime start, LocalDateTime end);
    default Page<Batch> findActiveBatches(
            String search,
            String stage,
            String type,
            String status,
            String listType,
            LocalDateTime start,
            LocalDateTime end,
            boolean currentMonth,
            Pageable pageable) {

        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);

        List<Criteria> andCriteria = new ArrayList<>();
        List<Criteria> activeStageFilters = new ArrayList<>();

        // Exclude deleted batches
        andCriteria.add(Criteria.where("isDeleted").ne(true));

        // Search
        if (search != null && !search.isBlank()) {
            String regex = search.trim();
            andCriteria.add(new Criteria().orOperator(
                    Criteria.where("batchId").regex(regex, "i"),
                    Criteria.where("motorIds").regex(regex, "i")
            ));
        }

        // Stage Filter
        if (stage != null && !stage.isBlank() && !"All".equalsIgnoreCase(stage)) {
            activeStageFilters.add(new Criteria().orOperator(
                    Criteria.where("departmentName").is(stage),
                    Criteria.where("subDepartmentName").is(stage)
            ));
        }

        // Batch Type Filter
        if (type != null && !type.isBlank() && !"All".equalsIgnoreCase(type)) {
            andCriteria.add(Criteria.where("batchType").is(type));
        }

        // Status Filter
        if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status)) {
            activeStageFilters.add(Criteria.where("status").is(status));
        }
        if (!activeStageFilters.isEmpty()) {
            andCriteria.add(Criteria.where("currentStage").elemMatch(
                    new Criteria().andOperator(
                            activeStageFilters.toArray(new Criteria[0]))));
        }

        // List Type Filter
        if (listType != null && !listType.isBlank() && !"All".equalsIgnoreCase(listType)) {

            if ("COMPLETED".equalsIgnoreCase(listType)) {
                andCriteria.add(completedBatchCriteria());

            } else if ("IN_PROGRESS".equalsIgnoreCase(listType)) {
                andCriteria.add(new Criteria().norOperator(completedBatchCriteria()));
            }
        }

        // Date Filter
        if (currentMonth) {
            LocalDate today = LocalDate.now();
            start = today.withDayOfMonth(1).atStartOfDay();
            end = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        } else {
            // Since start and end are already LocalDateTime, extract LocalDate directly safely:
            LocalDate parsedStart = (start != null) ? start.toLocalDate() : null;
            LocalDate parsedEnd = (end != null) ? end.toLocalDate() : null;

            if ((parsedStart == null) != (parsedEnd == null)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Both startDate and endDate must be provided."
                );
            }

            if (parsedStart != null && parsedEnd != null && parsedEnd.isBefore(parsedStart)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "End date cannot be before start date."
                );
            }

            // Re-apply safe boundaries just in case
            start = (parsedStart != null) ? parsedStart.atStartOfDay() : null;
            end = (parsedEnd != null) ? parsedEnd.atTime(LocalTime.MAX) : null;
        }

        if (start != null && end != null) {
            andCriteria.add(Criteria.where("createdAt").gte(start).lte(end));
        } else if (start != null) {
            andCriteria.add(Criteria.where("createdAt").gte(start));
        } else if (end != null) {
            andCriteria.add(Criteria.where("createdAt").lte(end));
        }

        Criteria finalCriteria = new Criteria().andOperator(
                andCriteria.toArray(new Criteria[0])
        );

        Query query = new Query(finalCriteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .with(pageable);

        long count = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1),
                Batch.class
        );

        List<Batch> batches = mongoTemplate.find(query, Batch.class);

        return PageableExecutionUtils.getPage(
                batches,
                pageable,
                () -> count
        );
    }
    default Page<Batch> findBatchesByBlockchainIdsAndStatus(
            List<String> distinctBatchIds,
            String search,
            String listType,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable) {

        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);

        List<Criteria> andCriteria = new ArrayList<>();
        List<Criteria> activeStageFilters = new ArrayList<>();

        // Exclude deleted batches
        andCriteria.add(Criteria.where("isDeleted").ne(true));

        // Restrict strictly to unique batch IDs extracted from blockchain events collection
        if (distinctBatchIds != null && !distinctBatchIds.isEmpty()) {
            andCriteria.add(Criteria.where("batchId").in(distinctBatchIds));
        } else {
            return PageableExecutionUtils.getPage(Collections.emptyList(), pageable, () -> 0L);
        }

        // Search
        if (search != null && !search.isBlank()) {
            String regex = search.trim();
            andCriteria.add(new Criteria().orOperator(
                    Criteria.where("batchId").regex(regex, "i"),
                    Criteria.where("motorIds").regex(regex, "i")
            ));
        }

        if (!activeStageFilters.isEmpty()) {
            andCriteria.add(Criteria.where("currentStage").elemMatch(
                    new Criteria().andOperator(
                            activeStageFilters.toArray(new Criteria[0]))));
        }

        // List Type Filter (IN_PROGRESS vs COMPLETED using completedBatchCriteria)
        if (listType != null && !listType.isBlank() && !"All".equalsIgnoreCase(listType)) {
            if ("COMPLETED".equalsIgnoreCase(listType)) {
                andCriteria.add(completedBatchCriteria());
            } else if ("IN_PROGRESS".equalsIgnoreCase(listType)) {
                andCriteria.add(new Criteria().norOperator(completedBatchCriteria()));
            }
        }

        if (start != null && end != null) {
            andCriteria.add(Criteria.where("createdAt").gte(start).lte(end));
        } else if (start != null) {
            andCriteria.add(Criteria.where("createdAt").gte(start));
        } else if (end != null) {
            andCriteria.add(Criteria.where("createdAt").lte(end));
        }

        Criteria finalCriteria = new Criteria().andOperator(
                andCriteria.toArray(new Criteria[0])
        );

        Query query = new Query(finalCriteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .with(pageable);

        long count = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1),
                Batch.class
        );

        List<Batch> batches = mongoTemplate.find(query, Batch.class);

        return PageableExecutionUtils.getPage(
                batches,
                pageable,
                () -> count
        );
    }
    private static Criteria completedBatchCriteria() {
        return new Criteria().orOperator(
                new Criteria().andOperator(
                        Criteria.where("batchType").is("SUBSCALE"),
                        Criteria.where("stageProgress").elemMatch(
                                Criteria.where("subDepartmentId").is(12L)
                                        .and("status").is(Status.COMPLETELY_APPROVED))),
                new Criteria().andOperator(
                        Criteria.where("batchType").ne("SUBSCALE"),
                        Criteria.where("stageProgress").elemMatch(
                                Criteria.where("subDepartmentId").is(13L)
                                        .and("status").is(Status.COMPLETELY_APPROVED)))
        );
    }

}
