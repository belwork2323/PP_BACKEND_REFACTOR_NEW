package in.co.bel.acempp.dto.subscale;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HardwarePreparationDetailsDto {
    @JsonProperty("numberOf40KgBems")
    private Integer numberOf40KgBems;

    @JsonProperty("numberOf10KgBems")
    private Integer numberOf10KgBems;

    @JsonProperty("numberOf2KgBems")
    private Integer numberOf2KgBems;

}
