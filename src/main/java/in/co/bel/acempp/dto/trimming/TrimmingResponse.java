package in.co.bel.acempp.dto.trimming;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrimmingResponse {
    private String formId;
    private String batchId;
    private Status status;
    private Integer motorCount;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private String submittedBy;
    private Integer pendingMotorCount;
    private Integer approvedMotorCount;
    private Integer rejectedMotorCount;
    private Integer inProgressMotorCount;
    private Integer totalMotorCount;
    private List<MotorStatusResponse> motorStatuses;
}