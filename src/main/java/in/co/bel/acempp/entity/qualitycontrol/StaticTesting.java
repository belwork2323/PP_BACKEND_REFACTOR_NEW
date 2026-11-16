package in.co.bel.acempp.entity.qualitycontrol;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.statictestingfacility.StaticMotorData;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "static_testings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StaticTesting {

    @Id
    private String id;

    @Indexed(unique = true)
    private String formId;                     // STF-2026-0001

    @Indexed(unique = true)                   // one form per batch
    private String batchId;

    private Long subDepartmentId;
    private Status formSubmissionType;         // DRAFT / SUBMIT
    private Status status;                     // DRAFT / WAITING_FOR_APPROVAL
    private List<StaticMotorData> motors;

    private PersonInfo createdBy;
    private LocalDateTime createdAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private String remarks;
    private String rejectionReason;

    private boolean isDeleted = false;
}
