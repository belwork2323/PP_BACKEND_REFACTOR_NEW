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
public class PostCureIngredientsResponse {
    private String recipeType;
    /** When true, parts-by-weight are display-only (recipe-fixed). */
    private boolean fixedPartsByWeight;
    @Builder.Default
    private List<PostCureIngredientDto> ingredients = new ArrayList<>();
}
