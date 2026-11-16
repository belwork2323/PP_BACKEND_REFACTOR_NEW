package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.batch.UnitMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitMasterRepository extends MongoRepository<UnitMaster, String> {
    Optional<UnitMaster> findByCode(String code);

    Optional<UnitMaster> findByUnitCode(String unitCode);

    Optional<UnitMaster> findFirstById(Long id);

}
