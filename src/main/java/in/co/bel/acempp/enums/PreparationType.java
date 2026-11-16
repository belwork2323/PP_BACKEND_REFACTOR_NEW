package in.co.bel.acempp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum PreparationType {
    ADDUCT,
    HTPB_BLENDING;

    @JsonCreator
    public static PreparationType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "ADDUCT" -> ADDUCT;
            case "HTPB_BLENDING", "HTPBBLENDING" -> HTPB_BLENDING;
            default -> throw new IllegalArgumentException("Unsupported preparation type: " + value);
        };
    }

    @JsonValue
    public String toValue() {
        return switch (this) {
            case ADDUCT -> "ADDUCT";
            case HTPB_BLENDING -> "HTPB Blending";
        };
    }
}
