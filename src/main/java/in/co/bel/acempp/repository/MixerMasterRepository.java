package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.batch.MixerMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MixerMasterRepository extends MongoRepository<MixerMaster, String> {
    Optional<MixerMaster> findByCode(String code);

    Optional<MixerMaster> findFirstById(Long id);
    Optional<MixerMaster> findByCodeAndIsActiveTrue(String id);
    Optional<MixerMaster> findByNameAndIsActiveTrue(String id);


}
