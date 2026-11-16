package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.DepartmentMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends MongoRepository<DepartmentMaster, String> {

    List<DepartmentMaster> findAll();
    List<DepartmentMaster> findAllByOrderByDepartmentIdAsc();
    Optional<DepartmentMaster> findBySubDepartmentsSubDepartmentId(Long subDepartmentId);
}
