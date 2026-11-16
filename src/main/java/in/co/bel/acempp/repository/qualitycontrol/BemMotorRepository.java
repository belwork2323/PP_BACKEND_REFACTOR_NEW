package in.co.bel.acempp.repository.qualitycontrol;
import in.co.bel.acempp.entity.qualitycontrol.BemMotor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BemMotorRepository extends MongoRepository<BemMotor, String> {

    Optional<BemMotor> findByMotorIdAndIsDeletedFalse(String motorId);
    Optional<BemMotor> findByFormIdAndIsDeletedFalse(String motorId);

    boolean existsByMotorIdAndIsDeletedFalse(String motorId);

    @Query("{ 'isDeleted': false, " +
            ":#{#search == null ? '1': '1'} : 1, " +
            "$and: [ " +
            "  ?#{ [0] == null ? { _id: { $exists: true } } : { 'motorId': { $regex: [0], $options: 'i' } } }, " +
            "  ?#{ [1] == null ? { _id: { $exists: true } } : { 'status': { $in: [1] } } } " +
            "] }")
    Page<BemMotor> findAllWithFilters(String search, List<String> statuses, Pageable pageable);
    boolean existsByStfTestNoAndIsDeletedFalse(String stfTestNo);
    Optional<BemMotor> findByStfTestNoAndIsDeletedFalse(String stfTestNo);

}