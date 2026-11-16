package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import in.co.bel.acempp.dto.rocketmotorcasing.ReportUploadDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sections {

    private MotorReceipt motorReceipt;
    private List<VisualInspection> visualInspection;
    private Weightment weightment;
    private List<DimensionalInspection> dimensionalInspection;
    private MockTrial mockTrial;
    private ReportUploadDto reportUpload;
    private RadiographyDetails radiographyDetails;
}
