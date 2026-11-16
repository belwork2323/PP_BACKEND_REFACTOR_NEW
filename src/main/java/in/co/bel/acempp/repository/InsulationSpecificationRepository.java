package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.InsulationSpecification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface InsulationSpecificationRepository
        extends MongoRepository<InsulationSpecification, String> {

    Optional<InsulationSpecification> findByInsulationType(String insulationType);

    Optional<InsulationSpecification> findByInsulationTypeIgnoreCase(String insulationType);

    Optional<InsulationSpecification> findFirstByInsulationSpecId(Long insulationSpecId);

    Optional<InsulationSpecification> findByCode(String code);
}