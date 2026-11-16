package in.co.bel.acempp.entity;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.enums.MaterialType;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.RmpFormTemplate;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialMaster implements MasterDataAuditable {

    @Id
    private String id;                       // MongoDB ObjectId
    private Long materialId;
    private String materialCode;
    private String materialName;
    private MaterialType materialType;
    private RawMaterialCategory rawMaterialType;
    private PreparationType preparationType;
    /** Which RMP process UI to open; null on legacy docs → treat as DEFAULT. */
    private RmpFormTemplate rmpFormTemplate;
    private List<MaterialGrade> grades;
    private List<MaterialSpecification> specifications;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}