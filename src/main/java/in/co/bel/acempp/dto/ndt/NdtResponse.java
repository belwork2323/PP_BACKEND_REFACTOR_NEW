package in.co.bel.acempp.dto.ndt;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.common.BatchFormRequest;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NdtResponse extends BatchFormRequest {

        private String batchType;
        private Status formStatus;
        private PersonInfo createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime createdOn;

        private PersonInfo submittedBy; // supports String or PersonInfo depending on endpoint
        private LocalDateTime submittedAt;
        private LocalDateTime submittedOn;

        // Update metadata
        private PersonInfo updatedBy;
        private LocalDateTime updatedAt;
        private LocalDateTime updatedOn;

        // Motor details & counts
        private List<NdtMotorDto> motors;
        private Boolean allMotorsApproved;
        private int motorCount;
        private int pendingMotorCount;
        private int approvedMotorCount;
        private int rejectedMotorCount;
        private int inProgressMotorCount;
        private int totalMotorCount;
        private List<MotorStatusResponse> motorStatuses;

}
