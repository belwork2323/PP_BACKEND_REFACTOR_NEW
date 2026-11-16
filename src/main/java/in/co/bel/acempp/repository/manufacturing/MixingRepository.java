package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.Mixing;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MixingRepository extends MongoRepository<Mixing, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    long countByIsDeletedFalse();
    Optional<Mixing> findByFormIdAndIsDeletedFalse(String formId);
    Optional<Mixing> findByBatchIdAndIsDeletedFalse(String batchId);
}