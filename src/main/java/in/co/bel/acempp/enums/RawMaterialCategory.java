package in.co.bel.acempp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum RawMaterialCategory {
    NORMAL,
    ACEM;

    @JsonCreator
    public static RawMaterialCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "NORMAL", "NORMAL_RAW_MATERIAL" -> NORMAL;
            case "ACEM", "ACEM_RAW_MATERIAL" -> ACEM;
            default -> throw new IllegalArgumentException("Unsupported raw material type: " + value);
        };
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
