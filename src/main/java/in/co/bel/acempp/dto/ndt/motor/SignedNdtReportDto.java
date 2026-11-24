package in.co.bel.acempp.dto.ndt.motor;

import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.constraints.NotNull;
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
public class SignedNdtReportDto {
    @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
    @Pattern(
            regexp = "^$|" + ValidationConstants.REMARKS_PATTERN,
            message = "Additional remarks contain invalid characters",
            groups = SubmitGroup.class
    )
    private String additionalRemarks;

    @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
    @Pattern(
            regexp = "^$|" + ValidationConstants.REMARKS_PATTERN,
            message = "QA remarks contain invalid characters",
            groups = SubmitGroup.class
    )
    private String qaRemarks;

    @NotNull(message = "Signed report document is required", groups = { SubmitGroup.class})
    private DocumentDto report;
}
