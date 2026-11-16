package in.co.bel.acempp.repository;
import in.co.bel.acempp.entity.batch.BatchAlert;
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

public interface SysMgrAlertsRepository extends MongoRepository<BatchAlert, String> {

    default Page<BatchAlert> findManagerAlertsWithFilters(
            String systemManagerId,
            String search,
            List<String> types,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable) {

        MongoTemplate mongoTemplate =
                ApplicationContextProvider.getBean(MongoTemplate.class);

        List<Criteria> criteriaList = new ArrayList<>();

        // System Manager
        if (systemManagerId != null
                && !systemManagerId.isBlank()
                && !"All".equalsIgnoreCase(systemManagerId)) {

            criteriaList.add(
                    Criteria.where("systemManagerId").is(systemManagerId)
            );
        }

        // Search (batchId / motorId / message)
        if (search != null && !search.isBlank()) {

            criteriaList.add(
                    new Criteria().orOperator(
                            Criteria.where("batchId").regex(search.trim(), "i"),
                            Criteria.where("motorId").regex(search.trim(), "i"),
                            Criteria.where("msg").regex(search.trim(), "i")
                    )
            );
        }

        // Type filter
        if (types != null && !types.isEmpty()) {

            criteriaList.add(
                    Criteria.where("type").in(types)
            );
        }

        // Date From
        if (start != null) {

            criteriaList.add(
                    Criteria.where("createdAt").gte(start)
            );
        }

        // Date To
        if (end != null) {

            criteriaList.add(
                    Criteria.where("createdAt").lte(end)
            );
        }

        Criteria criteria = criteriaList.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));

        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .with(pageable);

        long total = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1),
                BatchAlert.class);

        List<BatchAlert> alerts =
                mongoTemplate.find(query, BatchAlert.class);

        return PageableExecutionUtils.getPage(
                alerts,
                pageable,
                () -> total);
    }

    long countBySystemManagerId(String systemManagerId);

    long countBySystemManagerIdAndType(
            String systemManagerId,
            String type);
}