package in.co.bel.acempp.dto.subscale.subscalecreation;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.enums.SubscaleLinerType;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HardwarePreparationDetailsDTO {
    /** Legacy fixed counts — retained for older saved forms. */
    private Integer numberOf40KgBems;
    private Integer numberOf10KgBems;
    private Integer numberOf2KgBems;
    private Integer numberOf20KgBems;
    private Integer numberOfWheelPeels;
    private Integer numberOfCartoons;
    private Integer numberOfSbsTbs;
    /** MDM-driven article selections (new hardware prep flow). */
    private List<HardwareArticleSelectionDTO> articles;
    private SubscaleLinerType linerType;
    private String linerBatchNo;
    private LocalDate linerBatchDate;
}
