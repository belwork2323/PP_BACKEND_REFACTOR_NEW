package in.co.bel.acempp.dto.user.operations;

import lombok.*;

import java.util.List;

@Data
@Builder
public class MaterialLotResponse {
    private String batchId;
    private List<MaterialLotDto> materials;
}