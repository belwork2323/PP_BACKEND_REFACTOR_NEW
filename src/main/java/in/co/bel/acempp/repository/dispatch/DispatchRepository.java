package in.co.bel.acempp.repository.dispatch;

import in.co.bel.acempp.entity.dispatch.Dispatch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DispatchRepository extends MongoRepository<Dispatch, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    Optional<Dispatch> findByFormIdAndIsDeletedFalse(String formId);
    long countByIsDeletedFalse();
    Optional<Dispatch> findByBatchIdAndIsDeletedFalse(String batchId);
}