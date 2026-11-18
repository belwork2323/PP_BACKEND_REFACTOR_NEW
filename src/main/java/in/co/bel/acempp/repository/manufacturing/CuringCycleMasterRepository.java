package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.CuringCycleMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuringCycleMasterRepository
        extends MongoRepository<CuringCycleMaster, Long> {

    List<CuringCycleMaster> findAllByProjectIdAndMotorStage(String projectId, Integer motorStage);

    Optional<CuringCycleMaster> findByCuringCycleCode(String curingCycleCode);

    Optional<CuringCycleMaster> findFirstByCuringTypeAndIsActiveTrue(String curingType);

    Optional<CuringCycleMaster> findByProjectIdAndMotorStageAndIsActiveTrue(String projectId, Integer motorStage);
}
