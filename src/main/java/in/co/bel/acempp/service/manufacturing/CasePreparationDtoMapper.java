package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.casepreparation.CasePreparationDetailsRequest;
import in.co.bel.acempp.dto.casepreparation.CasePreparationMotorRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CasePreparationDtoMapper {

    public CasePreparationDetailsRequest toPersistence(CasePreparationDetailsRequest request) {
        if (request == null) return null;
        List<CasePreparationMotorRequest> motors = request.getMotors() == null
                ? null
                : request.getMotors().stream().map(this::toPersistence).toList();
        return CasePreparationDetailsRequest.builder().motors(motors).build();
    }

    public CasePreparationMotorRequest toPersistence(CasePreparationMotorRequest request) {
        if (request == null) return null;
        return CasePreparationMotorRequest.builder()
                .motorId(request.getMotorId())
                .prrcClearanceDate(request.getPrrcClearanceDate())
                .motorSubmissionType(request.getMotorSubmissionType())
                .abradingOperation(request.getAbradingOperation())
                .bellowBonding(request.getBellowBonding())
                .tceCleaning(request.getTceCleaning())
                .preHeating(request.getPreHeating())
                .linerCoatingOperation(request.getLinerCoatingOperation())
                .dispatchToCasting(request.getDispatchToCasting())
                .build();
    }
}
