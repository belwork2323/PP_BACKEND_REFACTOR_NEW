package in.co.bel.acempp.dto.statictestingfacility.bem;

import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.general.StatusCountsDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BemMotorListResponseDto {
    private List<BemMotorListItemDto> motors;
    private PaginationDto pagination;
    private StatusCountsDto statusCounts;
}