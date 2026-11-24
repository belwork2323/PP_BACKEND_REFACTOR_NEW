package in.co.bel.acempp.dto.ndt;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.ndt.motor.*;
import in.co.bel.acempp.dto.qcdivision.QcCertificateListDeserializer;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Motor ID is required", groups = SubmitGroup.class)
    private String motorId;

    @NotNull(message = "Motor submission type is required", groups = SubmitGroup.class)
    private Status motorSubmissionType;

    private Status motorSubmissionStatus;

    private String rejectionReason;

    @Valid
    @NotNull(message = "Radiography details are required", groups = SubmitGroup.class)
    private RadiographyDetailsDto radiographyDetails;

    @Valid
    private List<AdditionalExposureDetailsDto> additionalExposureDetails;

    @Valid
    private List<RadiographyObservationDto> radiographyObservations;

    @Valid
    @NotEmpty(message = "At least one visual inspection detail is required", groups = SubmitGroup.class)
    private List<VisualInspectionDetailsDto> visualInspectionDetails;

    @JsonDeserialize(using = QcCertificateListDeserializer.class)
    private List<DocumentDto> uploadedVideos;

    @Valid
    @NotNull(message = "Signed NDT report is required", groups = SubmitGroup.class)
    private SignedNdtReportDto signedNdtReport;
}
