package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.EquipmentMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentRepository extends MongoRepository<EquipmentMaster, String> {
    Optional<EquipmentMaster> findByEquipmentCode(String equipmentCode);

    Optional<EquipmentMaster> findByEquipmentId(Long equipmentId);
}
