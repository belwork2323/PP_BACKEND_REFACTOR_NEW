package in.co.bel.acempp.dto.masterdata.materials;

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
public class MaterialMasterListResponse {
    @Builder.Default
    private List<MaterialMasterRecordDto> items = new ArrayList<>();
    private MasterDataStatsDto stats;
}
