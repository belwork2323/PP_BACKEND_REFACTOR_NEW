package in.co.bel.acempp.dto.masterdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDataTypeDescriptor {
    private String type;
    private String label;
    private List<MasterDataFieldDef> fields;
}
