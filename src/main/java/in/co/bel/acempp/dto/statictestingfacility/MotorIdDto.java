package in.co.bel.acempp.dto.statictestingfacility;

import in.co.bel.acempp.enums.StfMotorSubType;
import lombok.*;

@Data
@Builder
public class MotorIdDto {
    private String motorId;
    private StfMotorSubType subType;
}
