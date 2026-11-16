package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.rawmaterialsourcing.AnalysedResultDeserializer;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicalProperty {

    private String paramKey;

    private String paramName;

    @JsonDeserialize(using = AnalysedResultDeserializer.class)
    private String reported;

    @JsonDeserialize(using = AnalysedResultDeserializer.class)
    private String acemSpec;

    private String unit;
}
