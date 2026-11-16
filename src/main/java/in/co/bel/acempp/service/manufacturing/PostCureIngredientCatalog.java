package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.postcureoperations.PostCureIngredientDto;
import in.co.bel.acempp.dto.postcureoperations.PostCureIngredientsResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static Post Cure mix recipes (loose-flap epoxy, IR-1, Hemcoat-3K).
 * Source of truth for {@code POST /post-cure/ingredients}.
 */
public final class PostCureIngredientCatalog {

    private PostCureIngredientCatalog() {
    }

    private static PostCureIngredientDto item(
            String ingredient,
            String partsByWeight,
            String... aliases) {
        return PostCureIngredientDto.builder()
                .ingredient(ingredient)
                .partsByWeight(partsByWeight)
                .aliases(aliases == null || aliases.length == 0 ? List.of() : List.of(aliases))
                .build();
    }

    private static final PostCureIngredientsResponse LOOSE_FLAP_EPOXY =
            PostCureIngredientsResponse.builder()
                    .recipeType("LOOSE_FLAP_EPOXY")
                    .fixedPartsByWeight(true)
                    .ingredients(List.of(
                            item("A-125 Hardener", "60 ±1", "A-125", "A125 HARDENER"),
                            item("GX-257 Resin", "40 ±1", "GX-257", "GX257 RESIN"),
                            item("HY-960 Accelerator", "6 ±0.5", "HY-960", "HY960 ACCELERATOR"),
                            item("DY-026 Diluent", "10 ±0.5", "DY-026", "DY026 DILUENT")
                    ))
                    .build();

    private static final PostCureIngredientsResponse IR1_PREMIX =
            PostCureIngredientsResponse.builder()
                    .recipeType("IR1_PREMIX")
                    .fixedPartsByWeight(true)
                    .ingredients(List.of(
                            item("Castor Oil", "60.0±1.0", "CASTOR OIL"),
                            item("Asbestos Powder", "38.5±1.0", "ASBESTOS POWDER"),
                            item("Nonox-D", "1.0±0.1", "NONOX-D", "NONOX D"),
                            item("Ferric Oxide", "0.5±0.05", "FERRIC OXIDE")
                    ))
                    .build();

    private static final PostCureIngredientsResponse IR1_FINAL_MIX =
            PostCureIngredientsResponse.builder()
                    .recipeType("IR1_FINAL_MIX")
                    .fixedPartsByWeight(true)
                    .ingredients(List.of(
                            item("IR-1 Premix", "100", "IR1 PREMIX", "IR-1 PREMIX"),
                            item("TDI", "14.5"),
                            item("Catalyst (5% w/w FeAA in benzene)", "2 ml", "CATALYST", "FeAA")
                    ))
                    .build();

    private static final PostCureIngredientsResponse HEMCOAT_3K_PREMIX =
            PostCureIngredientsResponse.builder()
                    .recipeType("HEMCOAT_3K_PREMIX")
                    .fixedPartsByWeight(true)
                    .ingredients(List.of(
                            item("HTPB", "80.00"),
                            item("NBD", "2.80", "nBD", "n-BD"),
                            item("HT", "2.15", "Hexane Triol", "HEXANE TRIOL"),
                            item("Kaolin", "15.00"),
                            item("Nonox-D", "0.05", "NONOX-D", "NONOX D")
                    ))
                    .build();

    private static final PostCureIngredientsResponse HEMCOAT_3K_FINAL_MIX =
            PostCureIngredientsResponse.builder()
                    .recipeType("HEMCOAT_3K_FINAL_MIX")
                    .fixedPartsByWeight(true)
                    .ingredients(List.of(
                            item("Hemcoat-3K Premix", "98", "HEMCOAT-3K PREMIX", "HEMCOAT 3K PREMIX"),
                            item("H12MDI", "2.3", "H12-MDI"),
                            item("Catalyst (2.5% w/w FeAA in HTPB)", "2 ml", "CATALYST", "FeAA")
                    ))
                    .build();

    private static final Map<String, PostCureIngredientsResponse> BY_TYPE = Map.of(
            "LOOSE_FLAP_EPOXY", LOOSE_FLAP_EPOXY,
            "IR1_PREMIX", IR1_PREMIX,
            "IR1_FINAL_MIX", IR1_FINAL_MIX,
            "HEMCOAT_3K_PREMIX", HEMCOAT_3K_PREMIX,
            "HEMCOAT_3K_FINAL_MIX", HEMCOAT_3K_FINAL_MIX
    );

    public static PostCureIngredientsResponse resolve(String recipeType) {
        String key = recipeType == null ? "" : recipeType.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (key.isEmpty()) {
            return PostCureIngredientsResponse.builder()
                    .recipeType("")
                    .fixedPartsByWeight(false)
                    .ingredients(List.of())
                    .build();
        }
        PostCureIngredientsResponse found = BY_TYPE.get(key);
        if (found != null) {
            return found;
        }
        return PostCureIngredientsResponse.builder()
                .recipeType(key)
                .fixedPartsByWeight(false)
                .ingredients(List.of())
                .build();
    }
}
