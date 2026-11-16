package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManufacturerInsightsDto {
    private long activeManufacturers;
    private TopManufacturerDto topManufacturer;
    private String highestYieldManufacturer;
}