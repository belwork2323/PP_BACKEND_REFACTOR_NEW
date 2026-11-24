package in.co.bel.acempp.dto.ndt.motor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.qcdivision.QcCertificateListDeserializer;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiographyObservationDto {

    @NotNull(message = "Section number is required", groups = SubmitGroup.class)
    private Integer sectionNumber;

    @NotBlank(message = "Orientation is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
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
