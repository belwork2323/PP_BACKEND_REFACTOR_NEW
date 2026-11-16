package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.RoleMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<RoleMaster, String> {

    Optional<RoleMaster> findByRoleId(Long roleId);
    List<RoleMaster> findAllByOrderBySortAsc();
}