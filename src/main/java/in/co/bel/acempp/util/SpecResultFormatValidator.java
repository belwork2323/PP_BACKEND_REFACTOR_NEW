package in.co.bel.acempp.util;

import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import in.co.bel.acempp.entity.MaterialSpecification;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared format rules for specification result values (analysed / ACEM QC):
 * numeric when a reference range has min and/or max; alphanumeric when N/A.
 */
public final class SpecResultFormatValidator {

    /** Letters, numbers, spaces, hyphens, underscores, or slashes (FE ALPHA_NUM). */
    public static final Pattern ALPHA_NUM_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 /_-]*$");

    private static final Pattern SPEC_RANGE_LABEL_PATTERN =
            Pattern.compile("-?\\d+(?:\\.\\d+)?\\s*[-\u2013\u2014]\\s*-?\\d+(?:\\.\\d+)?");
    private static final Pattern SPEC_BOUND_LABEL_PATTERN =
            Pattern.compile("(?:\u2265|>=|=>|>|\u2264|<=|=<|<)\\s*-?\\d+(?:\\.\\d+)?");

    private SpecResultFormatValidator() {}

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean isReferenceRangeNotApplicable(Double minValue, Double maxValue) {
        return minValue == null && maxValue == null;
    }

    public static boolean isReferenceRangeNotApplicable(ReferenceRange referenceRange) {
        if (referenceRange == null) {
            return true;
        }
        return isReferenceRangeNotApplicable(referenceRange.getMinValue(), referenceRange.getMaxValue());
    }

    public static boolean isParseableNumeric(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(value.replace(",", "").trim());
            return Double.isFinite(parsed);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean matchesAlphaNum(String value) {
        return value != null && ALPHA_NUM_PATTERN.matcher(value).matches();
    }

    /**
     * Validate optional text IDs (manufacturer, lotId, certificateType, prep lot numbers).
     * Blank values are ignored unless {@code required} is true.
     */
    public static void validateAlphaNumField(
            String value,
            String fieldPath,
            boolean required,
            Map<String, String> errors) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            if (required) {
                errors.put(fieldPath, fieldPath.substring(fieldPath.lastIndexOf('.') + 1) + " is required");
            }
            return;
        }
        if (!matchesAlphaNum(normalized)) {
            String fieldName = fieldPath.contains(".")
                    ? fieldPath.substring(fieldPath.lastIndexOf('.') + 1)
                    : fieldPath;
            errors.put(fieldPath, fieldName + " is invalid");
        }
    }

    public static void validateSpecResultFormat(
            String value,
            Double minValue,
            Double maxValue,
            String fieldPath,
            String fieldName,
            boolean required,
            Map<String, String> errors) {
        String normalized = normalizeOptionalText(value);
        String errorKey = fieldPath + "." + fieldName;

        if (normalized == null) {
            if (required) {
                errors.put(errorKey, fieldName + " is required");
            }
            return;
        }

        if (isReferenceRangeNotApplicable(minValue, maxValue)) {
            if (!matchesAlphaNum(normalized)) {
                errors.put(errorKey, fieldName + " is invalid");
            }
            return;
        }

        if (!isParseableNumeric(normalized)) {
            errors.put(errorKey, fieldName + " must be numeric");
        }
    }

    public static void validateSpecResultFormat(
            String value,
            MaterialSpecification masterSpec,
            String fieldPath,
            String fieldName,
            boolean required,
            Map<String, String> errors) {
        ReferenceRange referenceRange = masterSpec != null ? masterSpec.getReferenceRange() : null;
        Double minValue = referenceRange != null ? referenceRange.getMinValue() : null;
        Double maxValue = referenceRange != null ? referenceRange.getMaxValue() : null;
        validateSpecResultFormat(value, minValue, maxValue, fieldPath, fieldName, required, errors);
    }

    /** Persist: strip commas for numeric ranges; keep text for N/A. */
    public static String normalizeForPersist(String value, Double minValue, Double maxValue) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        if (isReferenceRangeNotApplicable(minValue, maxValue)) {
            return normalized;
        }
        return normalized.replace(",", "");
    }

    public static boolean isOutOfRange(String analysedResult, Double minValue, Double maxValue) {
        if (isReferenceRangeNotApplicable(minValue, maxValue)) {
            return false;
        }
        if (isBlank(analysedResult)) {
            return true;
        }
        try {
            double value = Double.parseDouble(analysedResult.replace(",", "").trim());
            if (minValue != null && value < minValue) {
                return true;
            }
            if (maxValue != null && value > maxValue) {
                return true;
            }
            return false;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    /** True when a display specification label implies a numeric range or bound. */
    public static boolean specificationLabelImpliesNumeric(String label) {
        if (isBlank(label)) {
            return false;
        }
        String trimmed = label.trim();
        return SPEC_RANGE_LABEL_PATTERN.matcher(trimmed).find()
                || SPEC_BOUND_LABEL_PATTERN.matcher(trimmed).find();
    }
}
