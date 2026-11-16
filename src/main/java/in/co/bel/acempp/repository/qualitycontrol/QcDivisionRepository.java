package in.co.bel.acempp.repository.qualitycontrol;

import in.co.bel.acempp.entity.qualitycontrol.QcDivision;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QcDivisionRepository extends MongoRepository<QcDivision, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    long countByIsDeletedFalse();
    Optional<QcDivision> findByFormIdAndIsDeletedFalse(String formId);
    Optional<QcDivision> findByBatchIdAndIsDeletedFalse(String batchId);
}