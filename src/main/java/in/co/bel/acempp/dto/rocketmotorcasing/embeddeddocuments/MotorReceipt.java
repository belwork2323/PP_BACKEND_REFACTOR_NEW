package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotorReceipt {

    private String casingType;

    private LocalDate receivingDate;

    private ItemsReceived itemsReceived;

    private Clearances clearances;

    private Insulation insulation;

    private NdtUtReport ndtUtReport;

    private String acemNdtObservations;

    private String projectRubberSurfaceObservations;

    private String otherDetails;
}
