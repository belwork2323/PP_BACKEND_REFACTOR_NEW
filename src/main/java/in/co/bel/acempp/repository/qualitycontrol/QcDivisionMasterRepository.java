package in.co.bel.acempp.repository.qualitycontrol;

import in.co.bel.acempp.entity.qualitycontrol.QcDivisionMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QcDivisionMasterRepository extends MongoRepository<QcDivisionMaster, String> {

    List<QcDivisionMaster> findAllByOrderByDisplayOrderAsc();

    Optional<QcDivisionMaster> findByDivisionId(Long divisionId);
    Optional<QcDivisionMaster> findByTypesDivisionId(Long divisionId);

    @Query("{ '$or': [ { 'divisionId': ?0 }, { 'types.divisionId': ?0 } ] }")
    Optional<QcDivisionMaster> findByDivisionOrTypeId(Long divisionId);
}
