package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageImpl;

@Repository
@RequiredArgsConstructor
public class SystemManagerDashboardRepository {

    private final MongoTemplate mongoTemplate;
//1.STATS
    // Total Batches
    public long countTotalBatches(LocalDateTime start, LocalDateTime end) {
        Query query = new Query();

        query.addCriteria(
                Criteria.where("isDeleted").ne(true)
                        .and("createdAt").gte(start).lte(end)
        );

        return mongoTemplate.count(query, Batch.class);
    }

    // In Progress Batches
    public long countInProgressBatches(LocalDateTime start, LocalDateTime end) {
        Query query = new Query();

        query.addCriteria(
                Criteria.where("isDeleted").ne(true)
                        .and("createdAt").gte(start).lte(end)
                        .and("currentStage").elemMatch(
                                Criteria.where("status").is(Status.IN_PROGRESS))
        );

        return mongoTemplate.count(query, Batch.class);
    }

    // Completed Batches
    public long countCompletedBatches(LocalDateTime start, LocalDateTime end) {
        Query query = new Query();

        query.addCriteria(new Criteria().andOperator(
                Criteria.where("isDeleted").ne(true),
                Criteria.where("createdAt").gte(start).lte(end),
                completedBatchCriteria()));

        return mongoTemplate.count(query, Batch.class);
    }

    // Pending Approvals
    public long countPendingApprovals(LocalDateTime start, LocalDateTime end) {
        Query query = new Query();

        query.addCriteria(
                Criteria.where("isDeleted").ne(true)
                        .and("createdAt").gte(start).lte(end)
                        .and("currentStage").elemMatch(
                                Criteria.where("status").in(
                                        Status.WAITING_FOR_APPROVAL,
                                        Status.WAITING_FOR_PARTIAL_APPROVAL))
        );

        return mongoTemplate.count(query, Batch.class);
    }

    // Rejected Batches
    public long countRejectedBatches(LocalDateTime start, LocalDateTime end) {
        Query query = new Query();

        query.addCriteria(
                Criteria.where("isDeleted").ne(true)
                        .and("createdAt").gte(start).lte(end)
                        .and("currentStage").elemMatch(
                                Criteria.where("status").is(Status.REJECTED))
        );

        return mongoTemplate.count(query, Batch.class);
    }
    //2.CHART DATA
    public List<Batch> findBatchesBetween(
            LocalDateTime start,
            LocalDateTime end) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where("isDeleted").ne(true)
                        .and("createdAt").gte(start).lte(end)
        );

        return mongoTemplate.find(query, Batch.class);
    }
    //3.ACTIVE BATCHES
    public Page<Batch> findActiveBatches(
            String managerId,
            String search,
            String priority,
            Status status,
            String listType,
            Long departmentId,
            Long subDepartmentId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        Query query = new Query();
        Criteria activeStageCriteria = new Criteria();
        boolean hasActiveStageFilter = false;

        query.addCriteria(
                Criteria.where("isDeleted").ne(true)
        );
        query.addCriteria(
                Criteria.where("createdAt")
                        .gte(startDate)
                        .lte(endDate)
        );

        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        if (search != null && !search.isBlank()) {

            query.addCriteria(
                    new Criteria().orOperator(
                            Criteria.where("projectName")
                                    .regex(search, "i"),
                            Criteria.where("batchId")
                                    .regex(search, "i")
                    )
            );
        }

        // Priority filter
        if (priority != null && !priority.isBlank()) {

            query.addCriteria(
                    Criteria.where("priority").is(priority)
            );
        }

        // Status filter
        if (status != null) {
            activeStageCriteria.and("status").is(status);
            hasActiveStageFilter = true;
        }
        // List Type Filter
        if (listType != null && !listType.isBlank() && !"All".equalsIgnoreCase(listType)) {

            if ("COMPLETED".equalsIgnoreCase(listType)) {
                query.addCriteria(completedBatchCriteria());

            } else if ("IN_PROGRESS".equalsIgnoreCase(listType)) {
                query.addCriteria(new Criteria().norOperator(completedBatchCriteria()));
            }
        }
        // Department filter
        if (departmentId != null) {
            activeStageCriteria.and("departmentId").is(departmentId);
            hasActiveStageFilter = true;
        }

        // Sub Department filter
        if (subDepartmentId != null) {
            activeStageCriteria.and("subDepartmentId").is(subDepartmentId);
            hasActiveStageFilter = true;
        }
        if (hasActiveStageFilter) {
            query.addCriteria(
                    Criteria.where("currentStage").elemMatch(activeStageCriteria));
        }


        long totalRecords =
                mongoTemplate.count(query, Batch.class);

        query.with(pageable);

        List<Batch> batches =
                mongoTemplate.find(query, Batch.class);

        return new PageImpl<>(
                batches,
                pageable,
                totalRecords
        );
}

    private Criteria completedBatchCriteria() {
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
