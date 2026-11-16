package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.postcureoperations.PostCureMotor;
import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads.InhibitionDetails;
import in.co.bel.acempp.enums.PostCureInhibitorType;
import org.springframework.stereotype.Component;

@Component
public class PostCureDtoMapper {

    public PostCureMotor toPersistence(PostCureMotor request) {
        if (request == null) return null;

        PostCureInhibitorType inhibitorType = request.getInhibitorType();
        return PostCureMotor.builder()
                .motorId(request.getMotorId())
                .motorReceiptDateTime(request.getMotorReceiptDateTime())
                .operationType(request.getOperationType())
                .inhibitorType(inhibitorType)
                .motorSubmissionType(request.getMotorSubmissionType())
                .looseFlapFillingDetails(request.getLooseFlapFillingDetails())
                .inhibitionDetails(toPersistenceInhibitionDetails(inhibitorType, request.getInhibitionDetails()))
                .build();
    }

    /**
     * For NOT_APPLICABLE, persist remarks-only details (strip mix sections).
     * For other types, persist as provided.
     */
    private static InhibitionDetails toPersistenceInhibitionDetails(
            PostCureInhibitorType inhibitorType,
            InhibitionDetails details) {
        if (inhibitorType != PostCureInhibitorType.NOT_APPLICABLE) {
            return details;
        }
        if (details == null) {
            return null;
        }
        String remarks = details.getNotApplicableRemarks();
        if (remarks != null && remarks.isBlank()) {
            remarks = null;
        }
        return InhibitionDetails.builder()
                .inhibitorType(PostCureInhibitorType.NOT_APPLICABLE)
                .notApplicableRemarks(remarks)
                .build();
    }
}
