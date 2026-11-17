package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CastingDetailsDTO {
    @JsonProperty("dateTimeOfCasting")
    @JsonAlias({"dateOfCastingDateTime", "dateOfCasting"})
    @Field("dateOfCasting")
    private LocalDateTime dateOfCastingDateTime;

    @Valid
    private List<CastingTableDTO> castingTable;
}
