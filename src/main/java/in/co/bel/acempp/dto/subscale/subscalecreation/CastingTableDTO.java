package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
public class CastingTableDTO {
    private String articleType;
    /** Article No / BEM No — unique within the same articleType (preferred identity). */
    private String bemNo;
    /** Mould number — not unique; casting-local only. */
    private String bemMouldNo;
    private String castingPitNo;
    @JsonAlias("castingStartTime")
    @Field("castingStartTime")
    private LocalDateTime castingStartDateTime;
    @JsonAlias("castingEndTime")
    @Field("castingEndTime")
    private LocalDateTime castingEndDateTime;
    @Pattern(
            regexp = "^[A-Za-z0-9\\s,]*$",
            message = "Vacuum level must be a valid signed number with up to 5 decimal places",
            groups = {DraftGroup.class, SubmitGroup.class}
    )
    private String vacuumLevel;
    private String remarks;

    /** Prefer bemNo; fall back to bemMouldNo for older saved forms. */
    public String resolveBemIdentity() {
        if (bemNo != null && !bemNo.isBlank()) {
            return bemNo.trim();
        }
        if (bemMouldNo != null && !bemMouldNo.isBlank()) {
            return bemMouldNo.trim();
        }
        return null;
    }
}
