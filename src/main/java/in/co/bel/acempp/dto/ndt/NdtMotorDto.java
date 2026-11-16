package in.co.bel.acempp.dto.ndt;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.ndt.motor.*;
import in.co.bel.acempp.dto.qcdivision.QcCertificateListDeserializer;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NdtMotorDto {

    private String motorId;

    private Status motorSubmissionType;

    private Status motorSubmissionStatus;

    private RadiographyDetailsDto radiographyDetails;

    private List<AdditionalExposureDetailsDto> additionalExposureDetails;

    private List<RadiographyObservationDto> radiographyObservations;

    private List<VisualInspectionDetailsDto> visualInspectionDetails;

    @JsonDeserialize(using = QcCertificateListDeserializer.class)
    private List<DocumentDto> uploadedVideos;

    private SignedNdtReportDto signedNdtReport;
}
