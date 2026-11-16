package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.batch.BlockchainEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.*;

public interface BlockchainEventRepository extends MongoRepository<BlockchainEvent, String> {

    Optional<BlockchainEvent> findByTransactionId(String transactionId);

    default Page<BlockchainEvent> findEventsWithFilters(
            String search, String eventType, String department, String subDepartment,
            LocalDateTime start, LocalDateTime end, Pageable pageable) {

        // Avoid constructing an empty Mongo query for the common unfiltered
        // dashboard request. Let Spring Data apply pagination directly to the
        // mapped blockchain_events collection.
        if ((search == null || search.isBlank())
                && (eventType == null || eventType.isBlank() || "All".equalsIgnoreCase(eventType))
                && (department == null || department.isBlank() || "All".equalsIgnoreCase(department))
                && (subDepartment == null || subDepartment.isBlank() || "All".equalsIgnoreCase(subDepartment))
                && start == null && end == null) {
            return findAll(pageable);
        }

        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);
        List<Criteria> andCriteria = new ArrayList<>();

        // Search – match batchId or transactionId
        if (search != null && !search.isBlank()) {
            String regex = search.trim();
            andCriteria.add(new Criteria().orOperator(
                    Criteria.where("batchId").regex(regex, "i"),
                    Criteria.where("transactionId").regex(regex, "i")
            ));
        }

        // Event type filter
        if (eventType != null && !eventType.isBlank() && !"All".equalsIgnoreCase(eventType)) {
            andCriteria.add(Criteria.where("eventType").is(eventType));
        }

        // Department filter
        if (department != null && !department.isBlank() && !"All".equalsIgnoreCase(department)) {
            andCriteria.add(Criteria.where("department").is(department));
        }

        // Sub‑department filter
        if (subDepartment != null && !subDepartment.isBlank() && !"All".equalsIgnoreCase(subDepartment)) {
            andCriteria.add(Criteria.where("subDepartment").is(subDepartment));
        }

        // Date range
        if (start != null) {
            andCriteria.add(Criteria.where("timestamp").gte(start));
        }
        if (end != null) {
            andCriteria.add(Criteria.where("timestamp").lte(end));
        }

        Criteria finalCriteria = andCriteria.isEmpty() ? new Criteria()
                : new Criteria().andOperator(andCriteria.toArray(new Criteria[0]));

        Query query = Query.query(finalCriteria)
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .with(pageable);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), BlockchainEvent.class);
        List<BlockchainEvent> list = mongoTemplate.find(query, BlockchainEvent.class);
        return PageableExecutionUtils.getPage(list, pageable, () -> count);
    }
    default List<String> findDistinctBatchIdsWithFilters(
            String search,
            String subDepartment,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);

        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("batchId").ne(null));

        // 1. Search filter across blockchain events
        if (search != null && !search.isBlank()) {
            String regex = search.trim();
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("batchId").regex(regex, "i"),
                    Criteria.where("assetId").regex(regex, "i"),
                    Criteria.where("eventStatusMessage").regex(regex, "i")
            ));
        }

        // 2. SubDepartment filter
        if (subDepartment != null && !subDepartment.isBlank() && !"All".equalsIgnoreCase(subDepartment)) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("subDepartment").is(subDepartment),
                    Criteria.where("department").is(subDepartment)
            ));
        }

        // 3. Date range filter on blockchain event 'timestamp'
        if (startDate != null && endDate != null) {
            criteriaList.add(Criteria.where("timestamp").gte(startDate).lte(endDate));
        } else if (startDate != null) {
            criteriaList.add(Criteria.where("timestamp").gte(startDate));
        } else if (endDate != null) {
            criteriaList.add(Criteria.where("timestamp").lte(endDate));
        }

        Criteria finalCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(finalCriteria);

        // Fetch distinct batch IDs safely using MongoTemplate
        return mongoTemplate.findDistinct(query, "batchId", BlockchainEvent.class, String.class);
    }

    List<BlockchainEvent> findByBatchIdOrderByTimestampDesc(String batchId);

}
