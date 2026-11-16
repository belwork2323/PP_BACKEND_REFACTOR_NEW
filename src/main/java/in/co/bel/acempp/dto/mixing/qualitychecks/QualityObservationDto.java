package in.co.bel.acempp.dto.mixing.qualitychecks;

import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QualityObservationDto {
    @NotNull(message = "Sample number is required", groups = SubmitGroup.class)
    @Min(value = 1, message = "Sample number must be at least 1", groups = DraftGroup.class)
    @Max(value = 10, message = "Sample number cannot exceed 10", groups = DraftGroup.class)
    private Integer sampleNo;

    /**
     * Observed value format / required checks are enforced in the UI.
     * Draft saves may send blank values; submit is gated client-side.
     */
    private String value;
}
