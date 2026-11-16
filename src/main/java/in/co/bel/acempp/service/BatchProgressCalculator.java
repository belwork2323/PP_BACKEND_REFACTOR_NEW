package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BatchProgressCalculator {

    private static final double IN_PROGRESS_WEIGHT = 0.50;
    private static final double WAITING_FOR_APPROVAL_WEIGHT = 0.80;
    private static final double FINAL_APPROVAL_PENDING_CAP = 0.90;

    private final BatchStageSequence stageSequence;

    public int calculate(Batch batch) {
        if (batch == null) {
            return 0;
        }
        boolean parallelMainFlow = batch.getParallelFlowEnabled() != null
                && batch.getParallelFlowEnabled()
                && batch.getBatchType() != null
                && !"SUBSCALE".equalsIgnoreCase(batch.getBatchType());
        return calculate(
                batch.getStageProgress(),
                stageSequence.getProgressStages(batch.getBatchType(), parallelMainFlow));
    }

    public int calculate(
            List<Batch.StageProgress> stageProgress,
            Collection<Long> applicableStageIds) {

        if (applicableStageIds == null || applicableStageIds.isEmpty()) {
            return 0;
        }

        List<Batch.StageProgress> stages = stageProgress == null
                ? Collections.emptyList()
                : stageProgress;

        double completedStageUnits = applicableStageIds.stream()
                .map(stageId -> stages.stream()
                        .filter(stage -> Objects.equals(stage.getSubDepartmentId(), stageId))
                        .findFirst()
                        .map(this::calculateStageFraction)
                        .orElse(0.0))
                .mapToDouble(Double::doubleValue)
                .sum();

        return clampPercentage((int) Math.round(
                completedStageUnits * 100.0 / applicableStageIds.size()));
    }

    public int calculateStagePercentage(Batch.StageProgress stage) {
        return clampPercentage((int) Math.round(calculateStageFraction(stage) * 100.0));
    }

    private double calculateStageFraction(Batch.StageProgress stage) {
        if (stage == null || stage.getStatus() == null) {
            return 0.0;
        }
        if (isCompleted(stage.getStatus())) {
            return 1.0;
        }

        double unitProgress = calculateUnitProgress(stage);
        if (unitProgress >= 0.0) {
            return Math.min(unitProgress, FINAL_APPROVAL_PENDING_CAP);
        }

        return switch (stage.getStatus()) {
            case IN_PROGRESS, DRAFT -> IN_PROGRESS_WEIGHT;
            case WAITING_FOR_APPROVAL, WAITING_FOR_PARTIAL_APPROVAL ->
                    WAITING_FOR_APPROVAL_WEIGHT;
            default -> 0.0;
        };
    }

    private double calculateUnitProgress(Batch.StageProgress stage) {
        double earned = 0.0;
        int total = 0;

        if (stage.getPremixStatuses() != null) {
            for (PremixStatusResponse status : stage.getPremixStatuses()) {
                if (status != null) {
                    earned += unitWeight(status.getPremixSubmissionStatus());
                    total++;
                }
            }
        }
        if (stage.getFinalMixStatuses() != null) {
            for (PremixStatusResponse status : stage.getFinalMixStatuses()) {
                if (status != null) {
                    earned += unitWeight(status.getPremixSubmissionStatus());
                    total++;
                }
            }
        }
        if (stage.getMotorStatuses() != null) {
            for (MotorStatusResponse status : stage.getMotorStatuses()) {
                if (status != null) {
                    earned += unitWeight(status.getMotorSubmissionStatus());
                    total++;
                }
            }
        }

        return total == 0 ? -1.0 : earned / total;
    }

    private double unitWeight(Status status) {
        if (status == null) {
            return 0.0;
        }
        return switch (status) {
            case APPROVED, COMPLETED, COMPLETELY_APPROVED -> 1.0;
            case WAITING_FOR_APPROVAL, WAITING_FOR_PARTIAL_APPROVAL
                  -> WAITING_FOR_APPROVAL_WEIGHT;
            case IN_PROGRESS, DRAFT -> IN_PROGRESS_WEIGHT;
            default -> 0.0;
        };
    }

    private boolean isCompleted(Status status) {
        return status == Status.COMPLETELY_APPROVED
                || status == Status.APPROVED
                || status == Status.COMPLETED;
    }

    private int clampPercentage(int percentage) {
        return Math.max(0, Math.min(100, percentage));
    }
}
