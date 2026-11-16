package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.DimensionalParameter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DimensionalParameterRepository extends MongoRepository<DimensionalParameter, String> {

    List<DimensionalParameter> findByMotorType(Integer motorType);

    List<DimensionalParameter> findByProjectIdAndMotorType(String projectId, Integer motorType);

    Optional<DimensionalParameter> findFirstByParameterId(Long parameterId);

    List<DimensionalParameter> findAllByProjectIdAndMotorTypeAndParamId(
            String projectId, Integer motorType, String paramId);
}
