package in.co.bel.acempp.dto.rocketmotorcasing;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportUploadDto {

    private List<DocumentDto> ndtUtReport;

    private List<DocumentDto> visualInspectionReport;

    private List<DocumentDto> weighmentReport;

    private List<DocumentDto> dimensionalInspectionReport;

    private List<DocumentDto> mockTrialReport;

    private List<DocumentDto> insulationLiningReport;
}