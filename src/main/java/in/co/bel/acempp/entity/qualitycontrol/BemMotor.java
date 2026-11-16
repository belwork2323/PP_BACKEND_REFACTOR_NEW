package in.co.bel.acempp.entity.qualitycontrol;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.BemStaticTestingDetails;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bem_motors")
public class BemMotor {

    @Id
    private String id;
    @Indexed(unique = true)
    private String motorId;
    @Indexed(unique = true)
    private String stfTestNo;
    private Long subDepartmentId;
    private String formId;
    private StfMotorSubType subType;
    private Status status;
    private Status formSubmissionType;
    private BemStaticTestingDetails staticTestingDetails;
    private Boolean isDeleted;
    private PersonInfo createdBy;
    private LocalDateTime createdAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private String remarks;
    private String rejectionReason;
}
