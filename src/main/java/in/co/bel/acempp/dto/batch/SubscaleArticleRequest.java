package in.co.bel.acempp.dto.batch;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscaleArticleRequest {

    private Long subscaleArticleId;
    private String subscaleArticleCode;
}