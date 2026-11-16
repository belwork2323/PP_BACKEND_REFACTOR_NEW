package in.co.bel.acempp.repository.sourcing;

import in.co.bel.acempp.entity.sourcing.RocketMotorCasing;
import in.co.bel.acempp.enums.Status;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RocketMotorCasingRepository extends MongoRepository<RocketMotorCasing, String> {
    boolean existsByMotorCasingId(String motorCasingId);
    Optional<RocketMotorCasing> findByMotorCasingId(String motorCasingId);
    List<RocketMotorCasing> findByProjectIdAndMotorStageAndStatusAndActiveTrue(String projectId, Integer motorStage,
            Status status
    );
    boolean existsByProjectIdAndActiveTrue(String projectId);
    Optional<RocketMotorCasing> findByMotorCasingIdAndSubDepartmentId(String motorCasingId, Long subDepartmentId
    );
    long countBySubDepartmentIdAndActiveTrue(Long subDepartmentId);
    long countBySubDepartmentIdAndStatus(Long subDepartmentId, Status status);
    long countBySubDepartmentIdAndStatusIn(Long subDepartmentId, List<Status> statuses);
    List<RocketMotorCasing> findByMotorIdAndProjectIdAndMotorStageAndActiveTrue(
            String motorId, String projectId, Integer motorStage);

    List<RocketMotorCasing> findByMotorIdInAndActiveTrue(List<String> motorIds);
}
