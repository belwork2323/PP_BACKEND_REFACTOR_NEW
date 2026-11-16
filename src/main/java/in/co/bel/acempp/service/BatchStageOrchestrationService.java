package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.dto.qcdivision.QcStageDivisionStatusDto;
import in.co.bel.acempp.dto.subscale.SubScaleMetadata;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.service.batch.MainBatchStageConstants;
import in.co.bel.acempp.service.batch.MainBatchStageProgressHelper;
import in.co.bel.acempp.service.batch.MainBatchUnlockService;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionStageStatusResolver;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionCatalog;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class BatchStageOrchestrationService {

    private static final int MAX_SAVE_ATTEMPTS = 3;
    private static final long STATIC_TESTING_SUB_DEPARTMENT_ID = 12L;

    private final BatchRepository batchRepository;
    private final BatchStageSequence stageSequence;
    private final QcDivisionStageStatusResolver qcDivisionStatusResolver;
    private final SubscaleMetadataResolver subscaleMetadataResolver;
    private final MainBatchUnlockService mainBatchUnlockService;

    @Data
    @Builder
    public static class StageUpdate {
        private Status status;
        private List<PremixStatusResponse> premixStatuses;
        private List<PremixStatusResponse> finalMixStatuses;
        private List<MotorStatusResponse> motorStatuses;
        private List<QcStageDivisionStatusDto> divisionStatuses;
        private PersonInfo submittedBy;
        private LocalDateTime submittedOn;
        private PersonInfo approvedBy;
        private LocalDateTime approvedOn;
        private String remarks;
        private String rejectionReason;
        private boolean approvedUnitEvent;
    }

    public Batch.StageProgress createInitialStage(Batch batch, Long subDepartmentId, LocalDateTime now) {
        Batch.StageProgress stage = createStage(batch, subDepartmentId, now);
        batch.getStageProgress().add(stage);
        rebuildCurrentStage(batch);
        return stage;
    }

    public void createInitialStages(Batch batch, List<Long> subDepartmentIds, LocalDateTime now) {
        if (batch.getStageProgress() == null) {
            batch.setStageProgress(new ArrayList<>());
        }
        for (Long subDepartmentId : subDepartmentIds) {
            boolean exists = batch.getStageProgress().stream()
                    .anyMatch(stage -> Objects.equals(stage.getSubDepartmentId(), subDepartmentId));
            if (!exists) {
                batch.getStageProgress().add(createStage(batch, subDepartmentId, now));
            }
        }
        rebuildCurrentStage(batch);
    }

    public void synchronizeStage(String batchId, Long subDepartmentId, StageUpdate update) {
        mutateWithRetry(batchId, batch ->
                applyStageUpdate(batch, subDepartmentId, update));
    }

    /**
     * Re-apply STF/QC/Dispatch unlock rules that may have been skipped (e.g. QC division 12
     * missing from older batches after STATIC_TEST_FACILITY was added to master).
     */
    public void reconcileStaleUnlocks(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            return;
        }
        mutateWithRetry(batchId, batch -> {
            if (MainBatchStageProgressHelper.isParallelMainFlow(batch)
                    || "SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
                mainBatchUnlockService.reconcileStfMotorUnlocks(batch, LocalDateTime.now());
                rebuildCurrentStage(batch);
            }
        });
    }

    void applyStageUpdate(Batch batch, Long subDepartmentId, StageUpdate update) {
        Batch.StageProgress stage = findStage(batch, subDepartmentId);
        applyUpdate(stage, update);
        LocalDateTime now = LocalDateTime.now();

        if (MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            if (update.isApprovedUnitEvent()) {
                mainBatchUnlockService.evaluateUnlockRules(
                        batch,
                        subDepartmentId,
                        update.getPremixStatuses(),
                        update.getFinalMixStatuses(),
                        update.getMotorStatuses(),
                        now);
            }
            // Mixing / Subscale / QC batch-level completions unlock Subscale / STF / WEIGHTMENT+QC.
            if ((Objects.equals(subDepartmentId, MainBatchStageConstants.MIXING)
                    || Objects.equals(subDepartmentId, MainBatchStageConstants.SUBSCALE)
                    || Objects.equals(subDepartmentId, MainBatchStageConstants.QC))
                    && update.getStatus() == Status.COMPLETELY_APPROVED) {
                mainBatchUnlockService.evaluateUnlockRules(
                        batch,
                        subDepartmentId,
                        null,
                        null,
                        null,
                        now);
            }
        } else if ("SUBSCALE".equalsIgnoreCase(batch.getBatchType())
                && (Objects.equals(subDepartmentId, MainBatchStageConstants.QC)
                || Objects.equals(subDepartmentId, MainBatchStageConstants.SUBSCALE))
                && (update.isApprovedUnitEvent()
                || update.getStatus() == Status.COMPLETELY_APPROVED)) {
            mainBatchUnlockService.evaluateUnlockRules(
                    batch,
                    subDepartmentId,
                    update.getPremixStatuses(),
                    update.getFinalMixStatuses(),
                    update.getMotorStatuses(),
                    now);
        } else if (update.getStatus() == Status.COMPLETELY_APPROVED) {
            unlockNextStageIfAbsent(batch, subDepartmentId, now);
        }

        ensureStfMotorStatusesSeeded(batch);
        // Re-apply STF unlock when NDT + subscale readiness is already true (stale YET_TO_START rows).
        if (MainBatchStageProgressHelper.isParallelMainFlow(batch)
                || "SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
            mainBatchUnlockService.reconcileStfMotorUnlocks(batch, now);
        }
        rebuildCurrentStage(batch);
    }

    public boolean isStageUnlockedAndActive(Batch batch, Long subDepartmentId) {
        if (batch == null || batch.getStageProgress() == null) {
            return false;
        }
        return batch.getStageProgress().stream()
                .anyMatch(stage -> Objects.equals(stage.getSubDepartmentId(), subDepartmentId)
                        && !isTerminal(stage.getStatus()));
    }

    public void assertStageUnlockedAndActive(Batch batch, Long subDepartmentId, String stageName) {
        if (!isStageUnlockedAndActive(batch, subDepartmentId)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Batch is not active in " + stageName);
        }
    }

    public void rebuildCurrentStage(Batch batch) {
        List<Batch.StageProgress> activeStages = batch.getStageProgress() == null
                ? List.of()
                : batch.getStageProgress().stream()
                .filter(stage -> !isTerminal(stage.getStatus()))
                .map(this::copyStage)
                .toList();
        batch.setCurrentStage(new ArrayList<>(activeStages));
    }

    public boolean isTerminal(Status status) {
        return status == Status.COMPLETELY_APPROVED;
    }

    private void mutateWithRetry(String batchId, Consumer<Batch> mutation) {
        OptimisticLockingFailureException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_SAVE_ATTEMPTS; attempt++) {
            Batch batch = batchRepository.findByBatchIdAndIsDeletedFalse(batchId)
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.BATCH_NOT_FOUND,
                            "Batch not found: " + batchId));
            mutation.accept(batch);
            try {
                batchRepository.save(batch);
                return;
            } catch (OptimisticLockingFailureException failure) {
                lastFailure = failure;
            }
        }
        throw lastFailure;
    }

    private void applyUpdate(Batch.StageProgress stage, StageUpdate update) {
        LocalDateTime now = LocalDateTime.now();
        if (update.getStatus() != null) {
            stage.setStatus(update.getStatus());
        }
        if (update.getPremixStatuses() != null) {
            stage.setPremixStatuses(copyPremixStatuses(update.getPremixStatuses()));
        }
        if (update.getFinalMixStatuses() != null) {
            stage.setFinalMixStatuses(copyPremixStatuses(update.getFinalMixStatuses()));
        }
        if (update.getMotorStatuses() != null) {
            stage.setMotorStatuses(copyMotorStatuses(update.getMotorStatuses()));
        }
        if (update.getDivisionStatuses() != null) {
            stage.setDivisionStatuses(copyDivisionStatuses(update.getDivisionStatuses()));
        }
        if (update.getSubmittedBy() != null) {
            stage.setSubmittedBy(update.getSubmittedBy());
            stage.setSubmittedOn(update.getSubmittedOn() != null ? update.getSubmittedOn() : now);
        }
        if (update.getApprovedBy() != null) {
            stage.setApprovedBy(update.getApprovedBy());
            stage.setApprovedOn(update.getApprovedOn() != null ? update.getApprovedOn() : now);
        }
        stage.setRemarks(update.getRemarks());
        stage.setRejectionReason(update.getRejectionReason());
        stage.setUpdatedAt(now);
    }

    private Batch.StageProgress findStage(Batch batch, Long subDepartmentId) {
        return batch.getStageProgress().stream()
                .filter(stage -> Objects.equals(stage.getSubDepartmentId(), subDepartmentId))
                .findFirst()
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_STATE,
                        "Subdepartment " + subDepartmentId + " is not unlocked for batch " + batch.getBatchId()));
    }

    private void unlockNextStageIfAbsent(Batch batch, Long sourceSubDepartmentId, LocalDateTime now) {
        Long nextSubDepartmentId = stageSequence.getNextStage(sourceSubDepartmentId, batch.getBatchType());
        if (nextSubDepartmentId == null) {
            return;
        }
        boolean alreadyUnlocked = batch.getStageProgress().stream()
                .anyMatch(stage -> Objects.equals(stage.getSubDepartmentId(), nextSubDepartmentId));
        if (!alreadyUnlocked) {
            batch.getStageProgress().add(createStage(batch, nextSubDepartmentId, now));
        }
    }

    private Batch.StageProgress createStage(Batch batch, Long subDepartmentId, LocalDateTime now) {
        String[] metadata = stageSequence.getMeta(subDepartmentId);
        Batch.StageProgress.StageProgressBuilder builder = Batch.StageProgress.builder()
                .departmentId(stageSequence.getDepartmentId(subDepartmentId))
                .departmentName(metadata[0])
                .subDepartmentId(subDepartmentId)
                .subDepartmentName(metadata[1])
                .status(Status.TO_BE_INITIATED)
                .enteredAt(now)
                .updatedAt(now);

        int premixCount = batch.getIdentificationSheet() != null
                && batch.getIdentificationSheet().getNumberOfPremix() != null
                ? batch.getIdentificationSheet().getNumberOfPremix()
                : 0;

        if (subDepartmentId == 3L) {
            builder.premixStatuses(initialPremixStatuses(premixCount, StageType.PREMIX));
        } else if (subDepartmentId == 5L) {
            builder.premixStatuses(initialPremixStatuses(premixCount, StageType.PREMIX));
            builder.finalMixStatuses(initialPremixStatuses(premixCount, StageType.FINAL_MIX));
        } else if (List.of(4L, 6L, 7L, 9L, 10L, 13L).contains(subDepartmentId)) {
            builder.motorStatuses(initialMotorStatuses(batch.getMotorIds()));
        } else if (subDepartmentId == STATIC_TESTING_SUB_DEPARTMENT_ID) {
            builder.motorStatuses(initialStfMotorStatuses(batch));
        } else if (subDepartmentId == QcDivisionCatalog.SUB_DEPARTMENT_ID) {
            builder.divisionStatuses(qcDivisionStatusResolver.initialStatuses(batch));
        }
        return builder.build();
    }

    private void ensureStfMotorStatusesSeeded(Batch batch) {
        if (batch.getStageProgress() == null) {
            return;
        }
        batch.getStageProgress().stream()
                .filter(stage -> Objects.equals(
                        stage.getSubDepartmentId(), STATIC_TESTING_SUB_DEPARTMENT_ID))
                .filter(stage -> stage.getMotorStatuses() == null
                        || stage.getMotorStatuses().isEmpty())
                .forEach(stage -> stage.setMotorStatuses(initialStfMotorStatuses(batch)));
    }

    List<MotorStatusResponse> initialStfMotorStatuses(Batch batch) {
        List<MotorStatusResponse> statuses = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        Optional.ofNullable(batch.getMotorIds())
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .filter(motorId -> !motorId.isBlank())
                .forEach(motorId -> {
                    if (seen.add(motorId)) {
                        statuses.add(MotorStatusResponse.builder()
                                .motorId(motorId)
                                .subType(StfMotorSubType.MAIN_MOTOR.name())
                                .motorSubmissionType(Status.DRAFT)
                                .motorSubmissionStatus(Status.TO_BE_INITIATED)
                                .build());
                    }
                });

        SubScaleMetadata metadata = resolveBemMetadataForStf(batch);
        subscaleMetadataResolver.toBemMotorIds(metadata).forEach(bemMotorId -> {
            if (seen.add(bemMotorId)) {
                statuses.add(MotorStatusResponse.builder()
                        .motorId(bemMotorId)
                        .subType(StfMotorSubType.BEM.name())
                        .motorSubmissionType(Status.DRAFT)
                        .motorSubmissionStatus(Status.TO_BE_INITIATED)
                        .build());
            }
        });
        return statuses;
    }

    /** Prefer approved-form resolver; fall back to persisted ID-sheet metadata.subScale. */
    private SubScaleMetadata resolveBemMetadataForStf(Batch batch) {
        SubScaleMetadata metadata =
                subscaleMetadataResolver.resolveApprovedMetadata(batch.getBatchId());
        if (metadata != null
                && metadata.getBemMotors() != null
                && !metadata.getBemMotors().isEmpty()) {
            return metadata;
        }
        if (batch.getIdentificationSheet() != null
                && batch.getIdentificationSheet().getMetadata() != null
                && batch.getIdentificationSheet().getMetadata().getSubScale() != null) {
            return batch.getIdentificationSheet().getMetadata().getSubScale();
        }
        return metadata;
    }

    private List<PremixStatusResponse> initialPremixStatuses(int count, StageType stageType) {
        List<PremixStatusResponse> statuses = new ArrayList<>(count);
        for (int premixNo = 1; premixNo <= count; premixNo++) {
            statuses.add(PremixStatusResponse.builder()
                    .premixNo(premixNo)
                    .stageType(stageType)
                    .premixSubmissionType(Status.DRAFT)
                    .premixSubmissionStatus(Status.TO_BE_INITIATED)
                    .build());
        }
        return statuses;
    }

    private List<MotorStatusResponse> initialMotorStatuses(List<String> motorIds) {
        if (motorIds == null) {
            return new ArrayList<>();
        }
        return motorIds.stream()
                .map(motorId -> MotorStatusResponse.builder()
                        .motorId(motorId)
                        .subType(StfMotorSubType.MAIN_MOTOR.name())
                        .motorSubmissionType(Status.DRAFT)
                        .motorSubmissionStatus(Status.TO_BE_INITIATED)
                        .build())
                .toList();
    }

    private Batch.StageProgress copyStage(Batch.StageProgress source) {
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
                .premixStatuses(copyPremixStatuses(source.getPremixStatuses()))
                .finalMixStatuses(copyPremixStatuses(source.getFinalMixStatuses()))
                .motorStatuses(copyMotorStatuses(source.getMotorStatuses()))
                .divisionStatuses(copyDivisionStatuses(source.getDivisionStatuses()))
                .build();
    }

    private List<PremixStatusResponse> copyPremixStatuses(List<PremixStatusResponse> source) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(status -> PremixStatusResponse.builder()
                        .premixNo(status.getPremixNo())
                        .division(status.getDivision())
                        .subType(status.getSubType())
                        .stageType(status.getStageType())
                        .premixSubmissionType(status.getPremixSubmissionType())
                        .premixSubmissionStatus(status.getPremixSubmissionStatus())
                        .build())
                .toList();
    }

    private List<MotorStatusResponse> copyMotorStatuses(List<MotorStatusResponse> source) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(status -> MotorStatusResponse.builder()
                        .motorId(status.getMotorId())
                        .division(status.getDivision())
                        .subType(status.getSubType())
                        .motorSubmissionType(status.getMotorSubmissionType())
                        .motorSubmissionStatus(status.getMotorSubmissionStatus())
                        .build())
                .toList();
    }

    private List<QcStageDivisionStatusDto> copyDivisionStatuses(
            List<QcStageDivisionStatusDto> source) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(status -> QcStageDivisionStatusDto.builder()
                        .divisionId(status.getDivisionId())
                        .divisionSubmissionType(status.getDivisionSubmissionType())
                        .status(status.getStatus())
                        .divisionName(status.getDivisionName())
                        .premixStatuses(copyPremixStatuses(status.getPremixStatuses()))
                        .finalMixStatuses(copyPremixStatuses(status.getFinalMixStatuses()))
                        .motorStatuses(copyMotorStatuses(status.getMotorStatuses()))
                        .build())
                .toList();
    }
}
