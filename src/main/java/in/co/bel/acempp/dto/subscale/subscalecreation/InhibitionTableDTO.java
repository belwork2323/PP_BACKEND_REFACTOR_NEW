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
public class InhibitionTableDTO {
    private String articleType;
    private String bemNo;

    @Pattern(regexp = "^\\d*\\.?\\d*$", groups = {DraftGroup.class, SubmitGroup.class})
    private String linerCoatedSleeveWeight;

    @Pattern(regexp = "^\\d*\\.?\\d*$", groups = {DraftGroup.class, SubmitGroup.class})
    private String weightBeforeInhibition;

    @Pattern(regexp = "^\\d*\\.?\\d*$", groups = {DraftGroup.class, SubmitGroup.class})
    private String weightAfterInhibition;

    @Pattern(regexp = "^\\d*\\.?\\d*$", groups = {DraftGroup.class, SubmitGroup.class})
    private String irAppliedWeight;

    // Frontend: PROPELLANT_WEIGHT uses negativeFloat pattern (/^-?\d+(\.\d{1,5})?$/)
    @Pattern(regexp = "^-?\\d+(\\.\\d{1,5})?$", message = "Propellant weight must be a signed float with up to 5 decimal places", groups = {DraftGroup.class, SubmitGroup.class})
    private String propellantWeight;

    @NotNull(message = "Date of application is required", groups = {SubmitGroup.class})
    @JsonProperty("dateTimeOfApplication")
    @JsonAlias({"dateOfApplicationDateTime", "dateOfApplication"})
    @Field("dateOfApplication")
    private LocalDateTime dateOfApplicationDateTime;

    @Size(max = 500)
    private String remarks;
}
