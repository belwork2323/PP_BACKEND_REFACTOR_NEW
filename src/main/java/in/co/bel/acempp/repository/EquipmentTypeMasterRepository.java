package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.batch.EquipmentTypeMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentTypeMasterRepository extends MongoRepository<EquipmentTypeMaster, String> {
    Optional<EquipmentTypeMaster> findByCode(String code);

    Optional<EquipmentTypeMaster> findByTypeCode(String typeCode);

    Optional<EquipmentTypeMaster> findFirstById(Long id);
}
