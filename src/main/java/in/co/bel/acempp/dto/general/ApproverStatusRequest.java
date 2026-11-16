package in.co.bel.acempp.dto.general;

import in.co.bel.acempp.enums.Status;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverStatusRequest {

    private String formId;

    private Long subDepartmentId;

    private Status actionType;

    private String remarks;

    private String rejectionReason;
}
