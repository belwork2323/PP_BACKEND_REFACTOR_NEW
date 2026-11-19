package in.co.bel.acempp.validation;

/** Shared validation limits and patterns used across sub-department forms. */
public final class ValidationConstants {

    public static final int STANDARD_TEXT_MAX = 100;
    /** Casting / legacy observations cap — do not reuse for new RMS remarks (use REMARKS_MAX). */
    public static final int REMARKS_OBSERVATIONS_MAX = 255;

    // Temporary Casting specification until the approved production range is provided.
    public static final String CASTING_VACUUM_LEVEL_MIN = "0";
    public static final String CASTING_VACUUM_LEVEL_MAX = "760";

    public static final String BUSINESS_TEXT_PATTERN = "^[A-Za-z0-9\\s/_-]+$";
    public static final String OPTIONAL_BUSINESS_TEXT_PATTERN = "^$|^[A-Za-z0-9\\s/_-]+$";
    public static final String OPTIONAL_DESCRIPTIVE_TEXT_PATTERN =
            "^$|^[A-Za-z0-9\\s\\-_,./:()]+$";

    public static final String ALPHABET_WITH_SPECIAL = "^[a-zA-Z0-9\\s\\-_./(),&+:]+$";
    public static final int MAX_STANDARD = 100;
    public static final int MAX_SHORT = 50;
    public static final int MAX_LONG_TEXT = 500;

    /** Short Text (Name): letters, numbers, spaces — max 100. */
    public static final int SHORT_TEXT_MIN = 1;
    public static final int SHORT_TEXT_MAX = 100;
    public static final String SHORT_TEXT_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 ]*$";

    /** Code / Reference Number: letters, numbers, hyphen, underscore — max 50. */
    public static final int CODE_REF_MIN = 1;
    public static final int CODE_REF_MAX = 50;
    public static final String CODE_REF_PATTERN = "^[A-Za-z0-9][A-Za-z0-9_-]*$";

    /** Remarks (optional): alphanumeric + common specials — max 500. */
    public static final int REMARKS_MIN = 0;
    public static final int REMARKS_MAX = 500;
    public static final String REMARKS_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 \\-_,./:()]*$";

    /** Decimal (+/−): character length and fraction digits. */
    public static final int DECIMAL_MAX_LENGTH = 20;
    public static final int DECIMAL_MAX_LENGTH_SIGNED = 21;
    public static final int DECIMAL_MAX_PLACES = 6;

    private ValidationConstants() {
    }
}
