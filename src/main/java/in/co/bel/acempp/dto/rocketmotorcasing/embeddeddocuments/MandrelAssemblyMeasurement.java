package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.IntegerValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MandrelAssemblyMeasurement {

    @JsonDeserialize(using = IntegerValue.class)
    private Integer srNo;

    @JsonDeserialize(using = BigDecimalValue.class)
    private BigDecimal mandrelRestOnDomeA;

    @JsonDeserialize(using = BigDecimalValue.class)
    private BigDecimal mandrelRestOnBottomCupB;

    @JsonDeserialize(using = BigDecimalValue.class)
    private BigDecimal differenceC;

    @JsonDeserialize(using = BigDecimalValue.class)
    private BigDecimal bellowThicknessD;

    @JsonDeserialize(using = BigDecimalValue.class)
    private BigDecimal mandrelLiftE;
}
