package in.co.bel.acempp.repository;
import in.co.bel.acempp.entity.batch.BlockchainEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface SysMgrBlockchainEventRepository extends MongoRepository<BlockchainEvent, String> {

    default Page<BlockchainEvent> findManagerEventsWithFilters(
            String systemManagerId,
            String search,
            String eventType,
            String department,
            String subDepartment,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable) {

        MongoTemplate mongoTemplate =
                ApplicationContextProvider.getBean(MongoTemplate.class);

        List<Criteria> andCriteria = new ArrayList<>();

        // System Manager Filter
        if (systemManagerId != null
                && !systemManagerId.isBlank()
                && !"All".equalsIgnoreCase(systemManagerId)) {

            andCriteria.add(
                    Criteria.where("systemManagerId").is(systemManagerId)
            );
        }

        // Search Filter (Batch Id / Transaction Id)
        if (search != null
                && !search.isBlank()) {

            andCriteria.add(
                    new Criteria().orOperator(
                            Criteria.where("batchId")
                                    .regex(search.trim(), "i"),

                            Criteria.where("transactionId")
                                    .regex(search.trim(), "i")
                    )
            );
        }

        // Event Type
        if (eventType != null
                && !eventType.isBlank()
                && !"All".equalsIgnoreCase(eventType)) {

            andCriteria.add(
                    Criteria.where("eventType").is(eventType)
            );
        }

        // Department
        if (department != null
                && !department.isBlank()
                && !"All".equalsIgnoreCase(department)) {

            andCriteria.add(
                    Criteria.where("department").is(department)
            );
        }

        // Sub Department
        if (subDepartment != null
                && !subDepartment.isBlank()
                && !"All".equalsIgnoreCase(subDepartment)) {

            andCriteria.add(
                    Criteria.where("subDepartment").is(subDepartment)
            );
        }

        // Start Date
        if (start != null) {
            andCriteria.add(
                    Criteria.where("timestamp").gte(start)
            );
        }

        // End Date
        if (end != null) {
            andCriteria.add(
                    Criteria.where("timestamp").lte(end)
            );
        }

        Criteria criteria = andCriteria.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(andCriteria.toArray(new Criteria[0]));

        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .with(pageable);

        long total = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1),
                BlockchainEvent.class);

        List<BlockchainEvent> events =
                mongoTemplate.find(query, BlockchainEvent.class);

        return PageableExecutionUtils.getPage(
                events,
                pageable,
                () -> total);
    }
}