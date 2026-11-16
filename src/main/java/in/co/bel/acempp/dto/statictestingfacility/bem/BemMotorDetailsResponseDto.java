package in.co.bel.acempp.dto.statictestingfacility.bem;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.BemStaticTestingDetails;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BemMotorDetailsResponseDto {
    private String formId;
    private String motorId;
    private String stfTestNo;
    private String bemNo;
    private StfMotorSubType subType;
    private Status status;
    private BemStaticTestingDetails staticTestingDetails;
    private PersonInfo createdBy;
    private LocalDateTime createdAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
}
