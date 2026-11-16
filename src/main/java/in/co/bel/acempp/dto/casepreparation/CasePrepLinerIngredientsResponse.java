package in.co.bel.acempp.dto.casepreparation;

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
public class CasePrepLinerIngredientsResponse {
    private String linerType;
    /** When true, parts-by-weight come from the recipe and should be read-only in UI. */
    private boolean fixedPartsByWeight;
    @Builder.Default
    private List<CasePrepLinerIngredientDto> premixIngredients = new ArrayList<>();
    @Builder.Default
    private List<CasePrepLinerIngredientDto> finalMixIngredients = new ArrayList<>();
}
