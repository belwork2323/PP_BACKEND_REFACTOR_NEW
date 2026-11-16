package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopManufacturerDto {
    private String manufacturerName;
    private long lotCount;
}