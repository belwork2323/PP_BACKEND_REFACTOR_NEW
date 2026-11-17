package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
public class HardwareArticleSelectionDTO {
    private Long subscaleArticleId;
    private String subscaleArticleCode;
    private String subscaleArticleName;
    private Integer noOfArticles;
    private Integer noOfParts;
    /** Snapshotted from Subscale Article Master at selection time. */
    private Boolean isBem;
}
