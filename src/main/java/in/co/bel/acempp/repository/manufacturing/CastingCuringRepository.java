package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.CastingCuring;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface CastingCuringRepository extends MongoRepository<CastingCuring, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    Optional<CastingCuring> findByFormIdAndIsDeletedFalse(String formId);
    long countByIsDeletedFalse();
    Optional<CastingCuring> findByBatchIdAndIsDeletedFalse(String batchId);

}