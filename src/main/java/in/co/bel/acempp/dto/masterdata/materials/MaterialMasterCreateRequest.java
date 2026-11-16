package in.co.bel.acempp.dto.masterdata.materials;

import in.co.bel.acempp.enums.MaterialType;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.RmpFormTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialMasterCreateRequest {
    @NotBlank
    @Size(max = 64)
    private String materialCode;

    @NotBlank
    @Size(max = 255)
    private String materialName;

    @NotNull
    private MaterialType materialType;

    private RawMaterialCategory rawMaterialType;

    private PreparationType preparationType;

    /** Defaults to DEFAULT when omitted. */
    private RmpFormTemplate rmpFormTemplate;

    private Boolean isActive;

    @Valid
    @Builder.Default
    private List<MaterialGradeWriteDto> grades = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<MaterialSpecWriteDto> specifications = new ArrayList<>();
}
