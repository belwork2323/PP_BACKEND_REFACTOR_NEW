package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.castingcuring.CastingCuringMotor;
import org.springframework.stereotype.Component;

@Component
public class CastingCuringDtoMapper {

    public CastingCuringMotor toPersistence(CastingCuringMotor request) {
        if (request == null) return null;
        return CastingCuringMotor.builder()
                .motorId(request.getMotorId())
                .motorReceivedAt(request.getMotorReceivedAt())
                .motorSubmissionType(request.getMotorSubmissionType())
                .setup(request.getSetup())
                .curingSetup(request.getCuringSetup())
                .castingSections(request.getCastingSections())
                .curingSections(request.getCuringSections())
                .build();
    }
}
