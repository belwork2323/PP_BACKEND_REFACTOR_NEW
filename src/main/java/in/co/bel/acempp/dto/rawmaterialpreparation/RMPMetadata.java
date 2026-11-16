package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RMPMetadata {

    private String formId;

    private List<PremixMetadata> premixes;

    private WeightmentSheetRequest weightmentSheet;
}
