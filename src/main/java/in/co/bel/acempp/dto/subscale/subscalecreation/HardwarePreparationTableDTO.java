package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HardwarePreparationTableDTO {
    @Size(max = 100, message = "Article type max length is 100", groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = "^[A-Za-z0-9\\s.,\\-_()/:%°±≤≥µμ²³⁻]*$", message = "Invalid characters in article type", groups = {DraftGroup.class, SubmitGroup.class})
    private String articleType;

    @Size(max = 100, message = "Rubber material max length is 100", groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = "^[A-Za-z0-9\\s.,\\-_()/:%°±≤≥µμ²³⁻]*$", message = "Invalid characters in rubber material", groups = {DraftGroup.class, SubmitGroup.class})
    private String rubberMaterial;

    @Size(max = 50, message = "Sleeve number max length is 50", groups = {DraftGroup.class, SubmitGroup.class})
    private String sleeveNo;

    @Size(max = 50, message = "Mould number max length is 50", groups = {DraftGroup.class, SubmitGroup.class})
    private String mouldNo;

    // Frontend: sizeRule -> numberRule (FLOAT pattern: /^\d*\.?\d*$/)
    @Pattern(regexp = "^\\d*\\.?\\d*$", message = "Invalid numeric format for length", groups = {DraftGroup.class, SubmitGroup.class})
    private String lengthMm;

    @Pattern(regexp = "^\\d*\\.?\\d*$", message = "Invalid numeric format for thickness", groups = {DraftGroup.class, SubmitGroup.class})
    private String thicknessMm;

    @Size(max = 100, message = "Liner applied max length is 100", groups = {DraftGroup.class, SubmitGroup.class})
    private String linerApplied;

    @Size(max = 500, message = "Observations max length is 500", groups = {DraftGroup.class, SubmitGroup.class})
    private String observations;
}
