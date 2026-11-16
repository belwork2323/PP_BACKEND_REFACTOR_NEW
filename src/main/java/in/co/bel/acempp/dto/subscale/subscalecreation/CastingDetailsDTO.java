package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CastingDetailsDTO {
    @JsonProperty("dateTimeOfCasting")
    @JsonAlias({"dateOfCastingDateTime", "dateOfCasting"})
    @Field("dateOfCasting")
    private LocalDateTime dateOfCastingDateTime;
    private List<CastingTableDTO> castingTable;
}
