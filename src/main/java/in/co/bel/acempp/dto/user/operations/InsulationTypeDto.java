package in.co.bel.acempp.dto.user.operations;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InsulationTypeDto {
    private Long insulationSpecId;
    private String insulationType;
}
