package in.co.bel.acempp.dto.casepreparation;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePreparationDetailsRequest {
    private List<@Valid CasePreparationMotorRequest> motors;
}
