package in.co.bel.acempp.validation;

/** Shared validation limits and patterns used across sub-department forms. */
public final class ValidationConstants {

    public static final int STANDARD_TEXT_MAX = 100;
    public static final int REMARKS_OBSERVATIONS_MAX = 255;

    // Temporary Casting specification until the approved production range is provided.
    public static final String CASTING_VACUUM_LEVEL_MIN = "0";
    public static final String CASTING_VACUUM_LEVEL_MAX = "760";

    public static final String BUSINESS_TEXT_PATTERN = "^[A-Za-z0-9\\s/_-]+$";
    public static final String OPTIONAL_BUSINESS_TEXT_PATTERN = "^$|^[A-Za-z0-9\\s/_-]+$";
    public static final String OPTIONAL_DESCRIPTIVE_TEXT_PATTERN =
            "^$|^[A-Za-z0-9\\s\\-_,./:()]+$";

    private ValidationConstants() {
    }
}
