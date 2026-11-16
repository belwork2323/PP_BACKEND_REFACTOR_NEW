package in.co.bel.acempp.repository.qualitycontrol;

import in.co.bel.acempp.entity.qualitycontrol.StaticTesting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface StaticTestingRepository extends MongoRepository<StaticTesting, String> {
    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    Optional<StaticTesting> findByFormIdAndIsDeletedFalse(String formId);   // <-- new
    long countByIsDeletedFalse();
    Optional<StaticTesting> findByBatchIdAndIsDeletedFalse(String batchId);
    List<StaticTesting> findAllByBatchIdAndIsDeletedFalse(String batchId);
    boolean existsByMotorsStfTestNoAndIsDeletedFalse(String stfTestNo);
}