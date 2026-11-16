package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.PasswordResetRequest;
import in.co.bel.acempp.enums.ResetRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRequestRepository
        extends MongoRepository<PasswordResetRequest, String> {

    boolean existsByUserIdAndStatus(String userId, ResetRequestStatus status);
    long countByStatus(ResetRequestStatus status);
    Optional<PasswordResetRequest> findByRequestId(String requestId);

    // Default method – dynamic query built safely with Criteria
    default Page<PasswordResetRequest> findRequestsWithFilters(
            String search,
            String status,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable) {

        MongoTemplate mongoTemplate = ApplicationContextProvider.getBean(MongoTemplate.class);

        List<Criteria> criteriaList = new ArrayList<>();

        if (search != null && !search.isEmpty()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("requestId").regex(search, "i"),
                    Criteria.where("userId").regex(search, "i")
            ));
        }

        if (status != null && !status.equals("ALL")) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        if (start != null) {
            criteriaList.add(Criteria.where("requestedAt").gte(start));
        }
        if (end != null) {
            criteriaList.add(Criteria.where("requestedAt").lte(end));
        }

        Criteria finalCriteria = criteriaList.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));

        Query query = Query.query(finalCriteria).with(pageable);

        long count = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1), PasswordResetRequest.class);
        List<PasswordResetRequest> list = mongoTemplate.find(
                query, PasswordResetRequest.class);

        return PageableExecutionUtils.getPage(list, pageable, () -> count);
    }
}