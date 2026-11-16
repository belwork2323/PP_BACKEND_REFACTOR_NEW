package in.co.bel.acempp.dto.rawmaterialsourcing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpecificationRequest {
    @NotBlank(message = "specificationCode is required")
    private String specificationCode;

    @JsonDeserialize(using = AnalysedResultDeserializer.class)
    private String analysedResult;

//    @NotNull(message = "isOutOfRange is required")
//    private Boolean isOutOfRange;

    @JsonDeserialize(using = AnalysedResultDeserializer.class)
    private String acemQcResult;
}
