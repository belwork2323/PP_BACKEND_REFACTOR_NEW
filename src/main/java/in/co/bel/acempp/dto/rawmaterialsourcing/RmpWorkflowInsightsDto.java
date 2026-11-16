package in.co.bel.acempp.dto.rawmaterialsourcing;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RmpWorkflowInsightsDto {
    private String  currentStatus;
    private String  rejectionReason;
    private boolean approvalPending;
    private boolean reworkRequired;
    private int     resubmissionCount;
}