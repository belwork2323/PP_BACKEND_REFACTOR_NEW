package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MaterialDetailDto {
    private String               materialCode;
    private String               lotNo;
    private List<SpecDetailDto> specifications;
}