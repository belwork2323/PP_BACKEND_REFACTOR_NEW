package in.co.bel.acempp.repository.sourcing;

import in.co.bel.acempp.entity.sourcing.Sourcing;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourcingRepository extends MongoRepository<Sourcing, String> {
    Optional<Sourcing> findBySourcingId(String srcId);
    long count();
}