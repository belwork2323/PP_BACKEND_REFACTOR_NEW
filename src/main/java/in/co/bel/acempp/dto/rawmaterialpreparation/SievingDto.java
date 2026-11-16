package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SievingDto {
    private LocalDateTime sievingDispatchDatetime;
    private String sievedQuantity;
    private String sieveMeshSize;
    private String observation;
    /** QA Raw Material Processing only — ignored by manufacturing RMP UI. */
    private String qaRemarks;
}
