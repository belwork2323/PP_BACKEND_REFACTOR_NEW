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
public class AluminumProcessDto {
    /** Lot details replace lot-no / total-qty rows; unloading/dispatch bins are weighment. */
    private String equipmentId;
    private String setRpm;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String observation;
    private String qtyKgQualified;
    private LocalDateTime dispatchDatetime;
    /** QA Raw Material Processing only — ignored by manufacturing RMP UI. */
    private String qaRemarks;
}
