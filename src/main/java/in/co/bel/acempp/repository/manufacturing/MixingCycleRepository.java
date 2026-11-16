package in.co.bel.acempp.repository.manufacturing;

import in.co.bel.acempp.entity.manufacturing.MixingCycleMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MixingCycleRepository extends MongoRepository<MixingCycleMaster, Long> {

    Optional<MixingCycleMaster> findByMixingCycleCode(String mixingCycleCode);

    List<MixingCycleMaster> findByMixingCycleCodeIn(Collection<String> mixingCycleCodes);

    List<MixingCycleMaster> findAllByProjectIdAndMotorStage(String projectId, Integer motorStage);

    List<MixingCycleMaster> findAllByMotorStage(Integer motorStage);

    Optional<MixingCycleMaster> findByMixingCycleCodeAndIsActiveTrue(String mixingCycleCode);

    Optional<MixingCycleMaster> findByMixingCycleNameAndIsActiveTrue(String mixingCycleCode);

}
