package in.co.bel.acempp.dto.masterdata.qualitychecks;

import in.co.bel.acempp.dto.masterdata.MasterDataStatsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckMasterListResponse {
    @Builder.Default
    private List<QualityCheckMasterRecordDto> items = new ArrayList<>();
    private MasterDataStatsDto stats;
}
