package in.co.bel.acempp.dto.batch;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscaleArticleInfo {

    private Long subscaleArticleId;
    private String subscaleArticleCode;
    private String subscaleArticleName;
    private Boolean isBem;
    private Boolean isActive;
}