package in.co.bel.acempp.dto.masterdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDataFieldDef {
    private String key;
    private String label;
    private String dataType;
    private Boolean required;
    private Integer maxLength;
    private Integer min;
    private Integer max;
    private String pattern;
    private Boolean readOnlyOnUpdate;
    private Boolean serverGenerated;
    private Boolean attribute;
}
