package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
public class CuringTableDTO {
    @Size(max = 100)
    private String articleType;

    @Size(max = 50)
    @JsonAlias("bemMouldNo")
    private String bemNo;

    private LocalDateTime curingStartDateTime;
    private LocalDateTime curingEndDateTime;

    @Size(max = 100)
    private String ovenNo;

    // Frontend: TEMPERATURE -> numberRule()
    @Pattern(regexp = "^\\d*\\.?\\d*$", message = "Invalid temperature format", groups = {DraftGroup.class, SubmitGroup.class})
    private String temperature;

    @Size(max = 100)
    private String hardness;

    private LocalDateTime decoringDateTime;

    private String decoringLoad;

    @Size(max = 500)
    private String grainSurfaceObservations;
}
