package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.MotorStage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotorStageRepository extends MongoRepository<MotorStage, String> {

    List<MotorStage> findAllByOrderByMotorStageAsc();

    List<MotorStage> findByProjectIdOrderByMotorStageAsc(String projectId);

    Optional<MotorStage> findFirstByStageId(Long stageId);

    Optional<MotorStage> findByMotorStage(Integer motorStage);

    List<MotorStage> findAllByProjectIdAndMotorStage(String projectId, Integer motorStage);
}
