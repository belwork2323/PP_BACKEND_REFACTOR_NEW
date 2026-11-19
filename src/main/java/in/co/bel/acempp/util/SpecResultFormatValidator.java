package in.co.bel.acempp.util;

import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.validation.ValidationConstants;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared format rules for specification result values (analysed / ACEM QC)
 * and RMS text / decimal field limits.
 */
public final class SpecResultFormatValidator {

    /** Legacy letters, numbers, spaces, hyphens, underscores, or slashes. */
    public static final Pattern ALPHA_NUM_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 /_-]*$");

    public static final Pattern SHORT_TEXT_PATTERN =
            Pattern.compile(ValidationConstants.SHORT_TEXT_PATTERN);

    public static final Pattern CODE_REF_PATTERN =
            Pattern.compile(ValidationConstants.CODE_REF_PATTERN);

    public static final Pattern REMARKS_PATTERN =
            Pattern.compile(ValidationConstants.REMARKS_PATTERN);

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

    public static boolean matchesShortText(String value) {
        return value != null && SHORT_TEXT_PATTERN.matcher(value).matches();
    }

    public static boolean matchesCodeRef(String value) {
        return value != null && CODE_REF_PATTERN.matcher(value).matches();
    }

    public static boolean matchesRemarks(String value) {
        return value != null && REMARKS_PATTERN.matcher(value).matches();
    }

    private static String fieldLabel(String fieldPath) {
        return fieldPath.contains(".")
                ? fieldPath.substring(fieldPath.lastIndexOf('.') + 1)
                : fieldPath;
    }

    /**
     * Validate optional text IDs (legacy ALPHA_NUM). Prefer category helpers for RMS.
     */
    public static void validateAlphaNumField(
            String value,
            String fieldPath,
            boolean required,
            Map<String, String> errors) {
        validateTextField(
                value,
                fieldPath,
                required,
                1,
                Integer.MAX_VALUE,
                ALPHA_NUM_PATTERN,
                errors);
    }

    public static void validateShortTextField(
            String value,
            String fieldPath,
            boolean required,
            Map<String, String> errors) {
        validateTextField(
                value,
                fieldPath,
                required,
                ValidationConstants.SHORT_TEXT_MIN,
                ValidationConstants.SHORT_TEXT_MAX,
                SHORT_TEXT_PATTERN,
                errors);
    }

    public static void validateCodeRefField(
            String value,
            String fieldPath,
            boolean required,
            Map<String, String> errors) {
        validateTextField(
                value,
                fieldPath,
                required,
                ValidationConstants.CODE_REF_MIN,
                ValidationConstants.CODE_REF_MAX,
                CODE_REF_PATTERN,
                errors);
    }

    public static void validateRemarksField(
            String value,
            String fieldPath,
            boolean required,
            Map<String, String> errors) {
        validateTextField(
                value,
                fieldPath,
                required,
                ValidationConstants.REMARKS_MIN,
                ValidationConstants.REMARKS_MAX,
                REMARKS_PATTERN,
                errors);
    }

    public static void validateTextField(
            String value,
            String fieldPath,
            boolean required,
            int minLength,
            int maxLength,
            Pattern pattern,
            Map<String, String> errors) {
        String normalized = normalizeOptionalText(value);
        String label = fieldLabel(fieldPath);
        if (normalized == null) {
            if (required) {
                errors.put(fieldPath, label + " is required");
            }
            return;
        }
        if (normalized.length() < minLength) {
            errors.put(fieldPath, label + " must be at least " + minLength + " characters");
            return;
        }
        if (normalized.length() > maxLength) {
            errors.put(fieldPath, label + " cannot exceed " + maxLength + " characters");
            return;
        }
        if (pattern != null && !pattern.matcher(normalized).matches()) {
            errors.put(fieldPath, label + " is invalid");
        }
    }

    /** Validate a decimal string: parseable, length, and ≤6 fraction digits. */
    public static void validateDecimalString(
            String value,
            String fieldPath,
            String fieldName,
            boolean required,
            Map<String, String> errors) {
        String normalized = normalizeOptionalText(value);
        String errorKey = fieldPath.contains(".") && fieldPath.endsWith("." + fieldName)
                ? fieldPath
                : (isBlank(fieldPath) ? fieldName : fieldPath + "." + fieldName);
        String label = fieldName != null && !fieldName.isBlank() ? fieldName : fieldLabel(errorKey);

        if (normalized == null) {
            if (required) {
                errors.put(errorKey, label + " is required");
            }
            return;
        }

        String compact = normalized.replace(",", "");
        int maxLen = compact.startsWith("-")
                ? ValidationConstants.DECIMAL_MAX_LENGTH_SIGNED
                : ValidationConstants.DECIMAL_MAX_LENGTH;
        if (compact.length() > maxLen) {
            errors.put(errorKey, label + " cannot exceed " + maxLen + " characters");
            return;
        }
        if (!isParseableNumeric(compact)) {
            errors.put(errorKey, label + " must be numeric");
            return;
        }
        int dot = compact.indexOf('.');
        if (dot >= 0) {
            int places = compact.length() - dot - 1;
            if (places > ValidationConstants.DECIMAL_MAX_PLACES) {
                errors.put(errorKey,
                        label + " cannot have more than "
                                + ValidationConstants.DECIMAL_MAX_PLACES
                                + " decimal places");
            }
        }
    }

    public static void validateDecimalNumber(
            Double value,
            String fieldPath,
            boolean required,
            Map<String, String> errors) {
        if (value == null) {
            if (required) {
                errors.put(fieldPath, fieldLabel(fieldPath) + " is required");
            }
            return;
        }
        if (!Double.isFinite(value)) {
            errors.put(fieldPath, fieldLabel(fieldPath) + " must be numeric");
            return;
        }
        String asText = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        validateDecimalString(asText, fieldPath, fieldLabel(fieldPath), true, errors);
    }

    /** Quantity fields: decimal format plus value must be greater than zero. */
    public static void validatePositiveDecimalNumber(
            Double value,
            String fieldPath,
            boolean required,
            Map<String, String> errors) {
        if (value == null) {
            if (required) {
                errors.put(fieldPath, fieldLabel(fieldPath) + " is required");
            }
            return;
        }
        if (!Double.isFinite(value)) {
            errors.put(fieldPath, fieldLabel(fieldPath) + " must be numeric");
            return;
        }
        if (value <= 0) {
            errors.put(fieldPath, fieldLabel(fieldPath) + " must be greater than 0");
            return;
        }
        String asText = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        validateDecimalString(asText, fieldPath, fieldLabel(fieldPath), true, errors);
    }

    /** Quantity fields stored as exact decimal strings (avoids Double rounding). */
    public static void validatePositiveDecimalString(
            String value,
            String fieldPath,
            boolean required,
            Map<String, String> errors) {
        String label = fieldLabel(fieldPath);
        validateDecimalString(value, fieldPath, label, required, errors);
        if (errors.containsKey(fieldPath)) {
            return;
        }
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return;
        }
        try {
            BigDecimal bd = new BigDecimal(normalized.replace(",", ""));
            if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                errors.put(fieldPath, label + " must be greater than 0");
            }
        } catch (NumberFormatException ex) {
            errors.put(fieldPath, label + " must be numeric");
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
            if (normalized.length() > ValidationConstants.CODE_REF_MAX) {
                errors.put(errorKey, fieldName + " cannot exceed "
                        + ValidationConstants.CODE_REF_MAX + " characters");
                return;
            }
            if (!matchesCodeRef(normalized)) {
                errors.put(errorKey, fieldName + " is invalid");
            }
            return;
        }

        validateDecimalString(normalized, fieldPath, fieldName, required, errors);
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
