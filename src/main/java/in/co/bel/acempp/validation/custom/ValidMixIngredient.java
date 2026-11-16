package in.co.bel.acempp.validation.custom;

import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads;
import in.co.bel.acempp.validation.ValidationConstants;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.*;
import java.math.BigDecimal;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MixIngredientValidator.class)
@Documented
public @interface ValidMixIngredient {
    String message() default "Invalid mix ingredient details";

    Class<?>[] groups() default {};

    Class<?>[] payload() default {};
}

class MixIngredientValidator implements ConstraintValidator<ValidMixIngredient, PostCureSectionPayloads.MixIngredient> {

    @Override
    public boolean isValid(PostCureSectionPayloads.MixIngredient ingredient, ConstraintValidatorContext context) {
        if (ingredient == null) {
            return true;
        }

        // Bypass validation entirely for the TOTAL summary row
        if ("TOTAL".equalsIgnoreCase(ingredient.getRowKey())) {
            return true;
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        // Validate ingredient
        if (ingredient.getIngredient() == null || ingredient.getIngredient().trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("ingredient is required")
                    .addPropertyNode("ingredient")
                    .addConstraintViolation();
            isValid = false;
        } else if (ingredient.getIngredient().length() > ValidationConstants.STANDARD_TEXT_MAX) {
            context.buildConstraintViolationWithTemplate("ingredient cannot exceed 100 characters")
                    .addPropertyNode("ingredient")
                    .addConstraintViolation();
            isValid = false;
        }

        // Validate mfgLot
        if (ingredient.getMfgLot() == null || ingredient.getMfgLot().trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("mfgLot is required")
                    .addPropertyNode("mfgLot")
                    .addConstraintViolation();
            isValid = false;
        }

        // Validate partsByWeight (supports tolerances like "60 ±1")
        if (ingredient.getPartsByWeight() == null || ingredient.getPartsByWeight().trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("partsByWeight is required")
                    .addPropertyNode("partsByWeight")
                    .addConstraintViolation();
            isValid = false;
        }

        // Validate quantityTaken
        if (ingredient.getQuantityTaken() == null) {
            context.buildConstraintViolationWithTemplate("quantityTaken is required")
                    .addPropertyNode("quantityTaken")
                    .addConstraintViolation();
            isValid = false;
        } else if (ingredient.getQuantityTaken().compareTo(new BigDecimal("0.000001")) < 0) {
            context.buildConstraintViolationWithTemplate("quantityTaken must be greater than 0")
                    .addPropertyNode("quantityTaken")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}