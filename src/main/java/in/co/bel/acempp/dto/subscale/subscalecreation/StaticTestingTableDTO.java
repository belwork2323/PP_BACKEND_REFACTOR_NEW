package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticTestingTableDTO {
    private String articleType;
    private String bemNo;
    private Double propellantMass;
    private Double dt;
    private Double webThickness;
    private Double nValue;
    private Double pressureAvg;
    private Double thrustAvg;
    private Double burnRate;
    private DocumentDto graph;
}
