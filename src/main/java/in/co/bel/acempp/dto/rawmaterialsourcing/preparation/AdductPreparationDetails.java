package in.co.bel.acempp.dto.rawmaterialsourcing.preparation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdductPreparationDetails {

    private String adductBatchPrepDate;
    private String tmpMfgLotNo;
    private Double tmpTotalQtyGm;
    private String nbdMfgLotNo;
    private Double nbdTotalQtyGm;
    private Double rpm;
    private Double processTemp;
    private Double jacketTemp;
    private String processStartTime;
    private String processEndTime;
    private Double finalAdductQty;
    private String dispatchDateTime;
}
