package in.co.bel.acempp.dto.subscale.subscalecreation;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.enums.SubscaleLinerType;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HardwarePreparationDetailsDTO {
    private Integer numberOf40KgBems;
    private Integer numberOf10KgBems;
    private Integer numberOf2KgBems;
    private Integer numberOf20KgBems;
    private Integer numberOfWheelPeels;
    private Integer numberOfCartoons;
    private Integer numberOfSbsTbs;

    @Valid
    private List<HardwareArticleSelectionDTO> articles;

    private SubscaleLinerType linerType;

    @Size(max = 50, message = "Liner batch number cannot exceed 50 characters", groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = "^[A-Za-z0-9\\s.,\\-_()/:%°±≤≥µμ²³⁻]*$", message = "Invalid characters in liner batch number", groups = {DraftGroup.class, SubmitGroup.class})
    private String linerBatchNo;

    private LocalDate linerBatchDate;
}
