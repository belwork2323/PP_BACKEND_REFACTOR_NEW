package in.co.bel.acempp.validation.custom;

import in.co.bel.acempp.dto.postcureoperations.PostCureMotor;
import in.co.bel.acempp.dto.postcureoperations.PostCureRequest;
import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads.ApplicationDetail;
import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads.InhibitionDetails;
import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads.MixDetails;
import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads.MixIngredient;
import in.co.bel.acempp.enums.PostCureInhibitorType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemValidationException;
import in.co.bel.acempp.validation.DtoValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ValidatePostCureMotor {

    private final DtoValidationService dtoValidationService;

    public void validateMotors(PostCureRequest request) {
        if (request.getMotors() == null) return;

        for (int index = 0; index < request.getMotors().size(); index++) {
            PostCureMotor motor = request.getMotors().get(index);
            if (motor == null) continue;

            validateMotorPayload(index, motor);

            try {
                dtoValidationService.validateDto(motor, motor.getMotorSubmissionType());
            } catch (AcemValidationException exception) {
                throw motorValidationError(index, exception.getErrors());
            }

            if (motor.getMotorSubmissionType() == Status.SUBMIT) {
                validateMotorSubmitRules(index, motor);
            }
        }
    }

    private void validateMotorPayload(int index, PostCureMotor motor) {
        PostCureInhibitorType inhibitorType = motor.getInhibitorType();
        InhibitionDetails inhibitionDetails = motor.getInhibitionDetails();

        if (inhibitorType == PostCureInhibitorType.NOT_APPLICABLE) {
            if (inhibitionDetails == null) {
                return;
            }
            rejectIfMixSectionsPresent(index, inhibitionDetails);
            return;
        }

        if (inhibitionDetails == null) {
            return;
        }

        if (inhibitionDetails.getNotApplicableRemarks() != null
                && !inhibitionDetails.getNotApplicableRemarks().isBlank()) {
            throw motorFieldError(index, "inhibitionDetails.notApplicableRemarks",
                    "notApplicableRemarks is allowed only for NOT_APPLICABLE");
        }
    }

    private void validateMotorSubmitRules(int index, PostCureMotor motor) {
        if (motor.getLooseFlapFillingDetails() == null) {
            throw motorFieldError(index, "looseFlapFillingDetails",
                    "looseFlapFillingDetails is required for submission");
        }

        PostCureInhibitorType inhibitorType = motor.getInhibitorType();
        if (inhibitorType == null) {
            throw motorFieldError(index, "inhibitorType", "inhibitorType is required for submission");
        }

        if (inhibitorType == PostCureInhibitorType.NOT_APPLICABLE) {
            InhibitionDetails details = motor.getInhibitionDetails();
            if (details == null) {
                throw motorFieldError(index, "inhibitionDetails",
                        "inhibitionDetails is required for NOT_APPLICABLE submission");
            }
            rejectIfMixSectionsPresent(index, details);
            if (details.getNotApplicableRemarks() == null
                    || details.getNotApplicableRemarks().isBlank()) {
                throw motorFieldError(index, "inhibitionDetails.notApplicableRemarks",
                        "notApplicableRemarks is required for NOT_APPLICABLE submission");
            }
            return;
        }

        if (motor.getInhibitionDetails() == null) {
            throw motorFieldError(index, "inhibitionDetails",
                    "inhibitionDetails is required for submission");
        }

        if (inhibitorType == PostCureInhibitorType.OTHERS) {
            requireOthersSections(index, motor.getInhibitionDetails());
        }
    }

    /**
     * OTHERS requires premix + final mix (flexible ingredients) plus qualification,
     * inhibitor batch, application, and dispatch details.
     */
    private void requireOthersSections(int index, InhibitionDetails details) {
        requireMixDetails(index, "inhibitionDetails.premixDetails", details.getPremixDetails());
        requireMixDetails(index, "inhibitionDetails.finalMixDetails", details.getFinalMixDetails());

        if (details.getQualificationDetails() == null) {
            throw motorFieldError(index, "inhibitionDetails.qualificationDetails",
                    "qualificationDetails is required for OTHERS submission");
        }
        if (details.getInhibitorBatchDetails() == null) {
            throw motorFieldError(index, "inhibitionDetails.inhibitorBatchDetails",
                    "inhibitorBatchDetails is required for OTHERS submission");
        }
        if (details.getApplicationDetails() == null || details.getApplicationDetails().isEmpty()) {
            throw motorFieldError(index, "inhibitionDetails.applicationDetails",
                    "applicationDetails is required for OTHERS submission");
        }
        for (int i = 0; i < details.getApplicationDetails().size(); i++) {
            ApplicationDetail row = details.getApplicationDetails().get(i);
            if (row == null) {
                throw motorFieldError(index, "inhibitionDetails.applicationDetails[" + i + "]",
                        "application detail row is required");
            }
        }
        if (details.getDispatchDetails() == null) {
            throw motorFieldError(index, "inhibitionDetails.dispatchDetails",
                    "dispatchDetails is required for OTHERS submission");
        }
    }

    private void requireMixDetails(int index, String path, MixDetails mix) {
        if (mix == null) {
            throw motorFieldError(index, path, path + " is required for OTHERS submission");
        }
        if (mix.getIngredients() == null || mix.getIngredients().isEmpty()) {
            throw motorFieldError(index, path + ".ingredients",
                    "ingredients are required for OTHERS submission");
        }
        boolean hasNonTotalRow = false;
        for (int i = 0; i < mix.getIngredients().size(); i++) {
            MixIngredient ingredient = mix.getIngredients().get(i);
            if (ingredient == null) {
                throw motorFieldError(index, path + ".ingredients[" + i + "]",
                        "ingredient row is required");
            }
            if ("TOTAL".equalsIgnoreCase(ingredient.getRowKey())) {
                continue;
            }
            hasNonTotalRow = true;
            if (ingredient.getIngredient() == null || ingredient.getIngredient().isBlank()) {
                throw motorFieldError(index, path + ".ingredients[" + i + "].ingredient",
                        "ingredient is required");
            }
        }
        if (!hasNonTotalRow) {
            throw motorFieldError(index, path + ".ingredients",
                    "at least one ingredient row is required for OTHERS submission");
        }
    }

    /** N/A may only carry inhibitorType + notApplicableRemarks. */
    private void rejectIfMixSectionsPresent(int index, InhibitionDetails details) {
        if (details.getPremixDetails() != null) {
            throw motorFieldError(index, "inhibitionDetails.premixDetails",
                    "premixDetails is not allowed for NOT_APPLICABLE");
        }
        if (details.getFinalMixDetails() != null) {
            throw motorFieldError(index, "inhibitionDetails.finalMixDetails",
                    "finalMixDetails is not allowed for NOT_APPLICABLE");
        }
        if (details.getQualificationDetails() != null) {
            throw motorFieldError(index, "inhibitionDetails.qualificationDetails",
                    "qualificationDetails is not allowed for NOT_APPLICABLE");
        }
        if (details.getInhibitorBatchDetails() != null) {
            throw motorFieldError(index, "inhibitionDetails.inhibitorBatchDetails",
                    "inhibitorBatchDetails is not allowed for NOT_APPLICABLE");
        }
        if (details.getApplicationDetails() != null
                && !details.getApplicationDetails().isEmpty()) {
            throw motorFieldError(index, "inhibitionDetails.applicationDetails",
                    "applicationDetails is not allowed for NOT_APPLICABLE");
        }
        if (details.getDispatchDetails() != null) {
            throw motorFieldError(index, "inhibitionDetails.dispatchDetails",
                    "dispatchDetails is not allowed for NOT_APPLICABLE");
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
        root.put("motors", motors);
        return new AcemValidationException("Validation failed", root);
    }
}
