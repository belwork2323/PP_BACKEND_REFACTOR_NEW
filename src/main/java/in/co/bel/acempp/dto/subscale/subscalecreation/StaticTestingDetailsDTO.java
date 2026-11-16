package in.co.bel.acempp.dto.subscale.subscalecreation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticTestingDetailsDTO {
    private List<StaticTestingTableDTO> staticTestingTable;
}
