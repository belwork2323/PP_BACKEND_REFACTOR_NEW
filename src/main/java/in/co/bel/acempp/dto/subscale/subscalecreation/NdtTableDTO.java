package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NdtTableDTO {
    private String articleType;
    private String bemNo;
    @JsonProperty("dateTimeOfNdt")
    @JsonAlias({"dateOfNdtDateTime", "dateOfNdt"})
    @Field("dateOfNdt")
    private LocalDateTime dateOfNdtDateTime;
    private String observations;
}
