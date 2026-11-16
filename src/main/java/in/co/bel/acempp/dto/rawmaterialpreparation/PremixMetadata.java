package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremixMetadata {
    private Integer premixNo;

    private LocalDate premixDate;

    private String materialType;

    private List<MaterialMetadata> materials;
}
