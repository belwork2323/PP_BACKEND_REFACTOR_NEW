package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends MongoRepository<AuditLog, String> {
}
