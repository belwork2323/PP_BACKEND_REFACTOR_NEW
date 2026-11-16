package in.co.bel.acempp.dto.trimming.trimmingcreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MotorDTO {

    @NotBlank(message = "Motor ID is required", groups = {SubmitGroup.class})
    @Pattern(
            regexp = "^$|^[A-Za-z0-9\\/_\\-\\s]+$",
            message = "Motor ID can only contain letters, numbers, spaces, hyphens, underscores, and slashes",
            groups = {DraftGroup.class, SubmitGroup.class}
    )
    private String motorId;

    @NotNull(message = "Motor stage is required", groups = {SubmitGroup.class})
    @PositiveOrZero(message = "Motor Stage must be zero or positive", groups = {DraftGroup.class, SubmitGroup.class})
    private Integer motorStage;

    @JsonAlias("motorReceivedAt")
    @Field("motorReceivedAt")
    private LocalDateTime motorReceivedDateTime;

    @Valid
    private List<SectionDTO> sections;

    @NotBlank(message = "Motor Submission Type is required", groups = {SubmitGroup.class})
    @Pattern(
            regexp = "^$|^[A-Za-z0-9\\/_\\-\\s]+$",
            message = "Motor Submission Type contains invalid characters",
            groups = {DraftGroup.class, SubmitGroup.class}
    )
    private String motorSubmissionType;

    private Status motorSubmissionStatus;
}
