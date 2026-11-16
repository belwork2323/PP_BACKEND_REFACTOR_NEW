package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
