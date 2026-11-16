package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.batch.OvenMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OvenMasterRepository extends MongoRepository<OvenMaster, String> {
    Optional<OvenMaster> findByCode(String code);

    Optional<OvenMaster> findFirstById(Long id);

    Optional<OvenMaster> findByCodeAndIsActiveTrue(String code);

    Optional<OvenMaster> findByNameAndIsActiveTrue(String name);
}
