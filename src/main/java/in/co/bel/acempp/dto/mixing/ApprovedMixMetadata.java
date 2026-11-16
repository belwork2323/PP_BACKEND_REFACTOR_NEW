package in.co.bel.acempp.dto.mixing;

import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedMixMetadata {

    private Integer premixNo;

    private String mixerId;
    private String buildingNo;
    private String bowlId;

    private LocalDateTime trialDateTime;
    private String observations;

    private LocalDate mixDate;
    private BigDecimal mixQuantity;

    private MixingCycleDto mixingCycle;
}