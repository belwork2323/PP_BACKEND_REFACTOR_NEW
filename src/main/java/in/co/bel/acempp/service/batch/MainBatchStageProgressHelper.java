package in.co.bel.acempp.service.batch;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionCatalog;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MainBatchStageProgressHelper {

    private MainBatchStageProgressHelper() {
    }

    public static Optional<Batch.StageProgress> findStage(Batch batch, long subDepartmentId) {
        if (batch.getStageProgress() == null) {
            return Optional.empty();
        }
        return batch.getStageProgress().stream()
                .filter(stage -> Objects.equals(stage.getSubDepartmentId(), subDepartmentId))
                .findFirst();
    }

    public static boolean isParallelMainFlow(Batch batch) {
        return batch != null
                && Boolean.TRUE.equals(batch.getParallelFlowEnabled())
                && MainBatchStageConstants.isMainBatch(batch.getBatchType());
    }

    public static boolean isPremixApproved(Batch batch, int premixNo) {
        return findStage(batch, MainBatchStageConstants.RMP)
                .map(Batch.StageProgress::getPremixStatuses)
                .filter(statuses -> statuses != null)
                .map(statuses -> statuses.stream()
                        .filter(status -> status.getPremixNo() == premixNo)
                        .anyMatch(status -> status.getPremixSubmissionStatus() == Status.APPROVED))
                .orElse(false);
    }

    /** Manufacturing Mixing Premix APPROVED for this premix number. */
    public static boolean isMixingPremixApproved(Batch batch, int premixNo) {
        return findStage(batch, MainBatchStageConstants.MIXING)
                .map(Batch.StageProgress::getPremixStatuses)
                .filter(statuses -> statuses != null)
                .map(statuses -> statuses.stream()
                        .filter(status -> status.getPremixNo() == premixNo)
                        .anyMatch(status -> status.getPremixSubmissionStatus() == Status.APPROVED))
                .orElse(false);
    }

    /** Manufacturing Mixing Final Mix APPROVED for this premix number. */
    public static boolean isMixingFinalMixApproved(Batch batch, int premixNo) {
        return findStage(batch, MainBatchStageConstants.MIXING)
                .map(Batch.StageProgress::getFinalMixStatuses)
                .filter(statuses -> statuses != null)
                .map(statuses -> statuses.stream()
                        .filter(status -> status.getPremixNo() == premixNo)
                        .anyMatch(status -> status.getPremixSubmissionStatus() == Status.APPROVED))
                .orElse(false);
    }

    public static boolean areAllPremixesApproved(Batch batch, long subDepartmentId) {
        return findStage(batch, subDepartmentId)
                .map(stage -> {
                    List<PremixStatusResponse> statuses = stage.getPremixStatuses();
                    if (statuses == null || statuses.isEmpty()) {
                        return true;
                    }
                    return statuses.stream()
                            .allMatch(status -> status.getPremixSubmissionStatus() == Status.APPROVED);
                })
                .orElse(false);
    }

    public static boolean areAllFinalMixesApproved(Batch batch) {
        return findStage(batch, MainBatchStageConstants.MIXING)
                .map(stage -> {
                    List<PremixStatusResponse> statuses = stage.getFinalMixStatuses();
                    if (statuses == null || statuses.isEmpty()) {
                        return true;
                    }
                    return statuses.stream()
                            .allMatch(status -> status.getPremixSubmissionStatus() == Status.APPROVED);
                })
                .orElse(false);
    }

    public static boolean isMotorApproved(Batch batch, long subDepartmentId, String motorId) {
        return findStage(batch, subDepartmentId)
                .map(Batch.StageProgress::getMotorStatuses)
                .filter(statuses -> statuses != null)
                .map(statuses -> statuses.stream()
                        .filter(status -> Objects.equals(status.getMotorId(), motorId))
                        .anyMatch(status -> status.getMotorSubmissionStatus() == Status.APPROVED))
                .orElse(false);
    }

    public static boolean isSubscaleCompletelyApproved(Batch batch) {
        return findStage(batch, MainBatchStageConstants.SUBSCALE)
                .map(stage -> stage.getStatus() == Status.COMPLETELY_APPROVED)
                .orElse(false);
    }

    public static boolean hasApprovedBemMotor(Batch batch) {
        return findStage(batch, MainBatchStageConstants.STF)
                .map(Batch.StageProgress::getMotorStatuses)
                .filter(statuses -> statuses != null)
                .map(statuses -> statuses.stream()
                        .filter(status -> StfMotorSubType.BEM.name().equals(status.getSubType()))
                        .anyMatch(status -> status.getMotorSubmissionStatus() == Status.APPROVED))
                .orElse(false);
    }

    public static boolean isPremixUnlockedByUpstream(
            Batch batch,
            long subDepartmentId,
            int premixNo,
            StageType stageType) {
        if (!isParallelMainFlow(batch)) {
            return true;
        }
        if (subDepartmentId == MainBatchStageConstants.RMP
                || subDepartmentId == MainBatchStageConstants.CASE_PREP) {
            return true;
        }
        if (subDepartmentId == MainBatchStageConstants.MIXING) {
            return isPremixApproved(batch, premixNo);
        }
        return true;
    }

    public static boolean isMotorUnlockedByUpstream(Batch batch, long subDepartmentId, String motorId) {
        if (!isParallelMainFlow(batch)) {
            return true;
        }
        if (subDepartmentId == MainBatchStageConstants.RMP
                || subDepartmentId == MainBatchStageConstants.CASE_PREP) {
            return true;
        }
        if (subDepartmentId == MainBatchStageConstants.CASTING_CURING) {
            return isMotorApproved(batch, MainBatchStageConstants.CASE_PREP, motorId);
        }
        if (subDepartmentId == MainBatchStageConstants.POST_CURE
                || subDepartmentId == MainBatchStageConstants.TRIMMING) {
            return isMotorApproved(batch, MainBatchStageConstants.CASTING_CURING, motorId);
        }
        if (subDepartmentId == MainBatchStageConstants.NDT) {
            return isMotorApproved(batch, MainBatchStageConstants.POST_CURE, motorId)
                    && isMotorApproved(batch, MainBatchStageConstants.TRIMMING, motorId);
        }
        if (subDepartmentId == MainBatchStageConstants.QC) {
            // QC stage is available from create; motor unlock is per-division unit, not stage-level.
            return QcDivisionProgressHelper.isDivisionUnlocked(
                    batch, QcDivisionCatalog.RAW_MATERIAL_REVALIDATION);
        }
        if (subDepartmentId == MainBatchStageConstants.STF) {
            return isStfMotorUnlockedByUpstream(batch, motorId);
        }
        if (subDepartmentId == MainBatchStageConstants.DISPATCH) {
            return QcDivisionProgressHelper.isMotorReadyForDispatch(batch, motorId);
        }
        return true;
    }

    /**
     * STF unlock (no QC gate):
     * - BEM: subscale COMPLETELY_APPROVED
     * - MAIN on MAIN batch: subscale + manufacturing NDT APPROVED for that motor
     * - MAIN on QUALIFICATION subscale: subscale only (no NDT path)
     */
    private static boolean isStfMotorUnlockedByUpstream(Batch batch, String motorId) {
        if (!isSubscaleCompletelyApproved(batch)) {
            return false;
        }
        Optional<MotorStatusResponse> stfMotor = findMotorInStage(
                batch, MainBatchStageConstants.STF, motorId);
        if (stfMotor.isPresent()
                && StfMotorSubType.BEM.name().equals(stfMotor.get().getSubType())) {
            return true;
        }
        if (QcDivisionProgressHelper.isQualificationSubscale(batch)
                || QcDivisionProgressHelper.isExperimentalSubscale(batch)) {
            return true;
        }
        return isMotorApproved(batch, MainBatchStageConstants.NDT, motorId);
    }

    private static boolean isMainMotorApprovedInStf(Batch batch, String motorId) {
        return findStage(batch, MainBatchStageConstants.STF)
                .map(Batch.StageProgress::getMotorStatuses)
                .filter(statuses -> statuses != null)
                .map(statuses -> statuses.stream()
                        .filter(status -> Objects.equals(status.getMotorId(), motorId))
                        .filter(status -> StfMotorSubType.MAIN_MOTOR.name().equals(status.getSubType())
                                || status.getSubType() == null)
                        .anyMatch(status -> status.getMotorSubmissionStatus() == Status.APPROVED))
                .orElse(false);
    }

    private static Optional<MotorStatusResponse> findMotorInStage(
            Batch batch,
            long subDepartmentId,
            String motorId) {
        return findStage(batch, subDepartmentId)
                .map(Batch.StageProgress::getMotorStatuses)
                .filter(statuses -> statuses != null)
                .flatMap(statuses -> statuses.stream()
                        .filter(status -> Objects.equals(status.getMotorId(), motorId))
                        .findFirst());
    }

    public static boolean isPremixUnlocked(
            Batch batch,
            long subDepartmentId,
            int premixNo,
            StageType stageType) {
        if (!isParallelMainFlow(batch)) {
            return findStage(batch, subDepartmentId)
                    .map(stage -> {
                        List<PremixStatusResponse> statuses = stageType == StageType.FINAL_MIX
                                ? stage.getFinalMixStatuses()
                                : stage.getPremixStatuses();
                        if (statuses == null) {
                            return false;
                        }
                        return statuses.stream()
                                .filter(status -> status.getPremixNo() == premixNo)
                                .anyMatch(status -> status.getPremixSubmissionStatus() != Status.YET_TO_START);
                    })
                    .orElse(false);
        }
        if (!isPremixUnlockedByUpstream(batch, subDepartmentId, premixNo, stageType)) {
            return false;
        }
        return findStage(batch, subDepartmentId)
                .map(stage -> {
                    List<PremixStatusResponse> statuses = stageType == StageType.FINAL_MIX
                            ? stage.getFinalMixStatuses()
                            : stage.getPremixStatuses();
                    if (statuses == null) {
                        return false;
                    }
                    return statuses.stream()
                            .filter(status -> status.getPremixNo() == premixNo)
                            .anyMatch(status -> status.getPremixSubmissionStatus() != Status.YET_TO_START);
                })
                .orElse(false);
    }

    public static boolean isMotorUnlocked(Batch batch, long subDepartmentId, String motorId) {
        if (!isParallelMainFlow(batch)) {
            return findStage(batch, subDepartmentId)
                    .map(Batch.StageProgress::getMotorStatuses)
                    .filter(statuses -> statuses != null)
                    .map(statuses -> statuses.stream()
                            .filter(status -> Objects.equals(status.getMotorId(), motorId))
                            .anyMatch(status -> !isUnitLocked(batch, status.getMotorSubmissionStatus())))
                    .orElse(false);
        }
        if (!isMotorUnlockedByUpstream(batch, subDepartmentId, motorId)) {
            return false;
        }
        // QC stage motorStatuses are division-tagged seed rows and stay YET_TO_START until
        // each QC division advances; real unlock lives in divisionStatuses (per unit).
        if (subDepartmentId == MainBatchStageConstants.QC) {
            return true;
        }
        // STF unlock is upstream NDT + subscale; tolerate stale YET_TO_START until heal runs.
        if (subDepartmentId == MainBatchStageConstants.STF) {
            return true;
        }
        return findStage(batch, subDepartmentId)
                .map(Batch.StageProgress::getMotorStatuses)
                .filter(statuses -> statuses != null)
                .map(statuses -> statuses.stream()
                        .filter(status -> Objects.equals(status.getMotorId(), motorId))
                        .anyMatch(status -> status.getMotorSubmissionStatus() != Status.YET_TO_START))
                .orElse(false);
    }

    public static boolean isPremixUnitLocked(
            Batch batch,
            long subDepartmentId,
            int premixNo,
            StageType stageType,
            Status submissionStatus) {
        if (!isParallelMainFlow(batch)) {
            return false;
        }
        if (!isPremixUnlockedByUpstream(batch, subDepartmentId, premixNo, stageType)) {
            return true;
        }
        return submissionStatus == null || submissionStatus == Status.YET_TO_START;
    }

    public static boolean isMotorUnitLocked(
            Batch batch,
            long subDepartmentId,
            String motorId,
            Status submissionStatus) {
        if (BatchMotorDiscardHelper.isDiscarded(batch, motorId)
                || BatchMotorDiscardHelper.isDiscardedStatus(submissionStatus)) {
            return true;
        }
        if (!isParallelMainFlow(batch)) {
            return false;
        }
        if (!isMotorUnlockedByUpstream(batch, subDepartmentId, motorId)) {
            return true;
        }
        // STF: upstream NDT + subscale is the unlock; do not keep display locked on stale
        // YET_TO_START after heal has not yet rewritten the row.
        if (subDepartmentId == MainBatchStageConstants.STF) {
            return false;
        }
        return submissionStatus == null || submissionStatus == Status.YET_TO_START;
    }

    public static Status defaultMotorSubmissionStatus(Batch batch, long subDepartmentId, String motorId) {
        if (!isParallelMainFlow(batch)) {
            return Status.TO_BE_INITIATED;
        }
        return isMotorUnlockedByUpstream(batch, subDepartmentId, motorId)
                ? Status.TO_BE_INITIATED
                : Status.YET_TO_START;
    }

    public static void normalizePremixStatusesForSync(
            Batch batch,
            long subDepartmentId,
            List<PremixStatusResponse> statuses,
            StageType stageType) {
        if (statuses == null || !isParallelMainFlow(batch)) {
            return;
        }
        for (PremixStatusResponse status : statuses) {
            if (status == null) {
                continue;
            }
            if (!isPremixUnlockedByUpstream(
                    batch, subDepartmentId, status.getPremixNo(), stageType)) {
                status.setPremixSubmissionType(Status.DRAFT);
                status.setPremixSubmissionStatus(Status.YET_TO_START);
            }
        }
    }

    public static boolean isUnitLocked(Batch batch, Status submissionStatus) {
        if (BatchMotorDiscardHelper.isDiscardedStatus(submissionStatus)) {
            return true;
        }
        if (!isParallelMainFlow(batch)) {
            return false;
        }
        return submissionStatus == null || submissionStatus == Status.YET_TO_START;
    }
}
