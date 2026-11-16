package in.co.bel.acempp.dto.postcureoperations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCureIngredientDto {
    /** Display name for the Ingredient column. */
    private String ingredient;
    /** Alternate names used to match materials master. */
    @Builder.Default
    private List<String> aliases = new ArrayList<>();
    /** Fixed parts-by-weight when known; null when editable/blank. */
    private String partsByWeight;
    /** Materials-master id when resolved; null if no match. */
    private Long materialId;
    /** Materials-master code when resolved; null if no match → MFG Lot is free text. */
    private String materialCode;
}
