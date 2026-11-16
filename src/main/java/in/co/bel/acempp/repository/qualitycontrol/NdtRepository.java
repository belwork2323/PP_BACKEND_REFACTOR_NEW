package in.co.bel.acempp.repository.qualitycontrol;

import in.co.bel.acempp.entity.qualitycontrol.Ndt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NdtRepository extends MongoRepository<Ndt, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    long countByIsDeletedFalse();
    Optional<Ndt> findByFormIdAndIsDeletedFalse(String formId);
    Optional<Ndt> findByBatchIdAndIsDeletedFalse(String batchId);
}