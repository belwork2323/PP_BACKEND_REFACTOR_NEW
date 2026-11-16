package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialMetadata {

    private Long materialId;

    private String materialCode;

    private String materialName;

    private Long gradeId;

    private String gradeCode;

    private String materialType;
}