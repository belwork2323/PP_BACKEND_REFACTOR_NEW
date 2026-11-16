package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.casepreparation.CasePrepLinerIngredientDto;
import in.co.bel.acempp.dto.casepreparation.CasePrepLinerIngredientsResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static liner coating recipes for Case Preparation (premix / final mix).
 * Source of truth for {@code POST /case-preparation/liner-ingredients}.
 */
public final class CasePrepLinerIngredientCatalog {

    private CasePrepLinerIngredientCatalog() {
    }

    private static CasePrepLinerIngredientDto item(
            String ingredient,
            String partsByWeight,
            String... aliases) {
        return CasePrepLinerIngredientDto.builder()
                .ingredient(ingredient)
                .partsByWeight(partsByWeight)
                .aliases(aliases == null || aliases.length == 0 ? List.of() : List.of(aliases))
                .build();
    }

    private static final CasePrepLinerIngredientsResponse PEDCOAT =
            CasePrepLinerIngredientsResponse.builder()
                    .linerType("PEDCOAT")
                    .fixedPartsByWeight(true)
                    .premixIngredients(List.of(
                            item("HTPB", "100.00"),
                            item("TMP", "1.00"),
                            item("V2O5", "1.60", "V₂O₅", "V2O5"),
                            item("Carbon Black", "10.00", "CARBON BLACK")
                    ))
                    .finalMixIngredients(List.of(
                            item("Liner Premix", "100.00", "LINER PREMIX"),
                            item("TDI", "10.64"),
                            item("CH2Cl2", "110.64", "CH₂Cl₂", "CH2CL2", "Dichloromethane")
                    ))
                    .build();

    private static final CasePrepLinerIngredientsResponse HEMCOAT =
            CasePrepLinerIngredientsResponse.builder()
                    .linerType("HEMCOAT_3L")
                    .fixedPartsByWeight(false)
                    .premixIngredients(List.of(
                            item("HTPB", null),
                            item("nBD", null, "NBD", "n-BD"),
                            item("Hexane Triol", null, "HEXANE TRIOL", "Hexanetriol"),
                            item("Carbon Black", null, "CARBON BLACK")
                    ))
                    .finalMixIngredients(List.of(
                            item("Liner Premix", null, "LINER PREMIX"),
                            item("TDI", null),
                            item("CH2Cl2", null, "CH₂Cl₂", "CH2CL2", "Dichloromethane")
                    ))
                    .build();

    private static final Map<String, CasePrepLinerIngredientsResponse> BY_TYPE = Map.of(
            "PEDCOAT", PEDCOAT,
            "HEMCOAT_3L", copyWithType(HEMCOAT, "HEMCOAT_3L"),
            "HEMCOAT_3L_M", copyWithType(HEMCOAT, "HEMCOAT_3L_M")
    );

    private static CasePrepLinerIngredientsResponse copyWithType(
            CasePrepLinerIngredientsResponse source,
            String linerType) {
        return CasePrepLinerIngredientsResponse.builder()
                .linerType(linerType)
                .fixedPartsByWeight(source.isFixedPartsByWeight())
                .premixIngredients(source.getPremixIngredients())
                .finalMixIngredients(source.getFinalMixIngredients())
                .build();
    }

    public static CasePrepLinerIngredientsResponse resolve(String linerType) {
        String key = linerType == null ? "" : linerType.trim().toUpperCase(Locale.ROOT);
        if (key.isEmpty() || "OTHERS".equals(key)) {
            return CasePrepLinerIngredientsResponse.builder()
                    .linerType(key.isEmpty() ? "" : key)
                    .fixedPartsByWeight(false)
                    .premixIngredients(List.of())
                    .finalMixIngredients(List.of())
                    .build();
        }
        CasePrepLinerIngredientsResponse found = BY_TYPE.get(key);
        if (found != null) {
            return found;
        }
        return CasePrepLinerIngredientsResponse.builder()
                .linerType(key)
                .fixedPartsByWeight(false)
                .premixIngredients(List.of())
                .finalMixIngredients(List.of())
                .build();
    }
}
