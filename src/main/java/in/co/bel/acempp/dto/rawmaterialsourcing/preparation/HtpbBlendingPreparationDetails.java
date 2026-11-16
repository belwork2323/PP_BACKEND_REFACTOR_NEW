package in.co.bel.acempp.dto.rawmaterialsourcing.preparation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HtpbBlendingPreparationDetails implements BlendingStylePreparationDetails {

    private String mfgBatchLotNo;
    private Double totalQty;
    private String equipmentId;
    private Double agitatorRpm;
    private Double processTemp;
    private Double jacketTemp;
    private String processStartTime;
    private String processEndTime;
    private String otherObservations;
}
