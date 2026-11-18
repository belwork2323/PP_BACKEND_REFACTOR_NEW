package in.co.bel.acempp.dto.mixing.processparticulars;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessParticularRowDto {


    @NotNull(message = "Operation ID is required", groups = SubmitGroup.class)
    @Positive(message = "Operation ID must be greater than 0", groups = {DraftGroup.class, SubmitGroup.class})
//    @JsonDeserialize(using = StrictScalarDeserializers.JsonNumberLongValue.class)
    private Long operationId;

    private String operationName;

    @NotNull(message = "RPM is required", groups = SubmitGroup.class)
//    @JsonDeserialize(using = StrictScalarDeserializers.JsonNumberBigDecimalValue.class)
    private Integer rpm;

    @NotNull(message = "Time in minutes is required", groups = SubmitGroup.class)
    @PositiveOrZero(message = "Time in minutes cannot be negative", groups = {DraftGroup.class, SubmitGroup.class})
    @Digits(integer = 8, fraction = 2, message = "Time can have up to 8 digits and 2 decimal places",
            groups = {DraftGroup.class, SubmitGroup.class})
//    @JsonDeserialize(using = StrictScalarDeserializers.JsonNumberBigDecimalValue.class)
    private BigDecimal time;

    @NotNull(message = "Temperature is required", groups = SubmitGroup.class)
    @Digits(integer = 8, fraction = 2, message = "Temperature can have up to 8 digits and 2 decimal places",
            groups = {DraftGroup.class, SubmitGroup.class})
//    @JsonDeserialize(using = StrictScalarDeserializers.JsonNumberBigDecimalValue.class)
    private BigDecimal temp;

    @NotNull(message = "Vacuum is required", groups = SubmitGroup.class)
    @PositiveOrZero(message = "Vacuum cannot be negative", groups = {DraftGroup.class, SubmitGroup.class})
    @Digits(integer = 8, fraction = 2, message = "Vacuum can have up to 8 digits and 2 decimal places",
            groups = {DraftGroup.class, SubmitGroup.class})
//    @JsonDeserialize(using = StrictScalarDeserializers.JsonNumberBigDecimalValue.class)
    private BigDecimal vacuum;
}
