package in.co.bel.acempp.dto.statictestingfacility.bem;

import in.co.bel.acempp.enums.Status;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverBemStatusRequest {

    private String motorId;

    private Status actionType;

    private String remarks;

    private String rejectionReason;
}