package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Weightment {

    private MeasurementValue weightWithoutHarness;

    private MeasurementValue weightWithHarness;

    private WeighscaleCalibration weighscaleCalibration;
}
