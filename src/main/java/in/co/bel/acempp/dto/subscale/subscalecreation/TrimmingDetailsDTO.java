package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class TrimmingDetailsDTO {
    @Valid
    private List<TrimmingTableDTO> trimmingTable;
}
