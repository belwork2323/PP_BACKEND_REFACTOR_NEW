package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscaleArticleRequest {

    private Long subscaleArticleId;
    @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class})
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
            message = "Invalid article code",
            groups = {CreateGroup.class, UpdateGroup.class}
    )
    private String subscaleArticleCode;
}