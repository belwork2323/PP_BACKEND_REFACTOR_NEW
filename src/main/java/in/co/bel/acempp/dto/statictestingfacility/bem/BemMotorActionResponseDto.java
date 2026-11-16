package in.co.bel.acempp.dto.statictestingfacility.bem;


import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BemMotorActionResponseDto {
    private String id;
    private String motorId;
    private String stfTestNo;
    private StfMotorSubType subType;
    private Status status;
    private Status formSubmissionType;
    private PersonInfo actionBy;
    private LocalDateTime actionAt;
}
