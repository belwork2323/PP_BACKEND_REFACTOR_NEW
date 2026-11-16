package in.co.bel.acempp.dto.rawmaterialpreparation;

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
public class MaterialProcessRequest {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long gradeId;
    private String gradeCode;

    /** DEFAULT_SOLID | DEFAULT_LIQUID | AP_* | ALUMINUM | DOA | HTPB | TDI | CC. */
    private String processType;

    @Builder.Default
    private List<LotDetailDto> lotDetails = new ArrayList<>();

    /** QA Raw Material Processing only — section-level remarks under Lot Details. */
    private String lotDetailsQaRemarks;

    /** QA Raw Material Processing only — process-root remarks (e.g. DEFAULT liquid). */
    private String qaRemarks;

    /** Solid only — null/omitted for liquid and custom typed materials. */
    private DryingTrayOvenDto drying;

    /** Solid only — null/omitted for liquid and custom typed materials. */
    private SievingDto sieving;

    /** AP Coarse typed process block — null for other process types. */
    private ApCoarseProcessDto apCoarse;

    /** AP Fine typed process block — null for other process types. */
    private ApFineProcessDto apFine;

    /** AP Ultra Fine typed process block — null for other process types. */
    private ApUltraFineProcessDto apUltraFine;

    /** Aluminum Powder typed process block — null for other process types. */
    private AluminumProcessDto aluminum;

    /** DOA-format dispatch block — used by DOA / HTPB / TDI / CC. Null for other process types. */
    private DoaProcessDto doa;
}
