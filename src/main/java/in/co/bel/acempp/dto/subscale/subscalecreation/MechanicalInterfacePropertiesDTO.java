package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MechanicalInterfacePropertiesDTO {
    private List<MechanicalPropertiesTableDTO> mechanicalPropertiesTable;
}
