package in.co.bel.acempp.dto.admin.motor;

import in.co.bel.acempp.dto.general.PaginationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorListResponse {
    private List<MotorSummaryDto> motors;
    private PaginationDto pagination;
}
