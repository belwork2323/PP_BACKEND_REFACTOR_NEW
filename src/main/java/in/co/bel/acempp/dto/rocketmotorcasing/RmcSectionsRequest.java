package in.co.bel.acempp.dto.rocketmotorcasing;

import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RmcSectionsRequest {

    @Valid
    private MotorReceipt motorReceipt;

    private List<@Valid VisualInspection> visualInspection;

    @Valid
    private Weightment weightment;

    private List<@Valid DimensionalInspection> dimensionalInspection;

    @Valid
    private MockTrial mockTrial;

    @Valid
    private ReportUploadDto reportUpload;

    @Valid
    private RadiographyDetails radiographyDetails;
}
