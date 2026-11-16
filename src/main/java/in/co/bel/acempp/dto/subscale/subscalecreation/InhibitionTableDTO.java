package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InhibitionTableDTO {
    private String articleType;
    private String bemNo;
    private String linerCoatedSleeveWeight;
    private String weightBeforeInhibition;
    private String weightAfterInhibition;
    private String irAppliedWeight;
    private String propellantWeight;
    @JsonProperty("dateTimeOfApplication")
    @JsonAlias({"dateOfApplicationDateTime", "dateOfApplication"})
    @Field("dateOfApplication")
    private LocalDateTime dateOfApplicationDateTime;
    private String remarks;
}
