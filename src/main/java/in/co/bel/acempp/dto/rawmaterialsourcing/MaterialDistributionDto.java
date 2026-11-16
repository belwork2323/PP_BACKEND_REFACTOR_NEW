package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaterialDistributionDto {
    private String materialCode;
    private long lotCount;
    private double percentage;
}