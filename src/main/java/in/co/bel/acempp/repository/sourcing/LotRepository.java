package in.co.bel.acempp.repository.sourcing;

import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.enums.Status;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LotRepository extends MongoRepository<Lot, String> {
    Optional<Lot> findByLotId(String lotId);
    boolean existsByLotId(String lotId);
    List<Lot> findBySourcingId(String srcId);
    // Status counts for a sub‑department (MongoDB auto‑implements these)
    long countBySubDepartmentIdAndStatusNot(Long subDepartmentId, Status status);
    long countBySubDepartmentIdAndStatus(Long subDepartmentId, Status status);
    long countBySubDepartmentIdAndStatusIn(Long subDepartmentId, List<Status> statuses);
    Optional<Lot> findByLotIdAndSubDepartmentId(String lotId, Long subDepartmentId
    );
}