package in.co.bel.acempp.dto.auth.resetpswd;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewedByDto {
    private String userId;
    private String name;
}