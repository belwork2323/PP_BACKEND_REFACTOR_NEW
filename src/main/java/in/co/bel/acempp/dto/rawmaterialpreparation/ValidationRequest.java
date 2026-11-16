package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequest {
    private boolean compareWithIdentificationSheet;
    private boolean deviationFound;
    private String deviationMessage;
}