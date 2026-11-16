package in.co.bel.acempp.dto.user.operations;

import lombok.*;

@Data
@Builder
public class MotorDetailDto {
    private String motorCasingId;
    private String motorId;
    private String motorCasingStatus;   // from RocketMotorCasing
    private String castingStatus;       // default NOT_STARTED for now
    private String curingStatus;        // default NOT_STARTED for now
}