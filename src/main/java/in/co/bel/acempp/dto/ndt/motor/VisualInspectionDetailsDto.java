package in.co.bel.acempp.dto.ndt.motor;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.qcdivision.QcCertificateListDeserializer;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualInspectionDetailsDto {
    @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
    @Pattern(
            regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
            message = "Observation type contains invalid characters",
            groups = SubmitGroup.class
    )
    private String observationType;

    private Integer sectionNumber;

    @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
    @Pattern(
            regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
            message = "Orientation contains invalid characters",
            groups = SubmitGroup.class
    )
    private String orientation;

    @NotBlank(message = "Observation is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.MAX_LONG_TEXT, groups = SubmitGroup.class)
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
            message = "Observation contains invalid characters",
            groups = SubmitGroup.class
    )
    private String observation;

    @JsonDeserialize(using = QcCertificateListDeserializer.class)
    private List<DocumentDto> uploadedImages;
}
