package in.co.bel.acempp.dto.general;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusCountsDto {
    private Long initiated;
    private Long approved;
    private Long inProgress;
    private Long waitingForApproval;
    private Long waitingForPartialApproval;
    private Long waitingForCompleteApproval;
    private Long finalApprovalCompleted;
    private Long rejected;
}