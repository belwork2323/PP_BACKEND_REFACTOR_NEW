package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.QualityCheckMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QualityCheckRepository
        extends MongoRepository<QualityCheckMaster, Long> {
    Optional<QualityCheckMaster> findByMixTypeAndMotorStage(String mixType, Integer stage);

    Optional<QualityCheckMaster> findByQualityCheckCode(String qualityCheckCode);

    @Query("{ 'qualityChecks.parameterId': { $in: ?0 } }")
    List<QualityCheckMaster> findByQualityCheckParameterIds(Collection<String> parameterIds);
}
