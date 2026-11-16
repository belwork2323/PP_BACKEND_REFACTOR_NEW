package in.co.bel.acempp.service.batch;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.Status;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Shared motor lifecycle checks for Motor Management stats and batch motor selection.
 */
public final class MotorBatchLifecycleHelper {

    private MotorBatchLifecycleHelper() {
    }

    /** Motor approved (or completely approved) on the Dispatch sub-department. */
    public static boolean isDispatched(Batch batch, String motorId) {
        if (batch == null || isBlank(motorId)) {
            return false;
        }
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.DISPATCH)
                .map(Batch.StageProgress::getMotorStatuses)
                .filter(Objects::nonNull)
                .map(statuses -> statuses.stream()
                        .anyMatch(status -> motorMatches(status, motorId) && isApprovedStatus(status.getMotorSubmissionStatus())))
                .orElse(false);
    }

    /**
     * Motor has started work on this batch.
     * <ul>
     *   <li>MAIN: motor left TO_BE_INITIATED / YET_TO_START in a motor stage (e.g. Case Prep → IN_PROGRESS).</li>
     *   <li>SUBSCALE: motors do not go through RMP; if the Subscale sub-department stage status
     *       is past TO_BE_INITIATED / YET_TO_START (e.g. IN_PROGRESS), the motor is in progress.</li>
     * </ul>
     */
    public static boolean hasStarted(Batch batch, String motorId) {
        if (batch == null || isBlank(motorId)) {
            return false;
        }
        if ("SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
            return isSubscaleStageStarted(batch);
        }
        if (batch.getStageProgress() == null) {
            return false;
        }
        for (Batch.StageProgress stage : batch.getStageProgress()) {
            if (stage == null || stage.getSubDepartmentId() == null) {
                continue;
            }
            long subDept = stage.getSubDepartmentId();
            if (subDept == MainBatchStageConstants.DISPATCH) {
                continue;
            }
            if (!MainBatchStageConstants.MOTOR_STAGES.contains(subDept)) {
                continue;
            }
            List<MotorStatusResponse> statuses = stage.getMotorStatuses();
            if (statuses == null) {
                continue;
            }
            for (MotorStatusResponse status : statuses) {
                if (!motorMatches(status, motorId)) {
                    continue;
                }
                if (isPastInitiated(status.getMotorSubmissionStatus())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Subscale batch stage (subDept 8) has moved past not-started. */
    private static boolean isSubscaleStageStarted(Batch batch) {
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.SUBSCALE)
                .map(Batch.StageProgress::getStatus)
                .map(MotorBatchLifecycleHelper::isPastInitiated)
                .orElse(false);
    }

    private static boolean isPastInitiated(Status status) {
        return status != null
                && status != Status.TO_BE_INITIATED
                && status != Status.YET_TO_START;
    }

    public static boolean isUnavailableForSelection(Batch batch, String motorId) {
        return BatchMotorDiscardHelper.isDiscarded(batch, motorId) || isDispatched(batch, motorId);
    }

    /**
     * Collect motor IDs that are discarded or dispatched on any of the given batches
     * (case-preserving first-seen form of the id).
     */
    public static Set<String> collectUnavailableMotorIds(Collection<Batch> batches) {
        Set<String> unavailable = new HashSet<>();
        Set<String> seenLower = new HashSet<>();
        if (batches == null) {
            return unavailable;
        }
        for (Batch batch : batches) {
            if (batch == null) {
                continue;
            }
            if (batch.getDiscardedMotors() != null) {
                for (Batch.DiscardedMotor entry : batch.getDiscardedMotors()) {
                    if (entry == null || isBlank(entry.getMotorId())) {
                        continue;
                    }
                    addIfNew(unavailable, seenLower, entry.getMotorId().trim());
                }
            }
            if (batch.getMotorIds() == null) {
                continue;
            }
            for (String rawId : batch.getMotorIds()) {
                if (isBlank(rawId)) {
                    continue;
                }
                String motorId = rawId.trim();
                if (seenLower.contains(motorId.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (isUnavailableForSelection(batch, motorId)) {
                    addIfNew(unavailable, seenLower, motorId);
                }
            }
        }
        return unavailable;
    }

    public static boolean containsIgnoreCase(Set<String> ids, String motorId) {
        if (ids == null || isBlank(motorId)) {
            return false;
        }
        String needle = motorId.trim().toLowerCase(Locale.ROOT);
        for (String id : ids) {
            if (id != null && id.trim().toLowerCase(Locale.ROOT).equals(needle)) {
                return true;
            }
        }
        return false;
    }

    private static void addIfNew(Set<String> unavailable, Set<String> seenLower, String motorId) {
        String lower = motorId.toLowerCase(Locale.ROOT);
        if (seenLower.add(lower)) {
            unavailable.add(motorId);
        }
    }

    private static boolean motorMatches(MotorStatusResponse status, String motorId) {
        if (status == null || status.getMotorId() == null) {
            return false;
        }
        return motorId.equalsIgnoreCase(String.valueOf(status.getMotorId()).trim());
    }

    private static boolean isApprovedStatus(Status status) {
        return status == Status.APPROVED || status == Status.COMPLETELY_APPROVED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
