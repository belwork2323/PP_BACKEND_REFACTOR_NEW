package in.co.bel.acempp.dto.masterdata.mixingcycles;

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
public class MixingCycleMasterListResponse {
    @Builder.Default
    private List<MixingCycleMasterRecordDto> items = new ArrayList<>();
    private MasterDataStatsDto stats;
}
