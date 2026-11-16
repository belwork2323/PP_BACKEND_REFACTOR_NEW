package in.co.bel.acempp.dto.user.operations;

import lombok.*;

import java.util.List;

@Data
@Builder
public class MaterialDto {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String rawMaterialType;
    private String preparationType;
    /** RMP process UI template (DEFAULT | AP | ALUMINUM | DOA | HTPB | TDI | CC | IO | NONOX_D). */
    private String rmpFormTemplate;
    private List<MaterialGrade> grades;
    private Long specCount;
}
