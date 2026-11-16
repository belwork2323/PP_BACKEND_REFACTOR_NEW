package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HardwarePreparationTableDTO {
    private String articleType;
    private String rubberMaterial;
    private String sleeveNo;
    private String mouldNo;
    private String lengthMm;
    private String thicknessMm;
    private String linerApplied;
    private String observations;
}
