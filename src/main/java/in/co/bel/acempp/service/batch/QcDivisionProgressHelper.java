package in.co.bel.acempp.service.batch;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.qcdivision.QcStageDivisionStatusDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Helpers for QC stage {@code divisionStatuses} unit locks and STF readiness.
 */
public final class QcDivisionProgressHelper {

    private QcDivisionProgressHelper() {
    }

    public static Optional<QcStageDivisionStatusDto> findDivision(Batch batch, String divisionName) {
        String canonical = QcDivisionCatalog.normalizeDivisionName(divisionName);
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.QC)
                .map(Batch.StageProgress::getDivisionStatuses)
                .filter(Objects::nonNull)
                .flatMap(statuses -> statuses.stream()
                        .filter(status -> Objects.equals(
                                QcDivisionCatalog.normalizeDivisionName(
                                        status.getDivisionName() != null
                                                ? status.getDivisionName()
                                                : QcDivisionCatalog.canonicalName(status.getDivisionId())),
                                canonical)
                                || Objects.equals(
                                QcDivisionCatalog.canonicalName(status.getDivisionId()), canonical))
                        .findFirst());
    }

    public static Optional<QcStageDivisionStatusDto> findDivisionById(Batch batch, long divisionId) {
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.QC)
                .map(Batch.StageProgress::getDivisionStatuses)
                .filter(Objects::nonNull)
                .flatMap(statuses -> statuses.stream()
                        .filter(status -> Objects.equals(status.getDivisionId(), divisionId))
                        .findFirst());
    }

    public static boolean isDivisionUnlocked(Batch batch, String divisionName) {
        return findDivision(batch, divisionName)
                .map(status -> status.getStatus() != null && status.getStatus() != Status.YET_TO_START)
                .orElse(false);
    }

    public static boolean isPremixUnitUnlocked(
            Batch batch, String divisionName, int premixNo, StageType stageType) {
        boolean markedUnlocked = findDivision(batch, divisionName)
                .map(division -> {
                    List<PremixStatusResponse> units = stageType == StageType.FINAL_MIX
                            ? division.getFinalMixStatuses()
                            : division.getPremixStatuses();
                    if (units == null) {
                        return false;
                    }
                    return units.stream()
                            .filter(unit -> unit.getPremixNo() == premixNo)
                            .anyMatch(unit -> unit.getPremixSubmissionStatus() != Status.YET_TO_START);
                })
                .orElse(false);
        if (!markedUnlocked) {
            return false;
        }
        // Parallel MAIN: manufacturing APPROVED is authoritative — ignore corrupted
        // divisionStatuses TO_BE_INITIATED that unlocked every Premix when one was approved.
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return true;
        }
        String canonical = QcDivisionCatalog.normalizeDivisionName(divisionName);
        if (QcDivisionCatalog.RAW_MATERIAL_PROCESSING.equals(canonical)) {
            return MainBatchStageProgressHelper.isPremixApproved(batch, premixNo);
        }
        if (QcDivisionCatalog.MIXING.equals(canonical)) {
            if (stageType == StageType.FINAL_MIX) {
                return MainBatchStageProgressHelper.isMixingFinalMixApproved(batch, premixNo);
            }
            return MainBatchStageProgressHelper.isMixingPremixApproved(batch, premixNo);
        }
        return true;
    }

    public static boolean isMotorUnitUnlocked(Batch batch, String divisionName, String motorId) {
        return findDivision(batch, divisionName)
                .map(division -> {
                    List<MotorStatusResponse> units = division.getMotorStatuses();
                    if (units == null) {
                        return false;
                    }
                    return units.stream()
                            .filter(unit -> Objects.equals(unit.getMotorId(), motorId))
                            .anyMatch(unit -> unit.getMotorSubmissionStatus() != Status.YET_TO_START);
                })
                .orElse(false);
    }

    public static boolean isMotorUnitApproved(Batch batch, String divisionName, String motorId) {
        String canonical = QcDivisionCatalog.normalizeDivisionName(divisionName);
        boolean fromDivision = findDivision(batch, canonical)
                .map(division -> Optional.ofNullable(division.getMotorStatuses()).orElseGet(List::of)
                        .stream()
                        .filter(unit -> Objects.equals(unit.getMotorId(), motorId))
                        .anyMatch(unit -> unit.getMotorSubmissionStatus() == Status.APPROVED))
                .orElse(false);
        if (fromDivision) {
            return true;
        }
        // Fallback: QC stage motorStatuses (authoritative after unit approval sync).
        // divisionStatuses unit rows can lag as TO_BE_INITIATED after unlock.
        return isMotorApprovedOnQcStage(batch, canonical, motorId);
    }

    public static boolean isRevalidationApproved(Batch batch) {
        return findDivision(batch, QcDivisionCatalog.RAW_MATERIAL_REVALIDATION)
                .map(status -> status.getStatus() == Status.APPROVED
                        || status.getStatus() == Status.COMPLETELY_APPROVED)
                .orElse(false);
    }

    public static boolean areAllPremixUnitsApproved(Batch batch, String divisionName) {
        String canonical = QcDivisionCatalog.normalizeDivisionName(divisionName);
        int expected = premixCount(batch);
        if (expected <= 0) {
            List<PremixStatusResponse> units = findDivision(batch, canonical)
                    .map(division -> Optional.ofNullable(division.getPremixStatuses()).orElseGet(List::of))
                    .orElseGet(List::of);
            if (units.isEmpty()) {
                return isPremixDivisionFullyApprovedOnQcStage(batch, canonical, StageType.PREMIX);
            }
            return units.stream().allMatch(unit -> isPremixUnitApproved(
                    batch, canonical, unit.getPremixNo(), StageType.PREMIX));
        }
        for (int premixNo = 1; premixNo <= expected; premixNo++) {
            if (!isPremixUnitApproved(batch, canonical, premixNo, StageType.PREMIX)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllFinalMixUnitsApproved(Batch batch, String divisionName) {
        String canonical = QcDivisionCatalog.normalizeDivisionName(divisionName);
        int expected = premixCount(batch);
        if (expected <= 0) {
            List<PremixStatusResponse> units = findDivision(batch, canonical)
                    .map(division -> Optional.ofNullable(division.getFinalMixStatuses()).orElseGet(List::of))
                    .orElseGet(List::of);
            if (units.isEmpty()) {
                return isPremixDivisionFullyApprovedOnQcStage(batch, canonical, StageType.FINAL_MIX);
            }
            return units.stream().allMatch(unit -> isPremixUnitApproved(
                    batch, canonical, unit.getPremixNo(), StageType.FINAL_MIX));
        }
        for (int premixNo = 1; premixNo <= expected; premixNo++) {
            if (!isPremixUnitApproved(batch, canonical, premixNo, StageType.FINAL_MIX)) {
                return false;
            }
        }
        return true;
    }

    /** True when this Premix / Final Mix is APPROVED in divisionStatuses or QC stage statuses. */
    public static boolean isPremixUnitApproved(
            Batch batch, String divisionName, int premixNo, StageType stageType) {
        String canonical = QcDivisionCatalog.normalizeDivisionName(divisionName);
        boolean fromDivision = findDivision(batch, canonical)
                .map(division -> {
                    List<PremixStatusResponse> units = stageType == StageType.FINAL_MIX
                            ? division.getFinalMixStatuses()
                            : division.getPremixStatuses();
                    if (units == null) {
                        return false;
                    }
                    return units.stream()
                            .filter(unit -> unit.getPremixNo() == premixNo)
                            .anyMatch(unit -> unit.getPremixSubmissionStatus() == Status.APPROVED);
                })
                .orElse(false);
        if (fromDivision) {
            return true;
        }
        return isPremixApprovedOnQcStage(batch, canonical, premixNo, stageType);
    }

    public static boolean isSameMotorApprovedAcrossStfMotorDivisions(Batch batch, String motorId) {
        for (String division : QcDivisionCatalog.STF_MOTOR_DIVISIONS) {
            if (!isMotorUnitApproved(batch, division, motorId)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isMainOrQualificationQcReadyForStf(Batch batch, String motorId) {
        return isRevalidationApproved(batch)
                && areAllPremixUnitsApproved(batch, QcDivisionCatalog.RAW_MATERIAL_PROCESSING)
                && areAllPremixUnitsApproved(batch, QcDivisionCatalog.MIXING)
                && areAllFinalMixUnitsApproved(batch, QcDivisionCatalog.MIXING)
                && isSameMotorApprovedAcrossStfMotorDivisions(batch, motorId);
    }

    /**
     * Dispatch readiness for a MAIN-batch motor: QC revalidation/processing/mixing complete,
     * same motorId APPROVED across Hardware…Weighment/QC, and any manufacturing STF BEM APPROVED.
     * Main-motor STF approval is not required. SUBSCALE never unlocks Dispatch.
     */
    public static boolean isMotorReadyForDispatch(Batch batch, String motorId) {
        if (batch == null || motorId == null || motorId.isBlank()) {
            return false;
        }
        if ("SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
            return false;
        }
        return isMainOrQualificationQcReadyForStf(batch, motorId)
                && MainBatchStageProgressHelper.hasApprovedBemMotor(batch);
    }

    private static int premixCount(Batch batch) {
        if (batch == null || batch.getIdentificationSheet() == null
                || batch.getIdentificationSheet().getNumberOfPremix() == null) {
            return 0;
        }
        return Math.max(0, batch.getIdentificationSheet().getNumberOfPremix());
    }

    private static boolean isMotorApprovedOnQcStage(
            Batch batch, String divisionName, String motorId) {
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.QC)
                .map(Batch.StageProgress::getMotorStatuses)
                .filter(Objects::nonNull)
                .map(statuses -> statuses.stream()
                        .filter(status -> Objects.equals(
                                QcDivisionCatalog.normalizeDivisionName(status.getDivision()),
                                divisionName))
                        .filter(status -> Objects.equals(status.getMotorId(), motorId))
                        .anyMatch(status -> status.getMotorSubmissionStatus() == Status.APPROVED))
                .orElse(false);
    }

    private static boolean isPremixApprovedOnQcStage(
            Batch batch, String divisionName, int premixNo, StageType stageType) {
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.QC)
                .map(stage -> {
                    List<PremixStatusResponse> units = stageType == StageType.FINAL_MIX
                            ? stage.getFinalMixStatuses()
                            : stage.getPremixStatuses();
                    if (units == null) {
                        return false;
                    }
                    return units.stream()
                            .filter(status -> Objects.equals(
                                    QcDivisionCatalog.normalizeDivisionName(status.getDivision()),
                                    divisionName))
                            .filter(status -> status.getPremixNo() == premixNo)
                            .filter(status -> stageType == StageType.FINAL_MIX
                                    || status.getStageType() == null
                                    || status.getStageType() == StageType.PREMIX
                                    || status.getStageType() == stageType)
                            .anyMatch(status -> status.getPremixSubmissionStatus() == Status.APPROVED);
                })
                .orElse(false);
    }

    private static boolean isPremixDivisionFullyApprovedOnQcStage(
            Batch batch, String divisionName, StageType stageType) {
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.QC)
                .map(stage -> {
                    List<PremixStatusResponse> units = stageType == StageType.FINAL_MIX
                            ? stage.getFinalMixStatuses()
                            : stage.getPremixStatuses();
                    if (units == null || units.isEmpty()) {
                        return false;
                    }
                    List<PremixStatusResponse> scoped = units.stream()
                            .filter(status -> Objects.equals(
                                    QcDivisionCatalog.normalizeDivisionName(status.getDivision()),
                                    divisionName))
                            .toList();
                    return !scoped.isEmpty()
                            && scoped.stream().allMatch(
                                    status -> status.getPremixSubmissionStatus() == Status.APPROVED);
                })
                .orElse(false);
    }

    public static boolean isExperimentalQcReadyForStf(Batch batch) {
        return isRevalidationApproved(batch)
                && areAllPremixUnitsApproved(batch, QcDivisionCatalog.RAW_MATERIAL_PROCESSING)
                && areAllPremixUnitsApproved(batch, QcDivisionCatalog.MIXING)
                && areAllFinalMixUnitsApproved(batch, QcDivisionCatalog.MIXING);
    }

    public static boolean isExperimentalSubscale(Batch batch) {
        return batch != null
                && "SUBSCALE".equalsIgnoreCase(batch.getBatchType())
                && "EXPERIMENTAL".equalsIgnoreCase(batch.getSubBatchType());
    }

    public static boolean isQualificationSubscale(Batch batch) {
        return batch != null
                && "SUBSCALE".equalsIgnoreCase(batch.getBatchType())
                && "QUALIFICATION".equalsIgnoreCase(batch.getSubBatchType());
    }

    /**
     * Append a missing QC {@code divisionStatuses} row (e.g. STATIC_TEST_FACILITY added after
     * the batch was seeded). No-op when the division already exists or QC stage is absent.
     */
    public static void ensureDivision(Batch batch, long divisionId, String divisionName) {
        if (batch == null || divisionName == null || divisionName.isBlank()) {
            return;
        }
        if (findDivision(batch, divisionName).isPresent()
                || findDivisionById(batch, divisionId).isPresent()) {
            return;
        }
        MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.QC)
                .ifPresent(stage -> {
                    List<QcStageDivisionStatusDto> statuses = stage.getDivisionStatuses();
                    if (statuses == null) {
                        statuses = new ArrayList<>();
                        stage.setDivisionStatuses(statuses);
                    }
                    String canonical = QcDivisionCatalog.normalizeDivisionName(divisionName);
                    statuses.add(QcStageDivisionStatusDto.builder()
                            .divisionId(divisionId)
                            .divisionName(canonical != null ? canonical : divisionName)
                            .divisionSubmissionType(Status.DRAFT)
                            .status(Status.YET_TO_START)
                            .motorStatuses(new ArrayList<>())
                            .build());
                });
    }

    public static void unlockDivision(Batch batch, String divisionName) {
        findDivision(batch, divisionName).ifPresent(division -> {
            if (division.getStatus() == Status.YET_TO_START) {
                division.setStatus(Status.TO_BE_INITIATED);
                division.setDivisionSubmissionType(Status.DRAFT);
            }
        });
    }

    /**
     * Ensure a motor unit row exists. When {@code unlocked} is false, idle units stay
     * {@code YET_TO_START}; when true, promote idle units to {@code TO_BE_INITIATED}.
     */
    public static void ensureMotorUnit(
            Batch batch,
            String divisionName,
            String motorId,
            String subType,
            boolean unlocked) {
        if (motorId == null || motorId.isBlank()) {
            return;
        }
        if (unlocked) {
            unlockMotorUnit(batch, divisionName, motorId);
            findDivision(batch, divisionName).ifPresent(division -> {
                Optional.ofNullable(division.getMotorStatuses()).orElseGet(List::of).stream()
                        .filter(unit -> Objects.equals(unit.getMotorId(), motorId))
                        .findFirst()
                        .ifPresent(unit -> {
                            if (subType != null && !subType.isBlank()
                                    && (unit.getSubType() == null || unit.getSubType().isBlank())) {
                                unit.setSubType(subType);
                            }
                        });
            });
            return;
        }
        findDivision(batch, divisionName).ifPresent(division -> {
            List<MotorStatusResponse> units = division.getMotorStatuses();
            if (units == null) {
                units = new ArrayList<>();
                division.setMotorStatuses(units);
            }
            Optional<MotorStatusResponse> existing = units.stream()
                    .filter(unit -> Objects.equals(unit.getMotorId(), motorId))
                    .findFirst();
            if (existing.isPresent()) {
                MotorStatusResponse unit = existing.get();
                Status status = unit.getMotorSubmissionStatus();
                if (status == null
                        || status == Status.YET_TO_START
                        || status == Status.TO_BE_INITIATED
                        || status == Status.DRAFT) {
                    unit.setMotorSubmissionType(Status.DRAFT);
                    unit.setMotorSubmissionStatus(Status.YET_TO_START);
                }
                if (subType != null && !subType.isBlank()
                        && (unit.getSubType() == null || unit.getSubType().isBlank())) {
                    unit.setSubType(subType);
                }
            } else {
                units.add(MotorStatusResponse.builder()
                        .motorId(motorId)
                        .subType(subType)
                        .motorSubmissionType(Status.DRAFT)
                        .motorSubmissionStatus(Status.YET_TO_START)
                        .build());
            }
        });
    }

    public static void unlockPremixUnit(
            Batch batch, String divisionName, int premixNo, StageType stageType) {
        unlockDivision(batch, divisionName);
        findDivision(batch, divisionName).ifPresent(division -> {
            List<PremixStatusResponse> units = stageType == StageType.FINAL_MIX
                    ? division.getFinalMixStatuses()
                    : division.getPremixStatuses();
            if (units == null) {
                return;
            }
            units.stream()
                    .filter(unit -> unit.getPremixNo() == premixNo)
                    .filter(unit -> unit.getPremixSubmissionStatus() == Status.YET_TO_START)
                    .forEach(unit -> {
                        unit.setPremixSubmissionType(Status.DRAFT);
                        unit.setPremixSubmissionStatus(Status.TO_BE_INITIATED);
                    });
        });
    }

    public static void unlockMotorUnit(Batch batch, String divisionName, String motorId) {
        unlockDivision(batch, divisionName);
        findDivision(batch, divisionName).ifPresent(division -> {
            List<MotorStatusResponse> units = division.getMotorStatuses();
            if (units == null) {
                units = new ArrayList<>();
                division.setMotorStatuses(units);
            }
            Optional<MotorStatusResponse> existing = units.stream()
                    .filter(unit -> Objects.equals(unit.getMotorId(), motorId))
                    .findFirst();
            if (existing.isPresent()) {
                MotorStatusResponse unit = existing.get();
                if (unit.getMotorSubmissionStatus() == Status.YET_TO_START) {
                    unit.setMotorSubmissionType(Status.DRAFT);
                    unit.setMotorSubmissionStatus(Status.TO_BE_INITIATED);
                }
            } else {
                units.add(MotorStatusResponse.builder()
                        .motorId(motorId)
                        .motorSubmissionType(Status.DRAFT)
                        .motorSubmissionStatus(Status.TO_BE_INITIATED)
                        .build());
            }
        });
    }
}
