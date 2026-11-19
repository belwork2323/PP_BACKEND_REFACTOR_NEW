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
public class HtpbBlendingPreparationDetails implements BlendingStylePreparationDetails {

    private String mfgBatchLotNo;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String totalQty;

    private String equipmentId;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String agitatorRpm;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String processTemp;

    @JsonDeserialize(using = DecimalStringDeserializer.class)
    private String jacketTemp;

    private String processStartTime;
    private String processEndTime;
    private String otherObservations;
}
