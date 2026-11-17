package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InhibitionDetailsDTO {
    @NotNull(message = "IR Batch No is required", groups = SubmitGroup.class)
    @Size(max = 50, groups = {DraftGroup.class, SubmitGroup.class})
    private String irBatchNo;

    @NotNull(message = "Date of manufacturing is required", groups = SubmitGroup.class)
    @JsonProperty("dateTimeOfManufacturing")
    @JsonAlias({"dateOfManufacturingDateTime", "dateOfManufacturing"})
    @Field("dateOfManufacturing")
    private LocalDateTime dateOfManufacturingDateTime;

    @Valid
    private List<InhibitionTableDTO> inhibitionTable;
}
