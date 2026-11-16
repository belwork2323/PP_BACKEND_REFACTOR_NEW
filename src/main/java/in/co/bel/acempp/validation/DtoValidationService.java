package in.co.bel.acempp.validation;

import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemValidationException;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DtoValidationService {

    private final Validator validator;

    public <T> void validateDto(T dto, Long subDepartmentId) {
        Status submissionType = resolveSubmissionType(dto, subDepartmentId);
        validateDto(dto, submissionType, SubmitGroup.class);
    }

    public <T> void validateDto(T dto, Status submissionType) {
        validateDto(dto, submissionType, SubmitGroup.class);
    }

    public <T> void validateDto(T dto, Status submissionType, Class<?>... submitGroups) {
        if (dto == null) return;

        Set<ConstraintViolation<T>> violations;

        if (submissionType != null && submissionType.isSubmit()) {
            Class<?>[] groups = new Class<?>[submitGroups.length + 1];
            groups[0] = DraftGroup.class;
            System.arraycopy(submitGroups, 0, groups, 1, submitGroups.length);
            violations = validator.validate(dto, groups);
        } else {
            violations = validator.validate(dto, DraftGroup.class);
        }

        if (!violations.isEmpty()) {
            Map<String, Object> errorTree = buildErrorTree(violations);

            // Pass the structured map into your custom exception
            throw new AcemValidationException("Validation failed", errorTree);
        }
    }

    private <T> Map<String, Object> buildErrorTree(Set<ConstraintViolation<T>> violations) {
        Map<String, Object> root = new LinkedHashMap<>();

        for (ConstraintViolation<T> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();

            String[] parts = propertyPath.split("\\.");
            Map<String, Object> currentLevel = root;

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];

                if (i == parts.length - 1) {
                    // Leaf field -> Assign error message string
                    currentLevel.put(part, errorMessage);
                } else {
                    // Check if segment has array index like "radiographyObservations[0]"
                    if (part.contains("[")) {
                        String fieldName = part.substring(0, part.indexOf('['));
                        int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) currentLevel.computeIfAbsent(
                                fieldName, k -> new ArrayList<>()
                        );

                        while (list.size() <= index) {
                            list.add(new LinkedHashMap<>());
                        }

                        currentLevel = list.get(index);
                    } else {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nextLevel = (Map<String, Object>) currentLevel.computeIfAbsent(
                                part, k -> new LinkedHashMap<>()
                        );
                        currentLevel = nextLevel;
                    }
                }
            }
        }
        return root;
    }

    public Status resolveSubmissionType(Object dto, Long subDepartmentId) {
        if (dto == null || subDepartmentId == null) return null;

        try {
            switch (subDepartmentId.intValue()) {
                case 1:
                    return getTopLevelStatus(dto, "getSubmissionType");

                case 2:
                case 8:
                    return getTopLevelStatus(dto, "getFormSubmissionType");

                case 3:
                    return getNestedPremixStatus(dto);

                case 4:
                    return getCaseMotorStatus(dto);

                case 5:
                    return getMixingStagePremixStatus(dto);

                case 6:
                case 7:
                case 9:
                case 10:
                case 12:
                case 13:
                    return getMotorsSubmissionStatus(dto);

                case 11:
                    return getDivisionSubmissionStatus(dto);

                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Status getTopLevelStatus(Object dto, String methodName) {
        Object val = invokeGetter(dto, methodName);
        return val != null ? parseStatus(val) : null;
    }

    private Status getNestedPremixStatus(Object dto) {
        try {
            Object prepDetails = invokeGetter(dto, "getPreparationDetails");
            if (prepDetails == null) return null;
            List<?> premixes = (List<?>) invokeGetter(prepDetails, "getPremixes");
            if (premixes != null && !premixes.isEmpty()) {
                Object val = invokeGetter(premixes.get(0), "getPremixSubmissionType");
                return val != null ? parseStatus(val) : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Status getCaseMotorStatus(Object dto) {
        try {
            Object caseDetails = invokeGetter(dto, "getCasePreparationDetails");
            if (caseDetails == null) return null;
            List<?> motors = (List<?>) invokeGetter(caseDetails, "getMotors");
            if (motors != null && !motors.isEmpty()) {
                Object val = invokeGetter(motors.get(0), "getMotorSubmissionType");
                return val != null ? parseStatus(val) : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Status getMixingStagePremixStatus(Object dto) {
        try {
            Object mixingDetails = invokeGetter(dto, "getMixingDetails");
            if (mixingDetails == null) return null;
            List<?> stages = (List<?>) invokeGetter(mixingDetails, "getStages");
            if (stages != null) {
                for (Object stage : stages) {
                    List<?> premixes = (List<?>) invokeGetter(stage, "getPremixes");
                    if (premixes != null && !premixes.isEmpty()) {
                        Object val = invokeGetter(premixes.get(0), "getPremixSubmissionType");
                        if (val != null) return parseStatus(val);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Status getMotorsSubmissionStatus(Object dto) {
        try {
            List<?> motors = (List<?>) invokeGetter(dto, "getMotors");
            if (motors != null && !motors.isEmpty()) {
                Object val = invokeGetter(motors.get(0), "getMotorSubmissionType");
                return val != null ? parseStatus(val) : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Status getDivisionSubmissionStatus(Object dto) {
        try {
            List<?> divisions = (List<?>) invokeGetter(dto, "getDivisionDetails");
            if (divisions != null && !divisions.isEmpty()) {
                Object val = invokeGetter(divisions.get(0), "getDivisionSubmissionType");
                return val != null ? parseStatus(val) : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Object invokeGetter(Object obj, String methodName) {
        if (obj == null) return null;
        try {
            Method method = obj.getClass().getMethod(methodName);
            return method.invoke(obj);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke " + methodName, e);
        }
    }

    private Status parseStatus(Object val) {
        if (val instanceof Status) {
            return (Status) val;
        }
        if (val != null) {
            return Status.valueOf(val.toString());
        }
        return null;
    }
}