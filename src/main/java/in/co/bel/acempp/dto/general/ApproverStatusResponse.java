package in.co.bel.acempp.dto.general;

import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverStatusResponse {

    private String formId;

    private Status status;

    private String remarks;

    private String rejectionReason;

    private PersonInfo actionBy;

    private LocalDateTime actionAt;
}
