package in.co.bel.acempp.dto.subscale.subscalecreation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class StaticTestingDetailsDTO {
    @Valid
    private List<StaticTestingTableDTO> staticTestingTable;
}
