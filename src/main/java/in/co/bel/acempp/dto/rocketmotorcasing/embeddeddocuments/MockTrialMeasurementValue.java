package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockTrialMeasurementValue {

    @JsonDeserialize(using = BigDecimalValue.class)
    private BigDecimal value;

    private String unit;
}
