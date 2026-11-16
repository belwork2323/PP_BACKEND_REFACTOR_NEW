package in.co.bel.acempp.entity.manufacturing;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingDetailsDto;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mixing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mixing {
    @Id
    private String id;
    @Indexed(unique = true)
    private String formId;         // MIX-2026-001
    @Indexed(unique = true)
    private String batchId;
    private Long subDepartmentId;
    private String batchType;
    private Status formSubmissionType;
    private Status status;
    private MixingDetailsDto mixingDetails;
    private Boolean active;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private String remarks;
    private String rejectionReason;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo updatedBy;

}
