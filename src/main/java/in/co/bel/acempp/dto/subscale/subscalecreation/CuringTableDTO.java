package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CuringTableDTO {
    private String articleType;
    /** Article No / BEM No (synced from casting); legacy JSON key: bemMouldNo. */
    @JsonAlias("bemMouldNo")
    private String bemNo;
    @JsonAlias("curingStartDate")
    @Field("curingStartDate")
    private LocalDateTime curingStartDateTime;
    @JsonAlias("curingEndDate")
    @Field("curingEndDate")
    private LocalDateTime curingEndDateTime;
    private String ovenNo;
    private String temperature;
    private String hardness;
    @JsonAlias("decoringDate")
    @Field("decoringDate")
    private LocalDateTime decoringDateTime;
    private String decoringLoad;
    private String grainSurfaceObservations;
}
