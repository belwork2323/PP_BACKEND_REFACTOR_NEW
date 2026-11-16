package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clearances {

    private String greenCardStatus;

    private String greenCardNo;

    private LocalDate clearanceDate;

    private String authority;

    private String detailsAndObservations;
}
