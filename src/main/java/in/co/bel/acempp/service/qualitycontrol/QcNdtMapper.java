package in.co.bel.acempp.service.qualitycontrol;

import in.co.bel.acempp.dto.ndt.NdtMotorDto;
import in.co.bel.acempp.dto.ndt.motor.RadiographyObservationDto;
import in.co.bel.acempp.dto.ndt.motor.VisualInspectionDetailsDto;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps manufacturing NDT motors to QC wire shape ({@code location} instead of {@code orientation}).
 */
@Component
public class QcNdtMapper {

    public QcDivisionPayloads.NdtMotorInput toQcMotor(NdtMotorDto source) {
        if (source == null) {
            return null;
        }
        QcDivisionPayloads.NdtMotorInput target = new QcDivisionPayloads.NdtMotorInput();
        target.setMotorId(source.getMotorId());
        target.setMotorSubmissionType(source.getMotorSubmissionType());
        target.setMotorSubmissionStatus(source.getMotorSubmissionStatus());
        target.setRadiographyDetails(source.getRadiographyDetails());
        target.setAdditionalExposureDetails(source.getAdditionalExposureDetails());
        target.setRadiographyObservations(mapRadiographyObservations(source.getRadiographyObservations()));
        target.setVisualInspectionDetails(mapVisualInspection(source.getVisualInspectionDetails()));
        target.setUploadedVideos(source.getUploadedVideos());
        target.setSignedNdtReport(source.getSignedNdtReport());
        return target;
    }

    private List<QcDivisionPayloads.QcNdtRadiographyObservation> mapRadiographyObservations(
            List<RadiographyObservationDto> source) {
        if (source == null) {
            return null;
        }
        List<QcDivisionPayloads.QcNdtRadiographyObservation> mapped = new ArrayList<>();
        for (RadiographyObservationDto row : source) {
            if (row == null) {
                continue;
            }
            QcDivisionPayloads.QcNdtRadiographyObservation target =
                    new QcDivisionPayloads.QcNdtRadiographyObservation();
            target.setSectionNumber(row.getSectionNumber());
            target.setLocation(row.getOrientation());
            target.setObservation(row.getObservation());
            target.setUploadedImages(row.getUploadedImages());
            mapped.add(target);
        }
        return mapped;
    }

    private List<QcDivisionPayloads.QcNdtVisualInspectionDetails> mapVisualInspection(
            List<VisualInspectionDetailsDto> source) {
        if (source == null) {
            return null;
        }
        List<QcDivisionPayloads.QcNdtVisualInspectionDetails> mapped = new ArrayList<>();
        for (VisualInspectionDetailsDto row : source) {
            if (row == null) {
                continue;
            }
            QcDivisionPayloads.QcNdtVisualInspectionDetails target =
                    new QcDivisionPayloads.QcNdtVisualInspectionDetails();
            target.setObservationType(row.getObservationType());
            target.setSectionNumber(row.getSectionNumber());
            target.setLocation(row.getOrientation());
            target.setObservation(row.getObservation());
            target.setUploadedImages(row.getUploadedImages());
            mapped.add(target);
        }
        return mapped;
    }
}
