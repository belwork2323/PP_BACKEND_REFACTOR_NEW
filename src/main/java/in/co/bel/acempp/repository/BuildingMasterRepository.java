package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.batch.BuildingMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildingMasterRepository extends MongoRepository<BuildingMaster, String> {
    Optional<BuildingMaster> findByCode(String code);

    Optional<BuildingMaster> findFirstById(Long id);
    Optional<BuildingMaster> findByCodeAndIsActiveTrue(String bldngno);
    Optional<BuildingMaster> findByNameAndIsActiveTrue(String bldngno);

}
