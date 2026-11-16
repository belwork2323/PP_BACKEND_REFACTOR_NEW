package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insulation {

    private LocalDate insulationCuringDate;

    private String type;

    private String reportNo;

    private String receiptStatus;

    private DocumentDto reportUpload;

    private InsulationSpecificationDto insulationSpecification;

}
