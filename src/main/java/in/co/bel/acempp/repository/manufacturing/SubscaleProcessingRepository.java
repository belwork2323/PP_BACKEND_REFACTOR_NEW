package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.SubscaleProcessing;
import in.co.bel.acempp.enums.Status;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscaleProcessingRepository extends MongoRepository<SubscaleProcessing, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    long countByIsDeletedFalse();
    Optional<SubscaleProcessing> findByFormIdAndIsDeletedFalse(String formId);
    Optional<SubscaleProcessing> findByBatchIdAndIsDeletedFalse(String batchId);
    List<SubscaleProcessing> findByBatchIdInAndStatusAndIsDeletedFalse(
            Collection<String> batchIds,
            Status status);
}
