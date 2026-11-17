package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TrimmingTableDTO {
    private String articleType;
    private String bemNo;
    private String heOd;
    private String hePortInner;
    private String hePortOuter;
    private String heBeforeInhibitionInner;
    private String heBeforeInhibitionOuter;
    private String neOd;
    private String nePortInner;
    private String nePortOuter;
    private String neWebInner;
    private String neWebOuter;
    @Pattern(regexp = "^\\d*\\.?\\d*$", message = "Invalid numeric format", groups = {DraftGroup.class, SubmitGroup.class})
    private String lengthBeforeInhibition;
}
