package in.co.bel.acempp.service.qualitycontrol;

import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.qcdivision.DivisionStatusDto;
import in.co.bel.acempp.dto.qcdivision.QcStageDivisionStatusDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.qualitycontrol.DivisionDetail;
import in.co.bel.acempp.entity.qualitycontrol.QcDivision;
import in.co.bel.acempp.entity.qualitycontrol.QcDivisionMaster;
import in.co.bel.acempp.entity.qualitycontrol.QcDivisionWorkflow;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionMasterRepository;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionRepository;
import in.co.bel.acempp.service.batch.MainBatchStageConstants;
import in.co.bel.acempp.service.batch.MainBatchStageProgressHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QcDivisionStageStatusResolver {

    private final QcDivisionMasterRepository masterRepository;
    private final QcDivisionRepository divisionRepository;

    public List<QcStageDivisionStatusDto> initialStatuses(Batch batch) {
        SeedMode mode = resolveSeedMode(batch);
        int premixCount = premixCount(batch);
        List<String> motorIds = Optional.ofNullable(batch.getMotorIds()).orElseGet(List::of);

        return enabledDivisionSeeds().stream()
                .map(seed -> buildInitialDivision(seed, mode, premixCount, motorIds))
                .toList();
    }

    public List<QcStageDivisionStatusDto> resolveLegacyStatuses(Batch batch) {
        QcDivision form = divisionRepository.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                .orElse(null);
        if (form == null || form.getDivisionWorkflow() == null
                || form.getDivisionWorkflow().isEmpty()) {
            return initialStatuses(batch);
        }

        Map<Long, String> divisionNames = divisionNamesById();
        List<DivisionDetail> details = Optional.ofNullable(form.getDivisionDetails())
                .orElseGet(Collections::emptyList);
        SeedMode mode = resolveSeedMode(batch);
        int premixCount = premixCount(batch);
        List<String> motorIds = Optional.ofNullable(batch.getMotorIds()).orElseGet(List::of);

        return form.getDivisionWorkflow().stream()
                .sorted(Comparator.comparing(QcDivisionWorkflow::getSequence,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(workflow -> {
                    Status status = workflow.getStatus() == null
                            ? Status.TO_BE_INITIATED : workflow.getStatus();
                    String canonical = QcDivisionCatalog.normalizeDivisionName(
                            QcDivisionCatalog.canonicalName(workflow.getDivisionId()) != null
                                    ? QcDivisionCatalog.canonicalName(workflow.getDivisionId())
                                    : workflow.getDivision());
                    boolean unlocked = !workflow.isLocked() || status != Status.YET_TO_START;
                    if (status == Status.TO_BE_INITIATED && workflow.isLocked()) {
                        unlocked = false;
                        status = Status.YET_TO_START;
                    }
                    QcStageDivisionStatusDto.QcStageDivisionStatusDtoBuilder builder =
                            QcStageDivisionStatusDto.builder()
                                    .divisionId(workflow.getDivisionId())
                                    .divisionSubmissionType(resolveSubmissionType(
                                            workflow.getDivisionId(), workflow.getDivision(),
                                            status, details))
                                    .status(unlocked ? status : Status.YET_TO_START)
                                    .divisionName(resolveDivisionName(
                                            workflow.getDivisionId(),
                                            canonical != null ? canonical : workflow.getDivision(),
                                            divisionNames));
                    attachUnits(builder, canonical, unlocked || mode != SeedMode.MAIN,
                            mode, premixCount, motorIds, status);
                    return builder.build();
                })
                .toList();
    }

    public List<QcStageDivisionStatusDto> compactStatuses(
            QcDivision form,
            List<DivisionStatusDto> statuses) {
        return compactStatuses(form, statuses, null);
    }

    public List<QcStageDivisionStatusDto> compactStatuses(
            QcDivision form,
            List<DivisionStatusDto> statuses,
            Batch batch) {
        if (statuses == null) {
            return null;
        }
        Map<Long, String> divisionNames = divisionNamesById();
        List<DivisionDetail> details = form == null || form.getDivisionDetails() == null
                ? List.of() : form.getDivisionDetails();
        Map<Long, QcStageDivisionStatusDto> existingById = new LinkedHashMap<>();
        if (batch != null) {
            MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.QC)
                    .map(Batch.StageProgress::getDivisionStatuses)
                    .orElseGet(List::of)
                    .forEach(status -> {
                        if (status.getDivisionId() != null) {
                            existingById.put(status.getDivisionId(), status);
                        }
                    });
        }
        return statuses.stream()
                .map(status -> {
                    QcStageDivisionStatusDto existing = existingById.get(status.getDivisionId());
                    return QcStageDivisionStatusDto.builder()
                            .divisionId(status.getDivisionId())
                            .divisionSubmissionType(resolveSubmissionType(
                                    status.getDivisionId(), status.getDivision(),
                                    status.getStatus(), details))
                            .status(status.getStatus())
                            .divisionName(resolveDivisionName(
                                    status.getDivisionId(), status.getDivision(), divisionNames))
                            .premixStatuses(existing == null ? null : existing.getPremixStatuses())
                            .finalMixStatuses(existing == null ? null : existing.getFinalMixStatuses())
                            .motorStatuses(existing == null ? null : existing.getMotorStatuses())
                            .build();
                })
                .toList();
    }

    private QcStageDivisionStatusDto buildInitialDivision(
            DivisionSeed seed,
            SeedMode mode,
            int premixCount,
            List<String> motorIds) {
        String canonical = QcDivisionCatalog.normalizeDivisionName(
                QcDivisionCatalog.canonicalName(seed.divisionId()));
        boolean unlocked = isInitiallyUnlocked(canonical, mode);
        Status divisionStatus = unlocked ? Status.TO_BE_INITIATED : Status.YET_TO_START;

        QcStageDivisionStatusDto.QcStageDivisionStatusDtoBuilder builder =
                QcStageDivisionStatusDto.builder()
                        .divisionId(seed.divisionId())
                        .divisionSubmissionType(Status.DRAFT)
                        .status(divisionStatus)
                        .divisionName(seed.divisionName());

        attachUnits(builder, canonical, unlocked, mode, premixCount, motorIds, divisionStatus);
        return builder.build();
    }

    private void attachUnits(
            QcStageDivisionStatusDto.QcStageDivisionStatusDtoBuilder builder,
            String canonical,
            boolean divisionUnlocked,
            SeedMode mode,
            int premixCount,
            List<String> motorIds,
            Status divisionStatus) {
        if (canonical == null) {
            return;
        }
        Status unitStatus = divisionUnlocked ? Status.TO_BE_INITIATED : Status.YET_TO_START;
        // MAIN locked divisions keep units YET_TO_START even if division row somehow unlocked later.
        if (mode == SeedMode.MAIN && divisionStatus == Status.YET_TO_START) {
            unitStatus = Status.YET_TO_START;
        }
        if (QcDivisionCatalog.isPremixScoped(canonical)) {
            builder.premixStatuses(lockedOrOpenPremixes(premixCount, StageType.PREMIX, unitStatus));
            if (QcDivisionCatalog.MIXING.equals(canonical)) {
                builder.finalMixStatuses(
                        lockedOrOpenPremixes(premixCount, StageType.FINAL_MIX, unitStatus));
            }
        } else if (QcDivisionCatalog.isMotorScoped(canonical)) {
            if (mode == SeedMode.EXPERIMENTAL) {
                builder.motorStatuses(List.of());
            } else {
                builder.motorStatuses(lockedOrOpenMotors(motorIds, unitStatus));
            }
        }
    }

    private boolean isInitiallyUnlocked(String canonical, SeedMode mode) {
        if (canonical == null) {
            return false;
        }
        return switch (mode) {
            case MAIN -> QcDivisionCatalog.RAW_MATERIAL_REVALIDATION.equals(canonical);
            case EXPERIMENTAL -> QcDivisionCatalog.isExperimentalUnlocked(canonical);
            case QUALIFICATION -> true;
        };
    }

    private SeedMode resolveSeedMode(Batch batch) {
        if (batch == null) {
            return SeedMode.MAIN;
        }
        if ("SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
            if ("EXPERIMENTAL".equalsIgnoreCase(batch.getSubBatchType())) {
                return SeedMode.EXPERIMENTAL;
            }
            return SeedMode.QUALIFICATION;
        }
        return SeedMode.MAIN;
    }

    private int premixCount(Batch batch) {
        if (batch.getIdentificationSheet() != null
                && batch.getIdentificationSheet().getNumberOfPremix() != null) {
            return batch.getIdentificationSheet().getNumberOfPremix();
        }
        return 0;
    }

    private List<PremixStatusResponse> lockedOrOpenPremixes(
            int count, StageType stageType, Status unitStatus) {
        List<PremixStatusResponse> statuses = new ArrayList<>(Math.max(count, 0));
        for (int premixNo = 1; premixNo <= count; premixNo++) {
            statuses.add(PremixStatusResponse.builder()
                    .premixNo(premixNo)
                    .stageType(stageType)
                    .premixSubmissionType(Status.DRAFT)
                    .premixSubmissionStatus(unitStatus)
                    .build());
        }
        return statuses;
    }

    private List<MotorStatusResponse> lockedOrOpenMotors(List<String> motorIds, Status unitStatus) {
        if (motorIds == null || motorIds.isEmpty()) {
            return new ArrayList<>();
        }
        return motorIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .map(motorId -> MotorStatusResponse.builder()
                        .motorId(motorId)
                        .subType(StfMotorSubType.MAIN_MOTOR.name())
                        .motorSubmissionType(Status.DRAFT)
                        .motorSubmissionStatus(unitStatus)
                        .build())
                .toList();
    }

    private Status resolveSubmissionType(
            Long divisionId,
            String division,
            Status aggregateStatus,
            List<DivisionDetail> details) {
        List<Status> explicitTypes = details.stream()
                .filter(detail -> Objects.equals(detail.getDivisionId(), divisionId)
                        || Objects.equals(detail.getDivision(), division))
                .map(DivisionDetail::getDivisionSubmissionType)
                .filter(Objects::nonNull)
                .toList();
        if (explicitTypes.contains(Status.SUBMIT)) {
            return Status.SUBMIT;
        }
        if (explicitTypes.contains(Status.DRAFT)) {
            return Status.DRAFT;
        }
        if (aggregateStatus == null) {
            return Status.DRAFT;
        }
        return switch (aggregateStatus) {
            case WAITING_FOR_APPROVAL, WAITING_FOR_PARTIAL_APPROVAL,
                    APPROVED, REJECTED, COMPLETELY_APPROVED -> Status.SUBMIT;
            default -> Status.DRAFT;
        };
    }

    private List<DivisionSeed> enabledDivisionSeeds() {
        List<DivisionSeed> seeds = new ArrayList<>();
        for (QcDivisionMaster master : masterRepository.findAllByOrderByDisplayOrderAsc()) {
            if (Boolean.TRUE.equals(master.getDisabled())) {
                continue;
            }
            List<QcDivisionMaster.DivisionType> types = Optional.ofNullable(master.getTypes())
                    .orElseGet(Collections::emptyList).stream()
                    .filter(type -> !Boolean.TRUE.equals(type.getDisabled()))
                    .sorted(Comparator.comparing(
                            QcDivisionMaster.DivisionType::getDisplayOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            if (!types.isEmpty()) {
                types.stream()
                        .filter(type -> QcDivisionCatalog.canonicalName(type.getDivisionId()) != null)
                        .map(type -> new DivisionSeed(
                                type.getDivisionId(),
                                displayName(type.getDivisionName(), type.getDivisionId())))
                        .forEach(seeds::add);
            } else if (QcDivisionCatalog.canonicalName(master.getDivisionId()) != null) {
                seeds.add(new DivisionSeed(
                        master.getDivisionId(),
                        displayName(master.getDivisionName(), master.getDivisionId())));
            }
        }
        return seeds;
    }

    private Map<Long, String> divisionNamesById() {
        Map<Long, String> names = new LinkedHashMap<>();
        for (QcDivisionMaster master : masterRepository.findAllByOrderByDisplayOrderAsc()) {
            names.put(master.getDivisionId(), master.getDivisionName());
            Optional.ofNullable(master.getTypes())
                    .orElseGet(Collections::emptyList)
                    .forEach(type -> names.put(type.getDivisionId(), type.getDivisionName()));
        }
        return names;
    }

    private String resolveDivisionName(
            Long divisionId,
            String fallback,
            Map<Long, String> divisionNames) {
        String divisionName = divisionNames.get(divisionId);
        if (divisionName == null || divisionName.isBlank()) {
            String canonical = QcDivisionCatalog.canonicalName(divisionId);
            return canonical != null ? canonical : fallback;
        }
        // Prefer catalog rename for id 10 even if master still says Propellant Properties / QC label.
        if (Objects.equals(divisionId, QcDivisionCatalog.QC_ID)) {
            return QcDivisionCatalog.QC;
        }
        return divisionName;
    }

    private String displayName(String configuredName, Long divisionId) {
        if (Objects.equals(divisionId, QcDivisionCatalog.QC_ID)) {
            return QcDivisionCatalog.QC;
        }
        return configuredName == null || configuredName.isBlank()
                ? QcDivisionCatalog.canonicalName(divisionId) : configuredName;
    }

    private enum SeedMode {
        MAIN,
        EXPERIMENTAL,
        QUALIFICATION
    }

    private record DivisionSeed(Long divisionId, String divisionName) {
    }
}
