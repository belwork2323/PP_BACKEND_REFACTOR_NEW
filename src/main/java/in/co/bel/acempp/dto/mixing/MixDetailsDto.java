package in.co.bel.acempp.dto.mixing;

import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MixDetailsDto {

    @NotNull(message = "Mix date is required", groups = SubmitGroup.class)
    private LocalDate mixDate;

    @NotNull(message = "Mix quantity is required", groups = SubmitGroup.class)
    @DecimalMin(value = "0.01", message = "Mix quantity must be greater than 0",
            groups = {DraftGroup.class, SubmitGroup.class})
    @DecimalMax(value = "100000.00", message = "Mix quantity exceeds maximum limit",
            groups = {DraftGroup.class, SubmitGroup.class})
    @Digits(integer = 8, fraction = 2, message = "Mix quantity can have up to 8 digits and 2 decimal places",
            groups = {DraftGroup.class, SubmitGroup.class})
//    @JsonDeserialize(using = StrictScalarDeserializers.JsonNumberBigDecimalValue.class)
    private BigDecimal mixQuantity;
}
