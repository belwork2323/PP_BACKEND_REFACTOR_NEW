package in.co.bel.acempp.dto.statictestingfacility.bem;


import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
public class BemMotorListItemDto {
    private String formId;
    private String motorId;
    private String stfTestNo;
    private StfMotorSubType subType;
    private Status status;
//    private String casingNo;
//    private Double propellantWeight;
    private PersonInfo createdBy;
    private LocalDateTime createdAt;
}
