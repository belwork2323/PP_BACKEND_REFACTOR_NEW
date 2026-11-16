package in.co.bel.acempp.dto.masterdata.qcdivisions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionMasterListRequest {
    private String search;
    private Boolean isActive;
}
