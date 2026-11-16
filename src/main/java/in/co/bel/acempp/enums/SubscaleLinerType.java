package in.co.bel.acempp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SubscaleLinerType {
    HEMCOAT_3L("HEMCOAT - 3L"),
    PEDCOAT("PEDCOAT"),
    HEMCOAT_3M("HEMCOAT - 3M"),
    QE_BASED("QE based");

    private final String value;

    SubscaleLinerType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SubscaleLinerType fromValue(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        for (SubscaleLinerType type : SubscaleLinerType.values()) {
            if (type.value.equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown LinerType: " + text);
    }

    @Override
    public String toString() {
        return value;
    }
}
