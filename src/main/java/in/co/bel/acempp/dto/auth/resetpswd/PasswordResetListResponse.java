package in.co.bel.acempp.dto.auth.resetpswd;

import in.co.bel.acempp.dto.general.PaginationDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PasswordResetListResponse {
    private ResetSummaryDto summary;
    private List<PasswordResetRequestDto> requests;
    private PaginationDto pagination;
}