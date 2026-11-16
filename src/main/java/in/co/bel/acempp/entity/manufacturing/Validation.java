package in.co.bel.acempp.entity.manufacturing;

import lombok.*;

@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class Validation {
    private Boolean compareWithIdentificationSheet;
    private Boolean deviationFound;
    private String deviationMessage;
}