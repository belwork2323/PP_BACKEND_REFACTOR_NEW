package in.co.bel.acempp.dto.rocketmotorcasing;

import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RmcSectionsResponse {
    private MotorReceipt motorReceipt;
    private List<VisualInspection> visualInspection;
    private Weightment weightment;
    private List<DimensionalInspection> dimensionalInspection;
    private MockTrial mockTrial;
    private ReportUploadDto reportUpload;
    private RadiographyDetails radiographyDetails;
}
