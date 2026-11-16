package in.co.bel.acempp.dto.masterdata.curingcycles;

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
public class CuringCycleMasterListResponse {
    @Builder.Default
    private List<CuringCycleMasterRecordDto> items = new ArrayList<>();
    private MasterDataStatsDto stats;
}
