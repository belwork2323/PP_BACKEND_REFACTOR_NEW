package in.co.bel.acempp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum PostCureInhibitorType {
    IR1,
    HEMCOAT_3K,
    OTHERS,
    NOT_APPLICABLE;

    @JsonCreator
    public static PostCureInhibitorType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "IR1" -> IR1;
            case "HEMCOAT_3K", "HEMCOAT3K" -> HEMCOAT_3K;
            case "OTHERS", "OTHER" -> OTHERS;
            case "NOT_APPLICABLE", "NA", "N_A" -> NOT_APPLICABLE;
            default -> throw new IllegalArgumentException(
                    "Unsupported Post Cure inhibitor type: " + value);
        };
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
