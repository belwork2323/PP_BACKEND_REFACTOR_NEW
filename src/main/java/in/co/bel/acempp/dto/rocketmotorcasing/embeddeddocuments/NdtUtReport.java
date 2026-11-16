package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NdtUtReport {

    private LocalDate postPptUtDate;

    private LocalDate ndtDate;

    private String observations;
}
