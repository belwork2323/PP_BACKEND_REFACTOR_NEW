package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.rawmaterialsourcing.AnalysedResultDeserializer;
import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecificationParameterDto {

    private String specificationCode;

    /** Populated on form details from master insulation specs; not required on create/update. */
    private String specificationName;

    /** Populated on form details from master insulation specs; not required on create/update. */
    private ReferenceRange referenceRange;

    /** Numeric when master has a range; alphanumeric text when range is N/A. */
    @JsonDeserialize(using = AnalysedResultDeserializer.class)
    private String reported;

    /** Numeric when master has a range; alphanumeric text when range is N/A. */
    @JsonDeserialize(using = AnalysedResultDeserializer.class)
    private String acemSpec;
}
