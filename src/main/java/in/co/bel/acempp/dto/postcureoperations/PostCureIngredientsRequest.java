package in.co.bel.acempp.dto.postcureoperations;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostCureIngredientsRequest {
    /**
     * Recipe key, e.g. LOOSE_FLAP_EPOXY, IR1_PREMIX, IR1_FINAL_MIX,
     * HEMCOAT_3K_PREMIX, HEMCOAT_3K_FINAL_MIX.
     */
    @NotBlank
    private String recipeType;
}
