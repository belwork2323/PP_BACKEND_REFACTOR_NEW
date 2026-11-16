package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.CastingStation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CastingStationRepository extends MongoRepository<CastingStation, String> {
    List<CastingStation> findByStatusOrderByStationIdAsc(String status);

    List<CastingStation> findAllByOrderByStationIdAsc();

    Optional<CastingStation> findByStationCode(String stationCode);

    Optional<CastingStation> findByStationId(Long stationId);
    Optional<CastingStation> findByStationCodeAndStatus(String stationCode, String status);
    Optional<CastingStation> findByStationNameAndStatus(String stationCode , String status);

}
