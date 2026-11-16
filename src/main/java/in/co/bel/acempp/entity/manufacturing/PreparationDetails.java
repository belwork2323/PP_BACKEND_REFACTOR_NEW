package in.co.bel.acempp.entity.manufacturing;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PreparationDetails {
    private List<Premix> premixes;
    private WeightmentSheet weightmentSheet;
}