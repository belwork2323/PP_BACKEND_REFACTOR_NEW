package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.PostCureOperations;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostCureRepository extends MongoRepository<PostCureOperations, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    Optional<PostCureOperations> findByFormIdAndIsDeletedFalse(String formId);
    long countByIsDeletedFalse();
    Optional<PostCureOperations> findByBatchIdAndIsDeletedFalse(String batchId);
}