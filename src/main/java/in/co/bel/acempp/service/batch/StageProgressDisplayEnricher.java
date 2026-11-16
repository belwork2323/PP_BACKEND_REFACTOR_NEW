package in.co.bel.acempp.service.batch;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class StageProgressDisplayEnricher {

    public void enrichBatchViews(
            Batch batch,
            List<Batch.StageProgress> stageProgress,
            List<Batch.StageProgress> currentStage) {
        enrich(batch, stageProgress);
        enrich(batch, currentStage);
    }

    public List<Batch.StageProgress> enrichCopies(Batch batch, List<Batch.StageProgress> stages) {
        if (stages == null || stages.isEmpty()) {
            return Collections.emptyList();
        }
        List<Batch.StageProgress> copies = new ArrayList<>(stages.size());
        for (Batch.StageProgress stage : stages) {
            copies.add(copyStage(stage));
        }
        enrich(batch, copies);
        return copies;
    }

    public void enrich(Batch batch, List<Batch.StageProgress> stages) {
        if (stages == null || stages.isEmpty()) {
            return;
        }
        for (Batch.StageProgress stage : stages) {
            enrichStage(batch, stage);
        }
    }

    private void enrichStage(Batch batch, Batch.StageProgress stage) {
        if (stage == null) {
            return;
        }
        Long subDepartmentId = stage.getSubDepartmentId();
        if (stage.getPremixStatuses() != null) {
            stage.setPremixStatuses(enrichPremixStatuses(
                    batch, subDepartmentId, stage.getPremixStatuses()));
        }
        if (stage.getFinalMixStatuses() != null) {
            stage.setFinalMixStatuses(enrichFinalMixStatuses(
                    batch, subDepartmentId, stage.getFinalMixStatuses()));
        }
        if (stage.getMotorStatuses() != null) {
            stage.setMotorStatuses(enrichMotorStatuses(
                    batch, subDepartmentId, stage.getMotorStatuses()));
        }
        if (stage.getDivisionStatuses() != null) {
            for (var division : stage.getDivisionStatuses()) {
                if (division == null || division.getMotorStatuses() == null) {
                    continue;
                }
                division.setMotorStatuses(enrichMotorStatuses(
                        batch, subDepartmentId, division.getMotorStatuses()));
            }
        }
    }

    private List<PremixStatusResponse> enrichPremixStatuses(
            Batch batch,
            Long subDepartmentId,
            List<PremixStatusResponse> statuses) {
        List<PremixStatusResponse> enriched = new ArrayList<>(statuses.size());
        for (PremixStatusResponse status : statuses) {
            if (status == null) {
                continue;
            }
            enriched.add(PremixStatusResponse.builder()
                    .premixNo(status.getPremixNo())
                    .division(status.getDivision())
                    .subType(status.getSubType())
                    .stageType(status.getStageType())
                    .premixSubmissionType(status.getPremixSubmissionType())
                    .premixSubmissionStatus(status.getPremixSubmissionStatus())
                    .locked(subDepartmentId == null
                            ? MainBatchStageProgressHelper.isUnitLocked(
                                    batch, status.getPremixSubmissionStatus())
                            : MainBatchStageProgressHelper.isPremixUnitLocked(
                                    batch,
                                    subDepartmentId,
                                    status.getPremixNo(),
                                    status.getStageType(),
                                    status.getPremixSubmissionStatus()))
                    .build());
        }
        return enriched;
    }

    private List<PremixStatusResponse> enrichFinalMixStatuses(
            Batch batch,
            Long subDepartmentId,
            List<PremixStatusResponse> statuses) {
        List<PremixStatusResponse> enriched = new ArrayList<>(statuses.size());
        for (PremixStatusResponse status : statuses) {
            if (status == null) {
                continue;
            }
            enriched.add(PremixStatusResponse.builder()
                    .premixNo(status.getPremixNo())
                    .division(status.getDivision())
                    .subType(status.getSubType())
                    .stageType(status.getStageType())
                    .premixSubmissionType(status.getPremixSubmissionType())
                    .premixSubmissionStatus(status.getPremixSubmissionStatus())
                    .locked(subDepartmentId == null
                            ? MainBatchStageProgressHelper.isUnitLocked(
                                    batch, status.getPremixSubmissionStatus())
                            : MainBatchStageProgressHelper.isPremixUnitLocked(
                                    batch,
                                    subDepartmentId,
                                    status.getPremixNo(),
                                    StageType.FINAL_MIX,
                                    status.getPremixSubmissionStatus()))
                    .build());
        }
        return enriched;
    }

    private List<MotorStatusResponse> enrichMotorStatuses(
            Batch batch,
            Long subDepartmentId,
            List<MotorStatusResponse> statuses) {
        List<MotorStatusResponse> enriched = new ArrayList<>(statuses.size());
        for (MotorStatusResponse status : statuses) {
            if (status == null) {
                continue;
            }
            Status submissionStatus = status.getMotorSubmissionStatus();
            Status submissionType = status.getMotorSubmissionType();
            boolean discarded = BatchMotorDiscardHelper.isDiscarded(batch, status.getMotorId());
            // Display-heal STF: upstream NDT + subscale ready but row still YET_TO_START.
            if (!discarded
                    && Objects.equals(subDepartmentId, MainBatchStageConstants.STF)
                    && (submissionStatus == null || submissionStatus == Status.YET_TO_START)
                    && MainBatchStageProgressHelper.isMotorUnlockedByUpstream(
                            batch, MainBatchStageConstants.STF, status.getMotorId())) {
                submissionStatus = Status.TO_BE_INITIATED;
                if (submissionType == null) {
                    submissionType = Status.DRAFT;
                }
            }
            if (discarded) {
                submissionStatus = Status.DISCARDED;
                if (submissionType == null) {
                    submissionType = Status.DRAFT;
                }
            }
            enriched.add(MotorStatusResponse.builder()
                    .motorId(status.getMotorId())
                    .division(status.getDivision())
                    .subType(status.getSubType())
                    .motorSubmissionType(submissionType)
                    .motorSubmissionStatus(submissionStatus)
                    .locked(discarded
                            || (subDepartmentId == null
                            ? MainBatchStageProgressHelper.isUnitLocked(
                                    batch, submissionStatus)
                            : MainBatchStageProgressHelper.isMotorUnitLocked(
                                    batch,
                                    subDepartmentId,
                                    status.getMotorId(),
                                    submissionStatus)))
                    .build());
        }
        return BatchMotorDiscardHelper.overlayMotorStatuses(batch, enriched);
    }

    private Batch.StageProgress copyStage(Batch.StageProgress source) {
        if (source == null) {
            return null;
        }
        return Batch.StageProgress.builder()
                .departmentId(source.getDepartmentId())
                .departmentName(source.getDepartmentName())
                .subDepartmentId(source.getSubDepartmentId())
                .subDepartmentName(source.getSubDepartmentName())
                .status(source.getStatus())
                .enteredAt(source.getEnteredAt())
                .updatedAt(source.getUpdatedAt())
                .remarks(source.getRemarks())
                .rejectionReason(source.getRejectionReason())
                .submittedBy(source.getSubmittedBy())
                .submittedOn(source.getSubmittedOn())
                .approvedBy(source.getApprovedBy())
                .approvedOn(source.getApprovedOn())
                .premixStatuses(source.getPremixStatuses())
                .finalMixStatuses(source.getFinalMixStatuses())
                .motorStatuses(source.getMotorStatuses())
                .divisionStatuses(source.getDivisionStatuses())
                .build();
    }
}
