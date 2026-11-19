package in.co.bel.acempp.dto.rawmaterialsourcing.preparation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.rawmaterialsourcing.DecimalStringDeserializer;
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

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String tmpTotalQtyGm;

    private String nbdMfgLotNo;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String nbdTotalQtyGm;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String rpm;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String processTemp;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String jacketTemp;

    private String processStartTime;
    private String processEndTime;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String finalAdductQty;

    private String dispatchDateTime;
}
