package in.co.bel.acempp.dto.qcdivision;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcStageDivisionStatusDto {
    private Long divisionId;
    private Status divisionSubmissionType;
    private Status status;
    private String divisionName;
    private List<PremixStatusResponse> premixStatuses;
    private List<PremixStatusResponse> finalMixStatuses;
    private List<MotorStatusResponse> motorStatuses;
}
