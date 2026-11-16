package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeighscaleCalibration {

    private String equipmentDetails;

    private LocalDate calibrationDueDate;
}
