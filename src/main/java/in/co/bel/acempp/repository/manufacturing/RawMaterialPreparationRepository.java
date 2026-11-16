package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.RawMaterialPreparation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RawMaterialPreparationRepository
        extends MongoRepository<RawMaterialPreparation, String> {

    boolean existsByBatchIdAndIsDeletedFalse(String batchId);
    Optional<RawMaterialPreparation> findByBatchIdAndIsDeletedFalse(String batchId);
    long countByIsDeletedFalse();
    Optional<RawMaterialPreparation> findByFormIdAndIsDeletedFalse(String formId);

    @Query("{ 'assetDeliveries.0': { $exists: true } }")
    java.util.List<RawMaterialPreparation> findAllWithAssetDeliveries();

    @Query("{ 'assetDeliveries.response.txId': ?0 }")
    Optional<RawMaterialPreparation> findByAssetTransactionId(String transactionId);
}
