package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.Trimming;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrimmingRepository extends MongoRepository<Trimming, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    Optional<Trimming> findByFormIdAndIsDeletedFalse(String formId);
    long countByIsDeletedFalse();
    Optional<Trimming> findByBatchIdAndIsDeletedFalse(String batchId);
}