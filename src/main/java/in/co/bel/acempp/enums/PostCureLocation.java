package in.co.bel.acempp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum PostCureLocation {
    HE_SIDE,
    NE_SIDE;

    @JsonCreator
    public static PostCureLocation fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "HE", "HE_SIDE", "HEAD_END", "HEAD_END_SIDE" -> HE_SIDE;
            case "NE", "NE_SIDE", "NOZZLE_END", "NOZZLE_END_SIDE" -> NE_SIDE;
            default -> throw new IllegalArgumentException(
                    "Unsupported Post Cure location: " + value);
        };
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
