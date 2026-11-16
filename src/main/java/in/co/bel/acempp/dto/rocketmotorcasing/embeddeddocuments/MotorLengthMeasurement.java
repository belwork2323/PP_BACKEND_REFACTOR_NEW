package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.IntegerValue;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class MotorLengthMeasurement {

    @JsonDeserialize(using = IntegerValue.class)
    private Integer srNo;

    @Valid
    private MockTrialMeasurementValue lfRubberThicknessHe;

    @Valid
    private MockTrialMeasurementValue heBossWidthWithoutLfRubber;

    @Valid
    private MockTrialMeasurementValue heDiaId;

    @Valid
    private MotorLengthDetails motorLength;
}
