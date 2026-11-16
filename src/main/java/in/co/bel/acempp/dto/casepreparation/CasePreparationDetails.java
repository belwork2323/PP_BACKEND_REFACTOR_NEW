package in.co.bel.acempp.dto.casepreparation;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CasePreparationDetails {
    private List<Motor> motors;
}
