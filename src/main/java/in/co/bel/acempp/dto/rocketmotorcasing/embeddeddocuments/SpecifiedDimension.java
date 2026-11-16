package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecifiedDimension {

    private Double minValue;

    private Double maxValue;

    private String unit;

    private String source;
}
