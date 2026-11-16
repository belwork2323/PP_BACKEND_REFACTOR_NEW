package in.co.bel.acempp.service.batch;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BatchMotorDiscardHelper {

    private BatchMotorDiscardHelper() {
    }

    public static boolean isDiscarded(Batch batch, String motorId) {
        if (batch == null || motorId == null || motorId.isBlank()) {
            return false;
        }
        String wanted = motorId.trim();
        List<Batch.DiscardedMotor> discarded = batch.getDiscardedMotors();
        if (discarded != null) {
            for (Batch.DiscardedMotor entry : discarded) {
                if (entry == null || entry.getMotorId() == null) {
                    continue;
                }
                if (wanted.equalsIgnoreCase(entry.getMotorId().trim())) {
                    return true;
                }
            }
        }
        // Fallback: already stamped DISCARDED on any stage/division motorStatuses
        return hasDiscardedStatusInStages(batch.getCurrentStage(), wanted)
                || hasDiscardedStatusInStages(batch.getStageProgress(), wanted);
    }

    private static boolean hasDiscardedStatusInStages(List<Batch.StageProgress> stages, String motorId) {
        if (stages == null || stages.isEmpty()) {
            return false;
        }
        for (Batch.StageProgress stage : stages) {
            if (stage == null) {
                continue;
            }
            if (hasDiscardedMotorStatus(stage.getMotorStatuses(), motorId)) {
                return true;
            }
            if (stage.getDivisionStatuses() != null) {
                for (var division : stage.getDivisionStatuses()) {
                    if (division != null
                            && hasDiscardedMotorStatus(division.getMotorStatuses(), motorId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasDiscardedMotorStatus(List<MotorStatusResponse> statuses, String motorId) {
        if (statuses == null) {
            return false;
        }
        for (MotorStatusResponse status : statuses) {
            if (status == null || status.getMotorId() == null) {
                continue;
            }
            if (motorId.equalsIgnoreCase(status.getMotorId().trim())
                    && status.getMotorSubmissionStatus() == Status.DISCARDED) {
                return true;
            }
        }
        return false;
    }

    public static List<String> discardedMotorIds(Batch batch) {
        List<String> ids = new ArrayList<>();
        if (batch == null) {
            return ids;
        }
        if (batch.getMotorIds() != null) {
            for (String motorId : batch.getMotorIds()) {
                if (motorId != null && isDiscarded(batch, motorId.trim())) {
                    ids.add(motorId.trim());
                }
            }
        }
        if (batch.getDiscardedMotors() != null) {
            for (Batch.DiscardedMotor entry : batch.getDiscardedMotors()) {
                if (entry == null || entry.getMotorId() == null || entry.getMotorId().isBlank()) {
                    continue;
                }
                String id = entry.getMotorId().trim();
                boolean present = ids.stream().anyMatch(existing -> existing.equalsIgnoreCase(id));
                if (!present) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public static Batch.DiscardedMotor findDiscarded(Batch batch, String motorId) {
        if (batch == null || motorId == null || motorId.isBlank()) {
            return null;
        }
        String wanted = motorId.trim();
        List<Batch.DiscardedMotor> discarded = batch.getDiscardedMotors();
        if (discarded == null) {
            return null;
        }
        for (Batch.DiscardedMotor entry : discarded) {
            if (entry == null || entry.getMotorId() == null) {
                continue;
            }
            if (wanted.equalsIgnoreCase(entry.getMotorId().trim())) {
                return entry;
            }
        }
        return null;
    }

    public static void assertNotDiscarded(Batch batch, String motorId) {
        if (isDiscarded(batch, motorId)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Motor " + motorId + " has been discarded and cannot be edited");
        }
    }

    /**
     * Record discard audit on the batch and stamp every matching motorStatuses row
     * in currentStage + stageProgress (and QC divisionStatuses) to DISCARDED.
     */
    public static void markDiscarded(
            Batch batch,
            String motorId,
            String reason,
            PersonInfo discardedBy,
            LocalDateTime discardedAt) {
        if (batch == null || motorId == null || motorId.isBlank()) {
            return;
        }
        String normalizedId = motorId.trim();
        List<Batch.DiscardedMotor> discarded = batch.getDiscardedMotors();
        if (discarded == null) {
            discarded = new ArrayList<>();
            batch.setDiscardedMotors(discarded);
        }

        Batch.DiscardedMotor existing = findDiscarded(batch, normalizedId);
        if (existing != null) {
            existing.setReason(reason);
            existing.setDiscardedAt(discardedAt);
            existing.setDiscardedBy(discardedBy);
        } else {
            discarded.add(Batch.DiscardedMotor.builder()
                    .motorId(normalizedId)
                    .reason(reason)
                    .discardedAt(discardedAt)
                    .discardedBy(discardedBy)
                    .build());
        }

        stampStages(batch.getCurrentStage(), normalizedId);
        stampStages(batch.getStageProgress(), normalizedId);
    }

    private static void stampStages(List<Batch.StageProgress> stages, String motorId) {
        if (stages == null) {
            return;
        }
        for (Batch.StageProgress stage : stages) {
            if (stage == null) {
                continue;
            }
            stampMotorStatuses(stage.getMotorStatuses(), motorId);
            if (stage.getDivisionStatuses() != null) {
                for (var division : stage.getDivisionStatuses()) {
                    if (division == null) {
                        continue;
                    }
                    stampMotorStatuses(division.getMotorStatuses(), motorId);
                }
            }
        }
    }

    private static void stampMotorStatuses(List<MotorStatusResponse> statuses, String motorId) {
        if (statuses == null) {
            return;
        }
        for (MotorStatusResponse status : statuses) {
            if (status == null || status.getMotorId() == null) {
                continue;
            }
            if (motorId.equalsIgnoreCase(status.getMotorId().trim())) {
                status.setMotorSubmissionStatus(Status.DISCARDED);
                if (status.getMotorSubmissionType() == null) {
                    status.setMotorSubmissionType(Status.DRAFT);
                }
            }
        }
    }

    public static void applyDiscardOverlay(Batch batch, MotorStatusResponse status) {
        if (batch == null || status == null || status.getMotorId() == null) {
            return;
        }
        if (!isDiscarded(batch, status.getMotorId())) {
            return;
        }
        status.setMotorSubmissionStatus(Status.DISCARDED);
        if (status.getMotorSubmissionType() == null) {
            status.setMotorSubmissionType(Status.DRAFT);
        }
        status.setLocked(true);
    }

    public static List<MotorStatusResponse> overlayMotorStatuses(
            Batch batch, List<MotorStatusResponse> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return statuses;
        }
        for (MotorStatusResponse status : statuses) {
            applyDiscardOverlay(batch, status);
        }
        // Ensure discarded motors appear even when missing from the stage list
        if (batch == null || batch.getDiscardedMotors() == null) {
            return statuses;
        }
        for (Batch.DiscardedMotor discarded : batch.getDiscardedMotors()) {
            if (discarded == null || discarded.getMotorId() == null || discarded.getMotorId().isBlank()) {
                continue;
            }
            String motorId = discarded.getMotorId().trim();
            boolean present = statuses.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(s -> s.getMotorId() != null
                            && motorId.equalsIgnoreCase(s.getMotorId().trim()));
            if (!present) {
                // Only add if this motor belongs to the batch
                if (batch.getMotorIds() != null
                        && batch.getMotorIds().stream()
                        .anyMatch(id -> id != null && motorId.equalsIgnoreCase(id.trim()))) {
                    statuses.add(MotorStatusResponse.builder()
                            .motorId(motorId)
                            .motorSubmissionType(Status.DRAFT)
                            .motorSubmissionStatus(Status.DISCARDED)
                            .locked(true)
                            .build());
                }
            }
        }
        return statuses;
    }

    public static boolean isDiscardedStatus(Status status) {
        return status == Status.DISCARDED;
    }

    public static boolean isDiscardedStatusName(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return "DISCARDED".equals(status.trim().toUpperCase(Locale.ROOT));
    }
}
