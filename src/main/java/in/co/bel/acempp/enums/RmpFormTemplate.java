package in.co.bel.acempp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Catalog of built-in RMP process UIs. Bound on Material Master so user-defined
 * material codes do not need to match hard-coded form names.
 */
public enum RmpFormTemplate {
    DEFAULT,
    AP,
    ALUMINUM,
    DOA,
    HTPB,
    TDI,
    /** Copper Chromite — lot details + sieving datetime + dispatch block. */
    CC,
    /** Iron Oxide — same layout as CC. */
    IO,
    /** NONOX-D — lot details + quantity sieved + mesh + dispatch block. */
    NONOX_D;

    @JsonCreator
    public static RmpFormTemplate fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "DEFAULT", "STANDARD" -> DEFAULT;
            case "AP" -> AP;
            case "ALUMINUM", "ALUMINIUM", "AL" -> ALUMINUM;
            case "DOA" -> DOA;
            case "HTPB" -> HTPB;
            case "TDI", "TBI" -> TDI;
            case "CC", "COPPER_CHROMITE" -> CC;
            case "IO", "IRON_OXIDE" -> IO;
            case "NONOX_D", "NONOXD", "NONOX", "NONOX-D" -> NONOX_D;
            default -> throw new IllegalArgumentException("Unsupported RMP form template: " + value);
        };
    }

    @JsonValue
    public String toValue() {
        return name();
    }

    /** Null / missing on legacy docs → DEFAULT. */
    public static RmpFormTemplate orDefault(RmpFormTemplate template) {
        return template == null ? DEFAULT : template;
    }
}
