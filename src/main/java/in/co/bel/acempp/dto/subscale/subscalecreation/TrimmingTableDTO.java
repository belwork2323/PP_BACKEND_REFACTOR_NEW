package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String lengthBeforeInhibition;
}
