package in.co.bel.acempp.repository;
import in.co.bel.acempp.entity.batch.Batch;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class SysMgrBatchRepositoryImpl implements SysMgrBatchRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Batch> getBatchStatusList(
            String managerId,
            String search,
            int page,
            int limit) {

        Query query = new Query();

        query.addCriteria(Criteria.where("isDeleted").is(false));

        // System Manager filter
        if (managerId != null && !managerId.isBlank()) {
            query.addCriteria(
                    Criteria.where("systemManager._id").is(managerId)
            );
        }

        // Search filter
        if (search != null && !search.trim().isEmpty()) {

            String regex = ".*" + java.util.regex.Pattern.quote(search.trim()) + ".*";

            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("batchId").regex(regex, "i"),
                    Criteria.where("projectName").regex(regex, "i"),
                    Criteria.where("motorIds").regex(regex, "i")
            ));
        }

        query.with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        query.skip((long) (page - 1) * limit);
        query.limit(limit);


        List<Batch> batches = mongoTemplate.find(query, Batch.class);


        return batches;
    }
    @Override
    public Long getBatchStatusCount(
            String managerId,
            String search) {

        Query query = new Query();

        query.addCriteria(Criteria.where("isDeleted").is(false));

        if (managerId != null && !managerId.isBlank()) {
            query.addCriteria(
                    Criteria.where("systemManager.id").is(managerId)
            );
        }

        if (search != null && !search.trim().isEmpty()) {

            String regex = ".*" + Pattern.quote(search.trim()) + ".*";

            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("batchId").regex(regex, "i"),
                    Criteria.where("projectName").regex(regex, "i"),
                    Criteria.where("motorIds").regex(regex, "i")
            ));
        }

        return mongoTemplate.count(query, Batch.class);
    }
}