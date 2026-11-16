package in.co.bel.acempp.dto.masterdata.materials;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.MaterialType;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.RmpFormTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialMasterRecordDto {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private MaterialType materialType;
    private RawMaterialCategory rawMaterialType;
    private PreparationType preparationType;
    private RmpFormTemplate rmpFormTemplate;
    private Boolean isActive;
    @Builder.Default
    private List<MaterialGradeDto> grades = new ArrayList<>();
    @Builder.Default
    private List<MaterialSpecDto> specifications = new ArrayList<>();

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
