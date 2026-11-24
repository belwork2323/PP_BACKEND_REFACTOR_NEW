package in.co.bel.acempp.dto.postcureoperations;

import com.fasterxml.jackson.annotation.JsonAlias;
import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads.InhibitionDetails;
import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads.LooseFlapFillingDetails;
import in.co.bel.acempp.enums.PostCureInhibitorType;
import in.co.bel.acempp.enums.PostCureOperationType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCureMotor {
    @NotBlank(message = "motorId is required", groups = {DraftGroup.class, SubmitGroup.class})
    @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "motorId cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.BUSINESS_TEXT_PATTERN, message = "motorId format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
    private String motorId;

    @NotNull(message = "motorReceiptDateTime is required", groups = {DraftGroup.class, SubmitGroup.class})
    @JsonAlias("motorReceiptDate")
    @Field("motorReceiptDate")
    private LocalDateTime motorReceiptDateTime;

    private PostCureOperationType operationType;

    @NotNull(message = "inhibitorType is required", groups = SubmitGroup.class)
    private PostCureInhibitorType inhibitorType;

    @NotNull(message = "motorSubmissionType is required", groups = {DraftGroup.class, SubmitGroup.class})
    private Status motorSubmissionType;

    private Status motorSubmissionStatus;

    private String rejectionReason;

    @Valid
    private LooseFlapFillingDetails looseFlapFillingDetails;

    @Valid
    private InhibitionDetails inhibitionDetails;
}
