package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Shared dispatch / premix block used by DOA, HTPB, TDI, CC, IO, NONOX_D.
 * Extra fields are optional and material-specific.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoaProcessDto {
    /** CC / IO — sieving date and time. */
    private LocalDateTime sievingDatetime;
    /** NONOX-D — quantity sieved (kg). */
    private String quantitySieved;
    /** NONOX-D — sieve mesh size. */
    private String sieveMeshSize;
    private LocalDateTime dispatchDatetime;
    private String observation;
    private String totalQtySentForPremix;
    /** QA Raw Material Processing only — ignored by manufacturing RMP UI. */
    private String qaRemarks;
}
