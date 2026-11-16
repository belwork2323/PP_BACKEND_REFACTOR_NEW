package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThermalProperties {

    private PropertyValue thermalConductivity;

    private PropertyValue specificHeat;

    private PropertyValue coefficientOfThermalExpansion;

    private PropertyValue ablationRate;
}
