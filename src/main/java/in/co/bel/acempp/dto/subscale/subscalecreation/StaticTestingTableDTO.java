package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class StaticTestingTableDTO {
    private String articleType;
    private String bemNo;
    @PositiveOrZero(groups = {DraftGroup.class, SubmitGroup.class})
    private Double propellantMass;
    private Double dt;
    private Double webThickness;
    private Double nValue;
    private Double pressureAvg;
    private Double thrustAvg;
    private Double burnRate;
    private DocumentDto graph;
}
