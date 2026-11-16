package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.enums.MaterialType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface MaterialRepository extends MongoRepository<MaterialMaster, String> {
    Optional<MaterialMaster> findFirstByMaterialCode(String materialCode);
    List<MaterialMaster> findByMaterialType(MaterialType materialType);
    Optional<MaterialMaster> findByMaterialIdAndMaterialCode(
            Long materialId,
            String materialCode
    );
    List<MaterialMaster> findByMaterialCodeIn(List<String> materialCodes);
    List<MaterialMaster> findByMaterialNameIn(List<String> materialNames);
    Optional<MaterialMaster> findFirstByMaterialId(Long materialId);
}