package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.CasePreparation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CasePreparationRepository extends MongoRepository<CasePreparation, String> {

    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    long countByIsDeletedFalse();
    Optional<CasePreparation> findByFormIdAndIsDeletedFalse(String formId);
    Optional<CasePreparation> findByBatchIdAndIsDeletedFalse(String batchId);
}