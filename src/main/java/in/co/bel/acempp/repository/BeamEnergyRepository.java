package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.BeamEnergy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeamEnergyRepository extends MongoRepository<BeamEnergy, String> {
    Optional<BeamEnergy> findByBeamEnergyCode(String beamEnergyCode);

    Optional<BeamEnergy> findByBeamEnergyId(Long beamEnergyId);
}
