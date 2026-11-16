package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MechanicalPropertiesTableDTO {
    private String articleType;
    private String bemNo;
    private Integer ts;
    private Integer elongation;
    private Integer modulus;
    private Integer sbs;
    private Integer tbs;
    private Integer peelStrength;
    private Double density;
    private String actor;
}
