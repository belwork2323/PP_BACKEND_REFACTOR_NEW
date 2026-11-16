package in.co.bel.acempp.entity.manufacturing;

import in.co.bel.acempp.dto.rawmaterialpreparation.ApCoarseProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.ApFineProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.ApUltraFineProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.AluminumProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.DoaProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.DryingTrayOvenDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.LotDetailDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.SievingDto;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialProcess {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long gradeId;
    private String gradeCode;

    /** DEFAULT_SOLID | DEFAULT_LIQUID | AP_* | ALUMINUM | DOA | HTPB | TDI | CC. */
    private String processType;

    @Builder.Default
    private List<LotDetailDto> lotDetails = new ArrayList<>();

    /** QA Raw Material Processing only. */
    private String lotDetailsQaRemarks;
    /** QA Raw Material Processing only. */
    private String qaRemarks;

    private DryingTrayOvenDto drying;
    private SievingDto sieving;
    private ApCoarseProcessDto apCoarse;
    private ApFineProcessDto apFine;
    private ApUltraFineProcessDto apUltraFine;
    private AluminumProcessDto aluminum;
    private DoaProcessDto doa;

    /**
     * Legacy flexible sections — retained for reading old documents only.
     * New writes leave this null/empty; prefer typed fields above.
     */
    @Deprecated
    private List<SectionData> sections;
}
