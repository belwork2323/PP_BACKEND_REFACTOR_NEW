package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
public class NdtTableDTO {
    @Size(max = 100)
    private String articleType;

    @Size(max = 50)
    private String bemNo;

    @NotNull(message = "Date of NDT is required", groups = SubmitGroup.class)
    @JsonProperty("dateTimeOfNdt")
    @JsonAlias({"dateOfNdtDateTime", "dateOfNdt"})
    @Field("dateOfNdt")
    private LocalDateTime dateOfNdtDateTime;

    @Size(max = 500)
    private String observations;
}
