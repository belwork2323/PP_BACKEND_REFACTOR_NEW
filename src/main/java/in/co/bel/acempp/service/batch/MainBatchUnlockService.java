package in.co.bel.acempp.service.batch;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.dto.subscale.SubScaleMetadata;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import in.co.bel.acempp.service.BatchStageSequence;
import in.co.bel.acempp.service.SubscaleMetadataResolver;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionCatalog;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionStageStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MainBatchUnlockService {

    private static final long STF = MainBatchStageConstants.STF;

    private final BatchStageSequence stageSequence;
    private final QcDivisionStageStatusResolver qcDivisionStatusResolver;
    private final SubscaleMetadataResolver subscaleMetadataResolver;

    public void evaluateUnlockRules(
            Batch batch,
            long sourceSubDepartmentId,
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses,
            List<MotorStatusResponse> motorStatuses,
            LocalDateTime now) {
        // Subscale: open mfg STF after Subscale/QC; unlock QC STF after any BEM APPROVED.
        // Must not fall through to parallel-MAIN gates (subscale is not parallel MAIN).
        if ("SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
            if (sourceSubDepartmentId == MainBatchStageConstants.QC
                    || sourceSubDepartmentId == MainBatchStageConstants.SUBSCALE) {
                unlockStfForSubscaleBatch(batch, now);
            }
            if (sourceSubDepartmentId == STF
                    || sourceSubDepartmentId == MainBatchStageConstants.QC
                    || MainBatchStageProgressHelper.hasApprovedBemMotor(batch)) {
                unlockStfFamilyFromBemApprovals(batch, now);
            }
            return;
        }

        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return;
        }

        if (sourceSubDepartmentId == MainBatchStageConstants.RMP && premixStatuses != null) {
            premixStatuses.stream()
                    .filter(status -> status.getPremixSubmissionStatus() == Status.APPROVED)
                    .forEach(status -> {
                        unlockPremixInMixing(batch, status.getPremixNo(), now);
                        ensureStage(batch, MainBatchStageConstants.QC, now);
                        QcDivisionProgressHelper.unlockPremixUnit(
                                batch,
                                QcDivisionCatalog.RAW_MATERIAL_PROCESSING,
                                status.getPremixNo(),
                                StageType.PREMIX);
                    });
        }

        if (sourceSubDepartmentId == MainBatchStageConstants.MIXING) {
            unlockSubscaleWhenMixingComplete(batch, now);
            unlockQcMixingFromManufacturing(batch, premixStatuses, finalMixStatuses, now);
        }

        if (sourceSubDepartmentId == MainBatchStageConstants.CASE_PREP && motorStatuses != null) {
            motorStatuses.stream()
                    .filter(status -> status.getMotorSubmissionStatus() == Status.APPROVED)
                    .forEach(status -> {
                        unlockMotorInCastingCuring(batch, status.getMotorId(), now);
                        ensureStage(batch, MainBatchStageConstants.QC, now);
                        QcDivisionProgressHelper.unlockMotorUnit(
                                batch, QcDivisionCatalog.HARDWARE, status.getMotorId());
                    });
        }

        if (sourceSubDepartmentId == MainBatchStageConstants.CASTING_CURING && motorStatuses != null) {
            boolean subscaleApproved =
                    MainBatchStageProgressHelper.isSubscaleCompletelyApproved(batch);
            motorStatuses.stream()
                    .filter(status -> status.getMotorSubmissionStatus() == Status.APPROVED)
                    .forEach(status -> {
                        unlockMotorInPostCureAndTrimming(batch, status.getMotorId(), now);
                        ensureStage(batch, MainBatchStageConstants.QC, now);
                        QcDivisionProgressHelper.unlockMotorUnit(
                                batch, QcDivisionCatalog.CASTING, status.getMotorId());
                        QcDivisionProgressHelper.unlockMotorUnit(
                                batch, QcDivisionCatalog.DE_CORING, status.getMotorId());
                        // QC CURING also requires subscale COMPLETELY_APPROVED.
                        if (subscaleApproved) {
                            QcDivisionProgressHelper.unlockMotorUnit(
                                    batch, QcDivisionCatalog.CURING, status.getMotorId());
                        }
                    });
        }

        if ((sourceSubDepartmentId == MainBatchStageConstants.POST_CURE
                || sourceSubDepartmentId == MainBatchStageConstants.TRIMMING)
                && motorStatuses != null) {
            motorStatuses.stream()
                    .filter(status -> status.getMotorSubmissionStatus() == Status.APPROVED)
                    .forEach(status -> {
                        unlockMotorInNdtIfReady(batch, status.getMotorId(), now);
                        ensureStage(batch, MainBatchStageConstants.QC, now);
                        if (sourceSubDepartmentId == MainBatchStageConstants.POST_CURE) {
                            QcDivisionProgressHelper.unlockMotorUnit(
                                    batch, QcDivisionCatalog.POST_CURE, status.getMotorId());
                        } else {
                            QcDivisionProgressHelper.unlockMotorUnit(
                                    batch, QcDivisionCatalog.TRIMMING, status.getMotorId());
                        }
                    });
        }

        if (sourceSubDepartmentId == MainBatchStageConstants.NDT && motorStatuses != null) {
            motorStatuses.stream()
                    .filter(status -> status.getMotorSubmissionStatus() == Status.APPROVED)
                    .forEach(status -> {
                        ensureStage(batch, MainBatchStageConstants.QC, now);
                        // Manufacturing NDT approval unlocks QC NDT only.
                        QcDivisionProgressHelper.unlockMotorUnit(
                                batch, QcDivisionCatalog.NDT, status.getMotorId());
                    });
        }

        // Subscale completely approved → unlock QC CURING (if casting done) + STF for NDT-ready motors (+ BEM).
        if (sourceSubDepartmentId == MainBatchStageConstants.SUBSCALE) {
            unlockStfMotorsReadyAfterSubscale(batch, now);
        }

        // Re-evaluate STF whenever unlock runs so NDT-then-subscale or subscale-then-NDT both work.
        unlockStfMotorsReadyAfterNdt(batch, now);

        // Any approved STF BEM unlocks QC STF + QC + Weighment (replaces former NDT family unlock).
        if (sourceSubDepartmentId == STF
                || sourceSubDepartmentId == MainBatchStageConstants.QC
                || MainBatchStageProgressHelper.hasApprovedBemMotor(batch)) {
            unlockStfFamilyFromBemApprovals(batch, now);
        }

        // Dispatch: QC bundle + any STF BEM (not STF MAIN-for-motor).
        if (sourceSubDepartmentId == STF
                || sourceSubDepartmentId == MainBatchStageConstants.QC
                || MainBatchStageProgressHelper.hasApprovedBemMotor(batch)) {
            unlockDispatchMotorsWhenReady(batch, now);
        }
    }

    /**
     * Re-apply STF / QC-family / Dispatch unlock predicates from current {@code stageProgress}
     * (e.g. after batch details load or stage sync). SUBSCALE never unlocks Dispatch.
     */
    public void reconcileStfMotorUnlocks(Batch batch, LocalDateTime now) {
        if (batch == null) {
            return;
        }
        if ("SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
            unlockStfForSubscaleBatch(batch, now);
            unlockStfFamilyFromBemApprovals(batch, now);
            return;
        }
        if (MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            unlockQcCuringWhenSubscaleApproved(batch, now);
            unlockStfMotorsReadyAfterNdt(batch, now);
            unlockStfFamilyFromBemApprovals(batch, now);
            unlockDispatchMotorsWhenReady(batch, now);
        }
    }

    /**
     * Unlock each MAIN-batch motor into Dispatch when QC readiness + any STF BEM APPROVED.
     * No-op for SUBSCALE.
     */
    private void unlockDispatchMotorsWhenReady(Batch batch, LocalDateTime now) {
        if (batch == null || "SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
            return;
        }
        Optional.ofNullable(batch.getMotorIds())
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(motorId -> !motorId.isBlank())
                .filter(motorId -> QcDivisionProgressHelper.isMotorReadyForDispatch(batch, motorId))
                .forEach(motorId -> unlockMotorInStage(
                        batch, MainBatchStageConstants.DISPATCH, motorId, now));
    }

    private void unlockQcMixingFromManufacturing(
            Batch batch,
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses,
            LocalDateTime now) {
        ensureStage(batch, MainBatchStageConstants.QC, now);
        if (premixStatuses != null) {
            premixStatuses.stream()
                    .filter(status -> status.getPremixSubmissionStatus() == Status.APPROVED)
                    .forEach(status -> QcDivisionProgressHelper.unlockPremixUnit(
                            batch,
                            QcDivisionCatalog.MIXING,
                            status.getPremixNo(),
                            StageType.PREMIX));
        }
        if (finalMixStatuses != null) {
            finalMixStatuses.stream()
                    .filter(status -> status.getPremixSubmissionStatus() == Status.APPROVED)
                    .forEach(status -> QcDivisionProgressHelper.unlockPremixUnit(
                            batch,
                            QcDivisionCatalog.MIXING,
                            status.getPremixNo(),
                            StageType.FINAL_MIX));
        }
        // Also unlock from persisted Mixing stage when event lists are null (COMPLETELY_APPROVED path).
        if (premixStatuses == null && finalMixStatuses == null) {
            MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.MIXING)
                    .ifPresent(mixing -> {
                        Optional.ofNullable(mixing.getPremixStatuses()).orElseGet(List::of).stream()
                                .filter(status -> status.getPremixSubmissionStatus() == Status.APPROVED)
                                .forEach(status -> QcDivisionProgressHelper.unlockPremixUnit(
                                        batch,
                                        QcDivisionCatalog.MIXING,
                                        status.getPremixNo(),
                                        StageType.PREMIX));
                        Optional.ofNullable(mixing.getFinalMixStatuses()).orElseGet(List::of).stream()
                                .filter(status -> status.getPremixSubmissionStatus() == Status.APPROVED)
                                .forEach(status -> QcDivisionProgressHelper.unlockPremixUnit(
                                        batch,
                                        QcDivisionCatalog.MIXING,
                                        status.getPremixNo(),
                                        StageType.FINAL_MIX));
                    });
        }
    }

    /**
     * After any STF BEM motor is APPROVED: unlock QC STATIC_TEST_FACILITY division,
     * seed all manufacturing STF motors (MAIN + BEM), unlock only mfg-APPROVED units,
     * plus QC and WEIGHTMENT for each batch main motor.
     */
    private void unlockStfFamilyFromBemApprovals(Batch batch, LocalDateTime now) {
        if (!MainBatchStageProgressHelper.hasApprovedBemMotor(batch)) {
            return;
        }
        ensureStage(batch, MainBatchStageConstants.QC, now);
        // Batches seeded before division 12 existed never get unlock without this backfill.
        QcDivisionProgressHelper.ensureDivision(
                batch,
                QcDivisionCatalog.STATIC_TEST_FACILITY_ID,
                QcDivisionCatalog.STATIC_TEST_FACILITY);
        QcDivisionProgressHelper.unlockDivision(batch, QcDivisionCatalog.STATIC_TEST_FACILITY);

        List<MotorStatusResponse> stfMotors = MainBatchStageProgressHelper.findStage(batch, STF)
                .map(Batch.StageProgress::getMotorStatuses)
                .orElseGet(List::of);
        for (MotorStatusResponse status : stfMotors) {
            if (status == null || status.getMotorId() == null || status.getMotorId().isBlank()) {
                continue;
            }
            boolean approved = status.getMotorSubmissionStatus() == Status.APPROVED;
            QcDivisionProgressHelper.ensureMotorUnit(
                    batch,
                    QcDivisionCatalog.STATIC_TEST_FACILITY,
                    status.getMotorId(),
                    status.getSubType(),
                    approved);
        }

        // Experimental subscale has no MAIN motors — QC / Weighment stay N/A.
        if (QcDivisionProgressHelper.isExperimentalSubscale(batch)) {
            return;
        }

        List<String> batchMotorIds = Optional.ofNullable(batch.getMotorIds()).orElseGet(List::of);
        for (String motorId : batchMotorIds) {
            if (motorId == null || motorId.isBlank()) {
                continue;
            }
            QcDivisionProgressHelper.unlockMotorUnit(batch, QcDivisionCatalog.QC, motorId);
            QcDivisionProgressHelper.unlockMotorUnit(batch, QcDivisionCatalog.WEIGHTMENT, motorId);
        }
    }

    /**
     * MAIN: open STF only when subscale is COMPLETELY_APPROVED and at least one motor is
     * NDT-approved; unlock those mains and seed BEM from subscale metadata.
     */
    private void unlockStfMotorsReadyAfterNdt(Batch batch, LocalDateTime now) {
        if (!MainBatchStageProgressHelper.isSubscaleCompletelyApproved(batch)) {
            return;
        }
        List<String> ndtReadyMotors = Optional.ofNullable(batch.getMotorIds())
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .filter(motorId -> MainBatchStageProgressHelper.isMotorApproved(
                        batch, MainBatchStageConstants.NDT, motorId))
                .toList();
        if (ndtReadyMotors.isEmpty()) {
            return;
        }

        Batch.StageProgress stfStage = ensureStage(batch, STF, now);
        ndtReadyMotors.forEach(motorId -> unlockMotor(stfStage.getMotorStatuses(), motorId));
        ensureStfBemMotors(batch, stfStage);
        unlockBemMotors(stfStage);
    }

    private void unlockStfForSubscaleBatch(Batch batch, LocalDateTime now) {
        if (!MainBatchStageProgressHelper.isSubscaleCompletelyApproved(batch)) {
            return;
        }
        Batch.StageProgress stfStage = ensureStage(batch, STF, now);
        ensureStfBemMotors(batch, stfStage);
        unlockBemMotors(stfStage);

        if (QcDivisionProgressHelper.isExperimentalSubscale(batch)) {
            // Experimental: BEM only — no QC / NDT gate.
            return;
        }

        // Qualification: ID-sheet mains unlock with subscale (no NDT path).
        Optional.ofNullable(batch.getMotorIds())
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .forEach(motorId -> unlockMotor(stfStage.getMotorStatuses(), motorId));
    }

    private void unlockPremixInMixing(Batch batch, int premixNo, LocalDateTime now) {
        Batch.StageProgress mixingStage = ensureStage(batch, MainBatchStageConstants.MIXING, now);
        unlockPremix(mixingStage.getPremixStatuses(), premixNo);
        unlockPremix(mixingStage.getFinalMixStatuses(), premixNo);
    }

    private void unlockSubscaleWhenMixingComplete(Batch batch, LocalDateTime now) {
        if (MainBatchStageProgressHelper.areAllPremixesApproved(batch, MainBatchStageConstants.MIXING)
                && MainBatchStageProgressHelper.areAllFinalMixesApproved(batch)) {
            ensureStage(batch, MainBatchStageConstants.SUBSCALE, now);
        }
    }

    private void unlockMotorInCastingCuring(Batch batch, String motorId, LocalDateTime now) {
        Batch.StageProgress ccStage = ensureStage(batch, MainBatchStageConstants.CASTING_CURING, now);
        unlockMotor(ccStage.getMotorStatuses(), motorId);
    }

    private void unlockMotorInPostCureAndTrimming(Batch batch, String motorId, LocalDateTime now) {
        Batch.StageProgress postCureStage = ensureStage(batch, MainBatchStageConstants.POST_CURE, now);
        Batch.StageProgress trimmingStage = ensureStage(batch, MainBatchStageConstants.TRIMMING, now);
        unlockMotor(postCureStage.getMotorStatuses(), motorId);
        unlockMotor(trimmingStage.getMotorStatuses(), motorId);
    }

    private void unlockMotorInNdtIfReady(Batch batch, String motorId, LocalDateTime now) {
        boolean postCureApproved = MainBatchStageProgressHelper.isMotorApproved(
                batch, MainBatchStageConstants.POST_CURE, motorId);
        boolean trimmingApproved = MainBatchStageProgressHelper.isMotorApproved(
                batch, MainBatchStageConstants.TRIMMING, motorId);
        if (postCureApproved && trimmingApproved) {
            unlockMotorInStage(batch, MainBatchStageConstants.NDT, motorId, now);
        }
    }

    private void unlockStfMotorsReadyAfterSubscale(Batch batch, LocalDateTime now) {
        unlockQcCuringWhenSubscaleApproved(batch, now);
        unlockStfMotorsReadyAfterNdt(batch, now);
    }

    /**
     * When subscale becomes COMPLETELY_APPROVED, unlock QC CURING for motors already
     * approved in manufacturing Casting/Curing.
     */
    private void unlockQcCuringWhenSubscaleApproved(Batch batch, LocalDateTime now) {
        if (!MainBatchStageProgressHelper.isSubscaleCompletelyApproved(batch)) {
            return;
        }
        ensureStage(batch, MainBatchStageConstants.QC, now);
        MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.CASTING_CURING)
                .map(Batch.StageProgress::getMotorStatuses)
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .filter(status -> status.getMotorSubmissionStatus() == Status.APPROVED)
                .map(MotorStatusResponse::getMotorId)
                .filter(Objects::nonNull)
                .forEach(motorId -> QcDivisionProgressHelper.unlockMotorUnit(
                        batch, QcDivisionCatalog.CURING, motorId));
    }

    private void unlockBemMotors(Batch.StageProgress stfStage) {
        if (stfStage.getMotorStatuses() == null) {
            return;
        }
        stfStage.getMotorStatuses().stream()
                .filter(status -> StfMotorSubType.BEM.name().equals(status.getSubType()))
                .filter(status -> status.getMotorSubmissionStatus() == Status.YET_TO_START)
                .forEach(status -> {
                    status.setMotorSubmissionType(Status.DRAFT);
                    status.setMotorSubmissionStatus(Status.TO_BE_INITIATED);
                });
    }

    private void unlockMotorInStage(
            Batch batch,
            long subDepartmentId,
            String motorId,
            LocalDateTime now) {
        Batch.StageProgress stage = ensureStage(batch, subDepartmentId, now);
        unlockMotor(stage.getMotorStatuses(), motorId);
    }

    private Batch.StageProgress ensureStage(Batch batch, long subDepartmentId, LocalDateTime now) {
        Optional<Batch.StageProgress> existing = MainBatchStageProgressHelper.findStage(batch, subDepartmentId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Batch.StageProgress created = createStage(batch, subDepartmentId, now);
        batch.getStageProgress().add(created);
        return created;
    }

    private Batch.StageProgress createStage(Batch batch, long subDepartmentId, LocalDateTime now) {
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

        if (subDepartmentId == MainBatchStageConstants.MIXING) {
            builder.premixStatuses(lockedPremixStatuses(premixCount, StageType.PREMIX));
            builder.finalMixStatuses(lockedPremixStatuses(premixCount, StageType.FINAL_MIX));
        } else if (subDepartmentId == STF) {
            builder.motorStatuses(initialStfMotorStatuses(batch, true));
        } else if (subDepartmentId == MainBatchStageConstants.QC) {
            builder.divisionStatuses(qcDivisionStatusResolver.initialStatuses(batch));
            builder.motorStatuses(lockedMotorStatuses(batch.getMotorIds()));
        } else if (MainBatchStageConstants.MOTOR_STAGES.contains(subDepartmentId)) {
            builder.motorStatuses(lockedMotorStatuses(batch.getMotorIds()));
        }
        return builder.build();
    }

    private void unlockPremix(List<PremixStatusResponse> statuses, int premixNo) {
        if (statuses == null) {
            return;
        }
        statuses.stream()
                .filter(status -> status.getPremixNo() == premixNo)
                .filter(status -> status.getPremixSubmissionStatus() == Status.YET_TO_START)
                .forEach(status -> {
                    status.setPremixSubmissionType(Status.DRAFT);
                    status.setPremixSubmissionStatus(Status.TO_BE_INITIATED);
                });
    }

    private void unlockMotor(List<MotorStatusResponse> statuses, String motorId) {
        if (statuses == null) {
            return;
        }
        statuses.stream()
                .filter(status -> Objects.equals(status.getMotorId(), motorId))
                .filter(status -> status.getMotorSubmissionStatus() == Status.YET_TO_START)
                .forEach(status -> {
                    status.setMotorSubmissionType(Status.DRAFT);
                    status.setMotorSubmissionStatus(Status.TO_BE_INITIATED);
                });
    }

    private List<PremixStatusResponse> lockedPremixStatuses(int count, StageType stageType) {
        List<PremixStatusResponse> statuses = new ArrayList<>(count);
        for (int premixNo = 1; premixNo <= count; premixNo++) {
            statuses.add(PremixStatusResponse.builder()
                    .premixNo(premixNo)
                    .stageType(stageType)
                    .premixSubmissionType(Status.DRAFT)
                    .premixSubmissionStatus(Status.YET_TO_START)
                    .build());
        }
        return statuses;
    }

    private List<MotorStatusResponse> lockedMotorStatuses(List<String> motorIds) {
        if (motorIds == null) {
            return new ArrayList<>();
        }
        return motorIds.stream()
                .map(motorId -> MotorStatusResponse.builder()
                        .motorId(motorId)
                        .subType(StfMotorSubType.MAIN_MOTOR.name())
                        .motorSubmissionType(Status.DRAFT)
                        .motorSubmissionStatus(Status.YET_TO_START)
                        .build())
                .toList();
    }

    private List<MotorStatusResponse> initialStfMotorStatuses(Batch batch, boolean locked) {
        List<MotorStatusResponse> statuses = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Status initialStatus = locked ? Status.YET_TO_START : Status.TO_BE_INITIATED;

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
                                .motorSubmissionStatus(initialStatus)
                                .build());
                    }
                });

        SubScaleMetadata metadata = subscaleMetadataResolver.resolveApprovedMetadata(batch.getBatchId());
        subscaleMetadataResolver.toBemMotorIds(metadata).forEach(bemMotorId -> {
            if (seen.add(bemMotorId)) {
                statuses.add(MotorStatusResponse.builder()
                        .motorId(bemMotorId)
                        .subType(StfMotorSubType.BEM.name())
                        .motorSubmissionType(Status.DRAFT)
                        .motorSubmissionStatus(initialStatus)
                        .build());
            }
        });
        return statuses;
    }

    private void ensureStfBemMotors(Batch batch, Batch.StageProgress stfStage) {
        List<MotorStatusResponse> statuses = stfStage.getMotorStatuses() == null
                ? new ArrayList<>()
                : new ArrayList<>(stfStage.getMotorStatuses());
        Set<String> existing = new LinkedHashSet<>();
        statuses.forEach(status -> {
            if (status != null && status.getMotorId() != null) {
                existing.add(status.getMotorId());
            }
        });

        SubScaleMetadata metadata = resolveBemMetadata(batch);
        // Seed unlocked whenever subscale is COMPLETELY_APPROVED (no QC gate).
        Status seedStatus = MainBatchStageProgressHelper.isSubscaleCompletelyApproved(batch)
                ? Status.TO_BE_INITIATED
                : Status.YET_TO_START;
        subscaleMetadataResolver.toBemMotorIds(metadata).forEach(bemMotorId -> {
            if (existing.add(bemMotorId)) {
                statuses.add(MotorStatusResponse.builder()
                        .motorId(bemMotorId)
                        .subType(StfMotorSubType.BEM.name())
                        .motorSubmissionType(Status.DRAFT)
                        .motorSubmissionStatus(seedStatus)
                        .build());
            }
        });
        stfStage.setMotorStatuses(statuses);
    }

    /**
     * Prefer live resolver (approved form); fall back to persisted ID-sheet metadata.subScale.
     */
    private SubScaleMetadata resolveBemMetadata(Batch batch) {
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
}
