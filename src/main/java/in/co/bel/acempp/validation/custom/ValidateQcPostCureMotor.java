package in.co.bel.acempp.validation.custom;

import in.co.bel.acempp.enums.PostCureInhibitorType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight dual-section SUBMIT rules for QC Post Cure motors
 * ({@code data.postCureMotorDetails[]}).
 */
@Component
public class ValidateQcPostCureMotor {

    public void validateMotors(List<Map<String, Object>> motors) {
        if (motors == null || motors.isEmpty()) {
            return;
        }
        for (int index = 0; index < motors.size(); index++) {
            Map<String, Object> motor = motors.get(index);
            if (motor == null) {
                continue;
            }
            Status submissionType = readStatus(motor.get("motorSubmissionType"));
            if (submissionType != Status.SUBMIT) {
                continue;
            }
            validateMotorSubmitRules(index, motor);
        }
    }

    private void validateMotorSubmitRules(int index, Map<String, Object> motor) {
        if (motor.get("looseFlapFillingDetails") == null) {
            throw motorFieldError(index, "looseFlapFillingDetails",
                    "looseFlapFillingDetails is required for submission");
        }

        PostCureInhibitorType inhibitorType = readInhibitorType(motor.get("inhibitorType"));
        Object inhibition = motor.get("inhibitionDetails");
        if (inhibitorType == null && inhibition instanceof Map<?, ?> inhibitionMap) {
            inhibitorType = readInhibitorType(inhibitionMap.get("inhibitorType"));
        }
        if (inhibitorType == null) {
            throw motorFieldError(index, "inhibitorType",
                    "inhibitorType is required for submission");
        }

        if (!(inhibition instanceof Map<?, ?> inhibitionMap)) {
            throw motorFieldError(index, "inhibitionDetails",
                    "inhibitionDetails is required for submission");
        }

        if (inhibitorType == PostCureInhibitorType.NOT_APPLICABLE) {
            rejectIfNaMixSectionsPresent(index, inhibitionMap);
            Object remarks = inhibitionMap.get("notApplicableRemarks");
            if (remarks == null || String.valueOf(remarks).trim().isEmpty()) {
                throw motorFieldError(index, "inhibitionDetails.notApplicableRemarks",
                        "notApplicableRemarks is required for NOT_APPLICABLE submission");
            }
        }
    }

    private void rejectIfNaMixSectionsPresent(int index, Map<?, ?> details) {
        if (details.get("premixDetails") != null
                || details.get("finalMixDetails") != null
                || details.get("qualificationDetails") != null
                || details.get("inhibitorBatchDetails") != null
                || details.get("dispatchDetails") != null
                || isNonEmptyList(details.get("applicationDetails"))) {
            throw motorFieldError(index, "inhibitionDetails",
                    "NOT_APPLICABLE may only include notApplicableRemarks on inhibitionDetails");
        }
    }

    private static boolean isNonEmptyList(Object value) {
        return value instanceof List<?> list && !list.isEmpty();
    }

    private PostCureInhibitorType readInhibitorType(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof PostCureInhibitorType typed) {
            return typed;
        }
        String raw = String.valueOf(value).trim().toUpperCase().replace('-', '_');
        if (raw.isEmpty() || "NULL".equals(raw)) {
            return null;
        }
        try {
            return PostCureInhibitorType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            if ("HEMCOAT3K".equals(raw) || "HEMCOAT_3K".equals(raw)) {
                return PostCureInhibitorType.HEMCOAT_3K;
            }
            if ("IR_1".equals(raw)) {
                return PostCureInhibitorType.IR1;
            }
            if ("OTHER".equals(raw) || "OTHERS".equals(raw)) {
                return PostCureInhibitorType.OTHERS;
            }
            if ("NOTAPPLICABLE".equals(raw) || "NA".equals(raw) || "N_A".equals(raw)) {
                return PostCureInhibitorType.NOT_APPLICABLE;
            }
            return null;
        }
    }

    private Status readStatus(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Status status) {
            return status;
        }
        try {
            return Status.valueOf(String.valueOf(value).trim().toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    private AcemValidationException motorFieldError(int index, String field, String message) {
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put(field, message);
        return motorValidationError(index, errors);
    }

    private AcemValidationException motorValidationError(
            int index,
            Map<String, Object> motorErrors) {
        List<Map<String, Object>> motors = new ArrayList<>();
        for (int position = 0; position <= index; position++) {
            motors.add(new LinkedHashMap<>());
        }
        motors.set(index, motorErrors);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("postCureMotorDetails", motors);
        return new AcemValidationException("Validation failed", root);
    }
}
