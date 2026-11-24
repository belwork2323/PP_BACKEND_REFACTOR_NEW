package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.batch.BatchMetadata;
import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.mixing.*;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingDetailsDto;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormRequest;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormResponse;
import in.co.bel.acempp.dto.mixing.mixingform.PremixDto;
import in.co.bel.acempp.dto.mixing.mixingform.TrialDetailsDto;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParameterDto;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.dto.rawmaterialpreparation.*;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.batch.BatchIdentificationSheet;
import in.co.bel.acempp.entity.batch.BuildingMaster;
import in.co.bel.acempp.entity.batch.MixerMaster;
import in.co.bel.acempp.entity.manufacturing.Mixing;
import in.co.bel.acempp.entity.manufacturing.MixingCycleMaster;
import in.co.bel.acempp.entity.manufacturing.QualityCheckMaster;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.BuildingMasterRepository;
import in.co.bel.acempp.repository.MixerMasterRepository;
import in.co.bel.acempp.repository.manufacturing.MixingCycleRepository;
import in.co.bel.acempp.repository.manufacturing.QualityCheckRepository;
import in.co.bel.acempp.repository.manufacturing.MixingRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.service.BatchService;
import in.co.bel.acempp.service.ValidateMaster;
import in.co.bel.acempp.service.batch.MainBatchGateValidator;
import in.co.bel.acempp.service.batch.MainBatchStageProgressHelper;
import in.co.bel.acempp.service.FileReadOnlyService;
import in.co.bel.acempp.service.FormIdGeneratorService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MixingService {

    private final BatchRepository batchRepository;
    private final MixingRepository mixingRepository;
    private final MixingCycleRepository mixingCycleRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final UserRepository userRepository;
    private final BatchService batchService;
    private final MainBatchGateValidator mainBatchGateValidator;
    private final FormIdGeneratorService formIdGenerator;
    private final FileReadOnlyService fileReadOnlyService;
    private final BuildingMasterRepository buildingMasterRepository;
    private final MixerMasterRepository mixerMasterRepository;
    private final ValidateMaster validateMaster;
    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;

    public MixingFormResponse createForm(MixingFormRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());

        Batch batch = batchRepository.findByBatchIdAndIsDeletedFalse(request.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "No batch exists for batchId "
                        + request.getBatchId()));
        batchService.assertStageUnlockedAndActive(
                batch, request.getSubDepartmentId(), "Mixing");

        if (mixingRepository.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Mixing form already submitted for this batch");
        }

        if (request.getMixingDetails() == null || request.getMixingDetails().getStages() == null
                || request.getMixingDetails().getStages().isEmpty()) {
            throw new AcemException(ErrorCode.MISSING_MANDATORY_FIELD,
                    "At least one stage with one premix entry is required");
        }
        boolean hasPremix = request.getMixingDetails()
                .getStages()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(stage ->
                        stage.getPremixes() != null
                                && !stage.getPremixes().isEmpty());
        if (!hasPremix) {
            throw new AcemException(
                    ErrorCode.MISSING_MANDATORY_FIELD,
                    "At least one stage with one premix entry is required");
        }

        int totalRequiredPremixes = batch.getIdentificationSheet().getNumberOfPremix();

        for (MixingStageDto stage : request.getMixingDetails().getStages()) {
            if (stage.getPremixes() != null) {
                for (PremixDto premix : stage.getPremixes()) {
                    if (premix.getMixerConfiguration() != null) {
                        validateMaster.validateAndResolveBuildingNumber(premix.getMixerConfiguration());
                        validateMaster.validateAndResolveMixer(premix.getMixerConfiguration());

                    }
                    if (premix.getMixingCycle() != null) {
                        validateMaster.validateAndResolveMixingCycle(premix.getMixingCycle());
                    }
                }
            }
        }
        MixingDetailsDto completeDetails =
                buildCompleteMixingDetails(
                        request.getMixingDetails(),
                        totalRequiredPremixes, true
                );

        List<PremixStatusResponse> premixStatuses = buildPremixStatuses(
                completeDetails,
                StageType.PREMIX,
                totalRequiredPremixes
        );

        List<PremixStatusResponse> finalMixStatuses = buildPremixStatuses(
                completeDetails,
                StageType.FINAL_MIX,
                totalRequiredPremixes
        );

        Status batchStatus = calculateBatchStatus(premixStatuses, finalMixStatuses);

        String formId = formIdGenerator.generate(
                "mixing",
                "formId",
                "MIX",
                3
        );

        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));

        PersonInfo createdByInfo =
                PersonInfo.builder()
                        .id(user.getUserId())
                        .fullName(user.getUsername())
                        .build();

        LocalDateTime now = LocalDateTime.now();

        Mixing mixing = Mixing.builder()
                .formId(formId)
                .batchId(request.getBatchId())
                .subDepartmentId(request.getSubDepartmentId())
                .batchType(batch.getBatchType())
                .formSubmissionType(request.getFormSubmissionType())
                .status(batchStatus)
                .mixingDetails(completeDetails)
                .submittedBy(createdByInfo)
                .submittedAt(now)
                .isDeleted(false)
                .build();

        mixingRepository.save(mixing);

        normalizeMixingStatusesForBatchSync(
                batch, request.getSubDepartmentId(), premixStatuses, finalMixStatuses);
        batchService.syncStageProgress(
                batch.getBatchId(),
                request.getSubDepartmentId(),
                batchStatus,
                premixStatuses,
                finalMixStatuses,
                null,
                (request.getFormSubmissionType() == Status.SUBMIT
                        || hasSubmittedMixItem(request.getMixingDetails())) ? createdByInfo : null,
                now,
                false,
                null,
                null);
        log.info("Mixing form {} successfully created for batch {} by user {}", formId, request.getBatchId(), userId);

        return MixingFormResponse.builder()
                .formId(formId)
                .batchId(request.getBatchId())
                .batchType(batch.getBatchType())
                .formSubmissionType(request.getFormSubmissionType())
                .status(batchStatus)
                .premixCount(totalRequiredPremixes)
                .pendingPremixCount(countStatus(premixStatuses, Status.WAITING_FOR_APPROVAL))
                .approvedPremixCount(countStatus(premixStatuses, Status.APPROVED))
                .rejectedPremixCount(countStatus(premixStatuses, Status.REJECTED))
                .inProgressPremixCount(countStatus(premixStatuses, Status.IN_PROGRESS))
                .totalPremixCount(totalRequiredPremixes)
                .premixStatuses(premixStatuses)
                .finalMixCount(totalRequiredPremixes)
                .pendingFinalMixCount(countStatus(finalMixStatuses, Status.WAITING_FOR_APPROVAL))
                .approvedFinalMixCount(countStatus(finalMixStatuses, Status.APPROVED))
                .rejectedFinalMixCount(countStatus(finalMixStatuses, Status.REJECTED))
                .inProgressFinalMixCount(countStatus(finalMixStatuses, Status.IN_PROGRESS))
                .totalFinalMixCount(totalRequiredPremixes)
                .finalMixStatuses(finalMixStatuses)
                .submittedBy(mixing.getSubmittedBy() != null
                        ? mixing.getSubmittedBy()
                        : null)
                .submittedAt(mixing.getSubmittedAt())
                .lastUpdatedBy(mixing.getUpdatedBy() != null
                        ? mixing.getUpdatedBy().getFullName()
                        : null)
                .lastUpdatedAt(mixing.getUpdatedAt())
                .build();
    }

    public MixingFormResponse updateForm(MixingFormRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());

        Mixing mixingForm = mixingRepository.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "No mixing form exists for formId "
                        + request.getFormId()));

        if (!Objects.equals(mixingForm.getBatchId(), request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The mixing form does not belong to the supplied batchId"
            );
        }

        Batch batch = batchRepository.findByBatchIdAndIsDeletedFalse(mixingForm.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId: " + mixingForm.getBatchId()));
        batchService.assertStageUnlockedAndActive(
                batch, mixingForm.getSubDepartmentId(), "Mixing");

        Status status = mixingForm.getStatus();

        if (status == Status.WAITING_FOR_APPROVAL
                || status == Status.APPROVED) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Submitted forms cannot be modified"
            );
        }

        if (request.getMixingDetails() == null || request.getMixingDetails().getStages() == null
                || request.getMixingDetails().getStages().isEmpty()) {

            throw new AcemException(ErrorCode.MISSING_MANDATORY_FIELD,
                    "At least one stage with one premix entry is required");
        }

        boolean hasPremix = request.getMixingDetails()
                .getStages()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(stage ->
                        stage.getPremixes() != null
                                && !stage.getPremixes().isEmpty());

        if (!hasPremix) {
            throw new AcemException(
                    ErrorCode.MISSING_MANDATORY_FIELD,
                    "At least one stage with one premix entry is required");
        }

        int totalRequiredPremixes = batch.getIdentificationSheet().getNumberOfPremix();
        boolean isFinalFormSubmit = request.getFormSubmissionType() == Status.SUBMIT;

        for (MixingStageDto stage : request.getMixingDetails().getStages()) {
            if (stage.getPremixes() != null) {
                for (PremixDto premix : stage.getPremixes()) {
                    if (premix.getMixerConfiguration() != null) {
                        validateMaster.validateAndResolveBuildingNumber(premix.getMixerConfiguration());
                        validateMaster.validateAndResolveMixer(premix.getMixerConfiguration());

                    }
                    if (premix.getMixingCycle() != null) {
                        validateMaster.validateAndResolveMixingCycle(premix.getMixingCycle());
                    }

                }
            }
        }

        MixingDetailsDto completeDetails =
                buildCompleteMixingDetails(
                        mixingForm.getMixingDetails(),
                        totalRequiredPremixes, false
                );

        mergeRequestedMixingDetails(
                batch,
                completeDetails,
                request.getMixingDetails(),
                totalRequiredPremixes
        );

        mixingForm.setMixingDetails(completeDetails);
        mixingForm.setRejectionReason(resolveFormRejectionReasonFromMixingDetails(completeDetails));
        List<PremixStatusResponse> premixStatuses =
                buildPremixStatuses(
                        completeDetails,
                        StageType.PREMIX,
                        totalRequiredPremixes
                );

        List<PremixStatusResponse> finalMixStatuses =
                buildPremixStatuses(
                        completeDetails,
                        StageType.FINAL_MIX,
                        totalRequiredPremixes
                );

        Status batchStatus = calculateBatchStatus(premixStatuses, finalMixStatuses);

        mixingForm.setFormSubmissionType(request.getFormSubmissionType());
        mixingForm.setStatus(batchStatus);
        mixingForm.setMixingDetails(completeDetails);

        User user = userRepository.findByUserId(userId).orElseThrow(() ->
                new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));

        LocalDateTime now = LocalDateTime.now();
        PersonInfo updatedBy = PersonInfo.builder()
                .id(userId)
                .fullName(user.getUsername())
                .build();

        mixingForm.setUpdatedBy(updatedBy);
        mixingForm.setUpdatedAt(now);

        mixingRepository.save(mixingForm);

        normalizeMixingStatusesForBatchSync(
                batch, mixingForm.getSubDepartmentId(), premixStatuses, finalMixStatuses);
        batchService.syncStageProgress(
                batch.getBatchId(),
                mixingForm.getSubDepartmentId(),
                batchStatus,
                premixStatuses,
                finalMixStatuses,
                null,
                (isFinalFormSubmit || hasSubmittedMixItem(request.getMixingDetails())) ? updatedBy : null,
                now,
                false,
                mixingForm.getRemarks(),
                mixingForm.getRejectionReason());

        log.info("Mixing form {} updated by user {}", request.getFormId(), userId);
        return MixingFormResponse.builder()
                .formId(mixingForm.getFormId())
                .batchId(mixingForm.getBatchId())
                .batchType(mixingForm.getBatchType())
                .formSubmissionType(mixingForm.getFormSubmissionType())
                .status(mixingForm.getStatus())
                .premixCount(totalRequiredPremixes)
                .pendingPremixCount(countStatus(premixStatuses, Status.WAITING_FOR_APPROVAL))
                .approvedPremixCount(countStatus(premixStatuses, Status.APPROVED))
                .rejectedPremixCount(countStatus(premixStatuses, Status.REJECTED))
                .inProgressPremixCount(countStatus(premixStatuses, Status.IN_PROGRESS))
                .totalPremixCount(totalRequiredPremixes)
                .premixStatuses(premixStatuses)
                .finalMixCount(totalRequiredPremixes)
                .pendingFinalMixCount(countStatus(finalMixStatuses, Status.WAITING_FOR_APPROVAL))
                .approvedFinalMixCount(countStatus(finalMixStatuses, Status.APPROVED))
                .rejectedFinalMixCount(countStatus(finalMixStatuses, Status.REJECTED))
                .inProgressFinalMixCount(countStatus(finalMixStatuses, Status.IN_PROGRESS))
                .totalFinalMixCount(totalRequiredPremixes)
                .finalMixStatuses(finalMixStatuses)
                .submittedBy(mixingForm.getSubmittedBy() != null
                        ? mixingForm.getSubmittedBy()
                        : null)
                .submittedAt(mixingForm.getSubmittedAt())
                .lastUpdatedBy(mixingForm.getUpdatedBy() != null
                        ? mixingForm.getUpdatedBy().getFullName()
                        : null)
                .lastUpdatedAt(mixingForm.getUpdatedAt())
                .build();
    }

    public MixingFormResponse getFormDetails(MixingFormRequest request) {

        Mixing mixingForm = mixingRepository.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND,
                        "No mixing form exists for formId " + request.getFormId()));
        Batch batch = batchRepository.findByBatchIdAndIsDeletedFalse(mixingForm.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch not found"));

        int totalRequiredPremixes =
                batch.getIdentificationSheet().getNumberOfPremix();

        List<PremixStatusResponse> premixStatuses =
                buildPremixStatuses(
                        mixingForm.getMixingDetails(),
                        StageType.PREMIX,
                        totalRequiredPremixes
                );

        List<PremixStatusResponse> finalMixStatuses =
                buildPremixStatuses(
                        mixingForm.getMixingDetails(),
                        StageType.FINAL_MIX,
                        totalRequiredPremixes
                );

        MixingDetailsDto responseDetails = mixingForm.getMixingDetails();
        enrichMixingDetails(responseDetails);

        return MixingFormResponse.builder()
                .formId(mixingForm.getFormId())
                .batchId(mixingForm.getBatchId())
                .batchType(batch.getBatchType())
                .subDepartmentId(mixingForm.getSubDepartmentId())
                .formSubmissionType(mixingForm.getFormSubmissionType())
                .status(mixingForm.getStatus())
                .premixCount(totalRequiredPremixes)
                .pendingPremixCount(countStatus(premixStatuses, Status.WAITING_FOR_APPROVAL))
                .approvedPremixCount(countStatus(premixStatuses, Status.APPROVED))
                .rejectedPremixCount(countStatus(premixStatuses, Status.REJECTED))
                .inProgressPremixCount(countStatus(premixStatuses, Status.IN_PROGRESS))
                .totalPremixCount(totalRequiredPremixes)
                .premixStatuses(premixStatuses)
                .finalMixCount(totalRequiredPremixes)
                .pendingFinalMixCount(countStatus(finalMixStatuses, Status.WAITING_FOR_APPROVAL))
                .approvedFinalMixCount(countStatus(finalMixStatuses, Status.APPROVED))
                .rejectedFinalMixCount(countStatus(finalMixStatuses, Status.REJECTED))
                .inProgressFinalMixCount(countStatus(finalMixStatuses, Status.IN_PROGRESS))
                .totalFinalMixCount(totalRequiredPremixes)
                .finalMixStatuses(finalMixStatuses)
                .submittedBy(mixingForm.getSubmittedBy())
                .submittedAt(mixingForm.getSubmittedAt() != null
                        ? mixingForm.getSubmittedAt()
                        : null)
                .lastUpdatedBy(mixingForm.getUpdatedBy() != null
                        ? mixingForm.getUpdatedBy().getFullName()
                        : null)
                .lastUpdatedAt(mixingForm.getUpdatedAt() != null
                        ? mixingForm.getUpdatedAt()
                        : null)
                .mixingDetails(responseDetails)
                .build();
    }

    private void enrichMixingDetails(MixingDetailsDto details) {
        if (details == null || details.getStages() == null) {
            return;
        }

        Set<String> cycleCodes = new LinkedHashSet<>();
        Set<Long> cycleIds = new LinkedHashSet<>();
        Set<String> parameterIds = new LinkedHashSet<>();

        for (MixingStageDto stage : details.getStages()) {
            if (stage == null || stage.getPremixes() == null) {
                continue;
            }
            for (PremixDto premix : stage.getPremixes()) {
                if (premix == null) {
                    continue;
                }
                MixingCycleDto cycle = premix.getMixingCycle();
                if (cycle != null) {
                    if (cycle.getMixingCycleCode() != null
                            && !cycle.getMixingCycleCode().isBlank()) {
                        cycleCodes.add(cycle.getMixingCycleCode());
                    }
                    if (cycle.getMixingCycleId() != null) {
                        cycleIds.add(cycle.getMixingCycleId());
                    }
                }
                if (premix.getQualityChecks() != null) {
                    premix.getQualityChecks().stream()
                            .filter(Objects::nonNull)
                            .map(QualityCheckParameterDto::getParameterId)
                            .filter(Objects::nonNull)
                            .filter(parameterId -> !parameterId.isBlank())
                            .forEach(parameterIds::add);
                }
            }
        }

        Map<String, MixingCycleMaster> cyclesByCode = new LinkedHashMap<>();
        if (!cycleCodes.isEmpty()) {
            mixingCycleRepository.findByMixingCycleCodeIn(cycleCodes).stream()
                    .filter(Objects::nonNull)
                    .filter(master -> master.getMixingCycleCode() != null)
                    .forEach(master -> cyclesByCode.putIfAbsent(
                            master.getMixingCycleCode(), master));
        }

        Map<Long, MixingCycleMaster> cyclesById = new LinkedHashMap<>();
        if (!cycleIds.isEmpty()) {
            mixingCycleRepository.findAllById(cycleIds).stream()
                    .filter(Objects::nonNull)
                    .filter(master -> master.getId() != null)
                    .forEach(master -> cyclesById.putIfAbsent(master.getId(), master));
        }

        Map<String, QualityCheckParams> qualityChecksById = new LinkedHashMap<>();
        for (MixingCycleMaster master : cyclesByCode.values()) {
            indexQualityChecksFromCycle(master, parameterIds, qualityChecksById);
        }
        for (MixingCycleMaster master : cyclesById.values()) {
            indexQualityChecksFromCycle(master, parameterIds, qualityChecksById);
        }
        if (!parameterIds.isEmpty()) {
            qualityCheckRepository.findByQualityCheckParameterIds(parameterIds).stream()
                    .filter(Objects::nonNull)
                    .map(QualityCheckMaster::getQualityChecks)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .filter(parameter -> parameter.getParameterId() != null
                            && parameterIds.contains(parameter.getParameterId()))
                    .forEach(parameter -> qualityChecksById.putIfAbsent(
                            parameter.getParameterId(), parameter));
        }

        for (MixingStageDto stage : details.getStages()) {
            if (stage == null || stage.getPremixes() == null) {
                continue;
            }
            for (PremixDto premix : stage.getPremixes()) {
                if (premix == null) {
                    continue;
                }
                enrichProcessParticulars(
                        stage.getStageType(), premix, cyclesByCode, cyclesById);
                enrichQualityChecks(premix, qualityChecksById);
            }
        }
    }

    private void enrichProcessParticulars(
            StageType stageType,
            PremixDto premix,
            Map<String, MixingCycleMaster> cyclesByCode,
            Map<Long, MixingCycleMaster> cyclesById) {
        if (premix.getProcessParticulars() == null) {
            return;
        }

        MixingCycleDto cycle = premix.getMixingCycle();
        MixingCycleMaster master = null;
        if (cycle != null && cycle.getMixingCycleCode() != null
                && !cycle.getMixingCycleCode().isBlank()) {
            master = cyclesByCode.get(cycle.getMixingCycleCode());
        }
        if (master == null && cycle != null && cycle.getMixingCycleId() != null) {
            master = cyclesById.get(cycle.getMixingCycleId());
        }

        Map<Long, String> operationNamesById = new LinkedHashMap<>();
        if (master != null && master.getCycles() != null) {
            List<MixingCycleMaster.Operation> operations = stageType == StageType.PREMIX
                    ? master.getCycles().getPremixOperations()
                    : stageType == StageType.FINAL_MIX
                    ? master.getCycles().getFinalMixOperations()
                    : Collections.emptyList();
            if (operations != null) {
                operations.stream()
                        .filter(Objects::nonNull)
                        .filter(operation -> operation.getOperationId() != null)
                        .forEach(operation -> operationNamesById.putIfAbsent(
                                operation.getOperationId(), operation.getOperationName()));
            }
        }

        premix.getProcessParticulars().stream()
                .filter(Objects::nonNull)
                .forEach(row -> row.setOperationName(row.getOperationId() == null
                        ? null
                        : operationNamesById.get(row.getOperationId().longValue())));
    }

    private void enrichQualityChecks(
            PremixDto premix,
            Map<String, QualityCheckParams> qualityChecksById) {
        if (premix.getQualityChecks() == null) {
            return;
        }
        premix.getQualityChecks().stream()
                .filter(Objects::nonNull)
                .forEach(parameter -> {
                    QualityCheckParams master = qualityChecksById.get(parameter.getParameterId());
                    parameter.setParameterName(master == null ? null : master.getParameterName());
                    parameter.setSpecification(master == null ? null : master.getSpecification());
                });
    }

    private static void indexQualityChecksFromCycle(
            MixingCycleMaster master,
            Collection<String> parameterIds,
            Map<String, QualityCheckParams> qualityChecksById) {
        if (master == null || master.getCycles() == null || parameterIds == null || parameterIds.isEmpty()) {
            return;
        }
        Stream.of(
                        master.getCycles().getPremixQualityChecks(),
                        master.getCycles().getFinalMixQualityChecks())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(parameter -> parameter.getParameterId() != null
                        && parameterIds.contains(parameter.getParameterId()))
                .forEach(parameter -> qualityChecksById.putIfAbsent(
                        parameter.getParameterId(), parameter));
    }

    private void updateApprovedMixingMetadata(
            Batch batch,
            StageType stageType,
            PremixDto approvedEntry) {

        if (batch.getIdentificationSheet() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Batch identification sheet is not available"
            );
        }

        BatchIdentificationSheet identificationSheet =
                batch.getIdentificationSheet();

        BatchMetadata batchMetadata =
                identificationSheet.getMetadata();

        if (batchMetadata == null) {
            batchMetadata = new BatchMetadata();
            identificationSheet.setMetadata(batchMetadata);
        }

        MixingMetadata mixingMetadata =
                batchMetadata.getMixing();

        if (mixingMetadata == null) {
            mixingMetadata = MixingMetadata.builder()
                    .stages(new ArrayList<>())
                    .build();

            batchMetadata.setMixing(mixingMetadata);
        }

        if (mixingMetadata.getStages() == null) {
            mixingMetadata.setStages(new ArrayList<>());
        }

        MixingStageMetadata stageMetadata =
                mixingMetadata.getStages().stream()
                        .filter(Objects::nonNull)
                        .filter(stage ->
                                stage.getStageType() == stageType
                        )
                        .findFirst()
                        .orElse(null);

        if (stageMetadata == null) {
            stageMetadata = MixingStageMetadata.builder()
                    .stageType(stageType)
                    .premixes(new ArrayList<>())
                    .build();

            mixingMetadata.getStages().add(stageMetadata);
        }

        if (stageMetadata.getPremixes() == null) {
            stageMetadata.setPremixes(new ArrayList<>());
        }
        ApprovedMixMetadata approvedMetadata =
                mapToApprovedMixMetadata(approvedEntry);

        List<ApprovedMixMetadata> approvedPremixes =
                stageMetadata.getPremixes();

        int existingIndex = IntStream.range(0, approvedPremixes.size())
                .filter(index -> {
                    ApprovedMixMetadata existing = approvedPremixes.get(index);

                    return existing != null
                            && Objects.equals(
                            existing.getPremixNo(),
                            approvedEntry.getPremixNo()
                    );
                })
                .findFirst()
                .orElse(-1);

        if (existingIndex >= 0) {
            approvedPremixes.set(existingIndex, approvedMetadata);
        } else {
            approvedPremixes.add(approvedMetadata);
        }

        approvedPremixes.sort(
                Comparator.comparing(
                        ApprovedMixMetadata::getPremixNo,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
        );
    }

    private ApprovedMixMetadata mapToApprovedMixMetadata(
            PremixDto entry) {

        MixerConfigurationDto mixer =
                entry.getMixerConfiguration();

        TrialDetailsDto trial =
                entry.getTrialDetails();

        MixDetailsDto mix =
                entry.getMixDetails();

        MixingCycleDto cycle =
                resolveMixingCycle(entry.getMixingCycle());

        return ApprovedMixMetadata.builder()
                .premixNo(entry.getPremixNo())

                .mixerId(
                        mixer != null
                                ? mixer.getMixerId()
                                : null
                )
                .buildingNo(
                        mixer != null
                                ? mixer.getBldgNo()
                                : null
                )
                .bowlId(
                        mixer != null
                                ? mixer.getBowlId()
                                : null
                )

                .trialDateTime(
                        trial != null
                                ? trial.getTrialDateTime()
                                : null
                )
                .observations(
                        trial != null
                                ? trial.getObservations()
                                : null
                )

                .mixDate(
                        mix != null
                                ? mix.getMixDate()
                                : null
                )
                .mixQuantity(
                        mix != null
                                ? mix.getMixQuantity()
                                : null
                )

                .mixingCycle(
                        cycle != null
                                ? MixingCycleDto.builder()
                                .mixingCycleId(cycle.getMixingCycleId())
                                .mixingCycleCode(
                                        cycle.getMixingCycleCode()
                                )
                                .mixingCycleName(
                                        cycle.getMixingCycleName()
                                )
                                .build()
                                : null
                )
                .build();
    }

    private MixingCycleDto resolveMixingCycle(MixingCycleDto cycle) {

        if (cycle == null) {
            return null;
        }

        MixingCycleMaster master;

        if (cycle.getMixingCycleCode() != null
                && !cycle.getMixingCycleCode().isBlank()) {

            master = mixingCycleRepository
                    .findByMixingCycleCode(cycle.getMixingCycleCode())
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Mixing cycle not found: "
                                    + cycle.getMixingCycleCode()
                    ));

        } else if (cycle.getMixingCycleId() != null) {

            master = mixingCycleRepository
                    .findById(cycle.getMixingCycleId())
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Mixing cycle not found for id: "
                                    + cycle.getMixingCycleId()
                    ));

        } else {
            throw new AcemException(
                    ErrorCode.MISSING_MANDATORY_FIELD,
                    "Mixing cycle id or code is required"
            );
        }

        return MixingCycleDto.builder()
                .mixingCycleId(master.getId())
                .mixingCycleCode(master.getMixingCycleCode())
                .mixingCycleName(master.getMixingCycleName())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public PremixApprovalResponse changeStatusOfPremix(PremixApprovalRequest request, String approverName) {

        if (request.getActionType() != Status.APPROVED
                && request.getActionType() != Status.REJECTED) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "actionType must be APPROVED or REJECTED"
            );
        }

        if (request.getActionType() == Status.REJECTED
                && (request.getRejectionReason() == null
                || request.getRejectionReason().isBlank())) {
            throw new AcemException(
                    ErrorCode.REJECTION_REASON_REQUIRED,
                    "rejectionReason is required for rejected mixing item"
            );
        }

        Mixing form = mixingRepository
                .findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Mixing form not found"
                ));

        if (!Objects.equals(
                form.getSubDepartmentId(),
                request.getSubDepartmentId())) {

            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The mixing form does not belong to the supplied subDepartmentId"
            );
        }

        if (form.getMixingDetails() == null
                || form.getMixingDetails().getStages() == null) {

            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Mixing stages are not available"
            );
        }

        PremixDto selectedItem = form.getMixingDetails()
                .getStages()
                .stream()
                .filter(stage -> Objects.equals(
                        stage.getStageType(),
                        request.getStageType()
                ))
                .flatMap(stage -> stage.getPremixes() != null
                        ? stage.getPremixes().stream()
                        : Stream.empty())
                .filter(item -> Objects.equals(
                        item.getPremixNo(),
                        request.getPremixNo()
                ))
                .findFirst()
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        request.getStageType()
                                + " " + request.getPremixNo()
                                + " not found in the mixing form"
                ));

        if (selectedItem.getPremixSubmissionStatus()
                != Status.WAITING_FOR_APPROVAL) {

            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    request.getStageType()
                            + " " + request.getPremixNo()
                            + " is not waiting for approval"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        selectedItem.setPremixSubmissionStatus(
                request.getActionType()
        );

        selectedItem.setRemarks(request.getRemarks());

        selectedItem.setRejectionReason(
                request.getActionType() == Status.REJECTED
                        ? request.getRejectionReason()
                        : null
        );

        selectedItem.setActionBy(
                PersonInfo.builder()
                        .fullName(approverName)
                        .build()
        );

        selectedItem.setActionAt(now);

        Batch batch = batchRepository
                .findByBatchIdAndIsDeletedFalse(form.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "Batch not found"
                ));
        if (request.getActionType() == Status.APPROVED) {
            updateApprovedMixingMetadata(
                    batch,
                    request.getStageType(),
                    selectedItem
            );
        }
        int totalRequiredItems =
                batch.getIdentificationSheet().getNumberOfPremix();

        List<PremixStatusResponse> premixStatuses =
                buildPremixStatuses(
                        form.getMixingDetails(),
                        StageType.PREMIX,
                        totalRequiredItems
                );

        List<PremixStatusResponse> finalMixStatuses =
                buildPremixStatuses(
                        form.getMixingDetails(),
                        StageType.FINAL_MIX,
                        totalRequiredItems
                );

        Status batchStatus = calculateBatchStatus(
                premixStatuses,
                finalMixStatuses
        );

        form.setStatus(batchStatus);

        PersonInfo approver = PersonInfo.builder()
                .fullName(approverName)
                .build();
        form.setUpdatedBy(approver);

        form.setUpdatedAt(now);
        form.setRemarks(request.getRemarks());

        if (batchStatus == Status.COMPLETELY_APPROVED) {
            form.setFormSubmissionType(Status.SUBMIT);
            form.setSubmittedBy(approver);
            form.setSubmittedAt(now);
        }

        form.setRejectionReason(resolveFormRejectionReasonFromMixingDetails(form.getMixingDetails()));

        if (request.getActionType() == Status.APPROVED && batchStatus == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }


        if (request.getActionType() == Status.APPROVED) {
            try {
                Map<String, Object> blockchainPayload = buildPremixBlockchainPayload(form, selectedItem, request, batchStatus, request.getStageType() , approverName , now);
                approvalBlockchainAssetService.createAssetOnBlockchain(
                        form.getSubDepartmentId(),
                        form.getBatchId(),
                        form.getFormId(),
                        blockchainPayload,
                        (request.getStageType() + "" + request.getPremixNo()),
                        approverName);
            } catch (Exception e) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new AcemException(ErrorCode.INVALID_REQUEST, "Blockchain sync failed: " + e.getMessage());
            }
        }
        mixingRepository.save(form);
        if (request.getActionType() == Status.APPROVED) {
            batchRepository.save(batch);
        }
        normalizeMixingStatusesForBatchSync(
                batch, form.getSubDepartmentId(), premixStatuses, finalMixStatuses);
        batchService.syncStageApproval(
                form.getBatchId(),
                form.getSubDepartmentId(),
                batchStatus,
                premixStatuses,
                finalMixStatuses,
                null,
                request.getActionType() == Status.APPROVED
                        && isApprovedMixPair(
                        premixStatuses,
                        finalMixStatuses,
                        request.getPremixNo()),
                form.getRemarks(),
                form.getRejectionReason(),
                approverName,
                now
        );

        List<PremixStatusResponse> selectedStageStatuses =
                request.getStageType() == StageType.PREMIX
                        ? premixStatuses
                        : finalMixStatuses;

        int pending = (int) selectedStageStatuses.stream()
                .filter(item -> item.getPremixSubmissionStatus()
                        == Status.WAITING_FOR_APPROVAL)
                .count();

        int approved = (int) selectedStageStatuses.stream()
                .filter(item -> item.getPremixSubmissionStatus()
                        == Status.APPROVED)
                .count();

        int rejected = (int) selectedStageStatuses.stream()
                .filter(item -> item.getPremixSubmissionStatus()
                        == Status.REJECTED)
                .count();

        return PremixApprovalResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .premixNo(request.getPremixNo())
                .actionType(request.getActionType())
                .status(request.getActionType())
                .premixSubmissionStatus(
                        selectedItem.getPremixSubmissionStatus()
                )
                .batchStatus(batchStatus)
                .allPremixesApproved(
                        approved == totalRequiredItems
                )
                .pendingPremixCount(pending)
                .approvedPremixCount(approved)
                .rejectedPremixCount(rejected)
                .totalPremixCount(totalRequiredItems)
                .remarks(selectedItem.getRemarks())
                .rejectionReason(
                        selectedItem.getRejectionReason()
                )
                .actionBy(approverName)
                .actionAt(now)
                .build();
    }

    private Map<String, Object> buildPremixBlockchainPayload(
            Mixing form,
            PremixDto selectedItem,
            PremixApprovalRequest request,
            Status batchStatus,
            StageType stageType,
            String approverName,
            LocalDateTime approvalTime) {

        Map<String, Object> rootPayload = new LinkedHashMap<>();

        // 1. Audit / Metadata Object (Created & Approved info separated)
        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getSubmittedBy() != null) {
                auditMap.put("createdBy", form.getSubmittedBy().getFullName());
            }
            if (form.getSubmittedAt() != null) {
                auditMap.put("createdAt", form.getSubmittedAt());
            }
        }

        // Use passed approver details or fallback to form's updated fields if already set
        if (approverName != null) {
            auditMap.put("approvedBy", approverName);
            auditMap.put("approvedAt", approvalTime);
        } else if (form != null && form.getUpdatedBy() != null) {
            auditMap.put("approvedBy", form.getUpdatedBy().getFullName());
            auditMap.put("approvedAt", form.getUpdatedAt());
        }
        rootPayload.put("audit", auditMap);

        // 2. Premix Technical & Process Details Object
        Map<String, Object> premixMap = new LinkedHashMap<>();
        if (selectedItem != null) {
            premixMap.put("stageType", stageType != null ? stageType.name() : null);
            premixMap.put("premixNo", selectedItem.getPremixNo());

            if (selectedItem.getMixerConfiguration() != null) {
                var config = selectedItem.getMixerConfiguration();
                Map<String, Object> configMap = new LinkedHashMap<>();
                configMap.put("mixerId", config.getMixerId());
                configMap.put("bldgNo", config.getBldgNo());
                configMap.put("bowlId", config.getBowlId());
                premixMap.put("mixerConfiguration", configMap);
            }

            if (selectedItem.getTrialDetails() != null) {
                var trial = selectedItem.getTrialDetails();
                Map<String, Object> trialMap = new LinkedHashMap<>();
                trialMap.put("observations", trial.getObservations());
                premixMap.put("trialDetails", trialMap);
            }

            if (selectedItem.getMixDetails() != null) {
                var mix = selectedItem.getMixDetails();
                Map<String, Object> mixMap = new LinkedHashMap<>();
                mixMap.put("mixQuantity", mix.getMixQuantity());
                premixMap.put("mixDetails", mixMap);
            }

            if (selectedItem.getMixingCycle() != null) {
                var cycle = selectedItem.getMixingCycle();
                Map<String, Object> cycleMap = new LinkedHashMap<>();
                cycleMap.put("mixingCycleCode", cycle.getMixingCycleCode());
                cycleMap.put("mixingCycleName", cycle.getMixingCycleName());
                premixMap.put("mixingCycle", cycleMap);
            }

            premixMap.put("processParticulars", selectedItem.getProcessParticulars() == null ? Collections.emptyList() : selectedItem.getProcessParticulars());
            premixMap.put("qualityChecks", selectedItem.getQualityChecks() == null ? Collections.emptyList() : selectedItem.getQualityChecks());
        }
        rootPayload.put("premixDetails", premixMap);

        return rootPayload;
    }

    private boolean isApprovedMixPair(
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses,
            Integer premixNo) {
        if (premixNo == null) {
            return false;
        }
        boolean premixApproved = premixStatuses.stream()
                .anyMatch(status -> status.getPremixNo() == premixNo
                        && status.getPremixSubmissionStatus() == Status.APPROVED);
        boolean finalMixApproved = finalMixStatuses.stream()
                .anyMatch(status -> status.getPremixNo() == premixNo
                        && status.getPremixSubmissionStatus() == Status.APPROVED);
        return premixApproved && finalMixApproved;
    }


    private MixingDetailsDto buildCompleteMixingDetails(
            MixingDetailsDto source,
            int totalRequiredPremixes,
            boolean sourceIsRequest) {

        if (totalRequiredPremixes <= 0) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Total required premixes must be greater than zero"
            );
        }

        Map<StageType, Map<Integer, PremixDto>> entriesByStage =
                new EnumMap<>(StageType.class);

        entriesByStage.put(StageType.PREMIX, new HashMap<>());
        entriesByStage.put(StageType.FINAL_MIX, new HashMap<>());

        if (source != null && source.getStages() != null) {

            for (MixingStageDto stage : source.getStages()) {

                if (stage == null) {
                    continue;
                }

                StageType stageType = stage.getStageType();

                if (stageType == null) {
                    if (sourceIsRequest) {
                        throw new AcemException(
                                ErrorCode.MISSING_MANDATORY_FIELD,
                                "Stage type is required"
                        );
                    }
                    continue;
                }

                if (stageType != StageType.PREMIX
                        && stageType != StageType.FINAL_MIX) {
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "Only PREMIX and FINAL_MIX stages are allowed"
                    );
                }

                if (stage.getPremixes() == null) {
                    continue;
                }

                Map<Integer, PremixDto> stageEntries =
                        entriesByStage.get(stageType);

                for (PremixDto item : stage.getPremixes()) {

                    if (item == null) {
                        if (sourceIsRequest) {
                            throw new AcemException(
                                    ErrorCode.INVALID_REQUEST,
                                    "Premix entry cannot be null"
                            );
                        }
                        continue;
                    }

                    validatePremixNumber(
                            item,
                            totalRequiredPremixes
                    );

                    PremixDto existing = stageEntries.putIfAbsent(
                            item.getPremixNo(),
                            item
                    );

                    if (existing != null) {
                        throw new AcemException(
                                ErrorCode.INVALID_REQUEST,
                                "Duplicate entry for "
                                        + stageType
                                        + " premix "
                                        + item.getPremixNo()
                        );
                    }
                }
            }
        }

        List<MixingStageDto> completeStages = new ArrayList<>();

        for (StageType stageType :
                List.of(StageType.PREMIX, StageType.FINAL_MIX)) {

            Map<Integer, PremixDto> sourceEntries =
                    entriesByStage.get(stageType);

            List<PremixDto> completePremixes =
                    new ArrayList<>(totalRequiredPremixes);

            for (int premixNo = 1;
                 premixNo <= totalRequiredPremixes;
                 premixNo++) {

                PremixDto item = sourceEntries.get(premixNo);

                if (item == null) {
                    item = createUninitiatedPremix(premixNo);

                } else if (sourceIsRequest) {
                    item = prepareRequestedPremix(item);

                } else {
                    item = normalizePersistedPremix(item);
                }

                completePremixes.add(item);
            }

            completeStages.add(
                    MixingStageDto.builder()
                            .stageType(stageType)
                            .premixes(completePremixes)
                            .build()
            );
        }

        return MixingDetailsDto.builder()
                .stages(completeStages)
                .build();
    }

    private PremixDto createUninitiatedPremix(int premixNo) {

        return PremixDto.builder()
                .premixNo(premixNo)
                .premixSubmissionType(Status.DRAFT)
                .premixSubmissionStatus(Status.TO_BE_INITIATED)
                .build();
    }

    private static String resolveFormRejectionReasonFromMixingDetails(MixingDetailsDto details) {
        if (details == null || details.getStages() == null) {
            return null;
        }
        return details.getStages().stream()
                .filter(Objects::nonNull)
                .filter(stage -> stage.getPremixes() != null)
                .flatMap(stage -> stage.getPremixes().stream())
                .filter(p -> p != null && p.getPremixSubmissionStatus() == Status.REJECTED)
                .map(PremixDto::getRejectionReason)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElse(null);
    }

    private PremixDto prepareRequestedPremix(PremixDto item) {

        Status submissionType =
                item.getPremixSubmissionType();

        /*
         * A null submission type means the entry was included only
         * as a fixed-list placeholder. Do not persist its default data.
         */
        if (submissionType == null) {

            if (item.getPremixSubmissionStatus() != null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Submission status cannot be provided without submission type"
                );
            }

            return createUninitiatedPremix(
                    item.getPremixNo()
            );
        }

        /*
         * The UI can submit the complete PREMIX/FINAL_MIX grid. An untouched
         * slot is represented as DRAFT/TO_BE_INITIATED, so preserve it as an
         * uninitiated placeholder instead of treating the DRAFT value alone
         * as evidence that work has started.
         */
        if (isUninitiatedPlaceholder(item)) {
            return createUninitiatedPremix(item.getPremixNo());
        }

        if (submissionType == Status.DRAFT) {
            item.setPremixSubmissionStatus(
                    Status.IN_PROGRESS
            );

        } else if (submissionType == Status.SUBMIT) {
            item.setPremixSubmissionStatus(
                    Status.WAITING_FOR_APPROVAL
            );

        } else {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "premixSubmissionType must be DRAFT or SUBMIT"
            );
        }

        item.setActionBy(null);
        item.setActionAt(null);
        item.setRemarks(null);
        item.setRejectionReason(null);

        MixingCycleDto requestedCycle = item.getMixingCycle();
        if (requestedCycle != null
                && ((requestedCycle.getMixingCycleCode() != null
                && !requestedCycle.getMixingCycleCode().isBlank())
                || requestedCycle.getMixingCycleId() != null)) {
            item.setMixingCycle(resolveMixingCycle(requestedCycle));
        }

        return item;
    }

    private void mergeRequestedMixingDetails(
            Batch batch,
            MixingDetailsDto savedDetails,
            MixingDetailsDto requestedDetails,
            int totalRequiredPremixes) {

        if (requestedDetails == null
                || requestedDetails.getStages() == null
                || requestedDetails.getStages().isEmpty()) {
            throw new AcemException(
                    ErrorCode.MISSING_MANDATORY_FIELD,
                    "At least one mixing stage is required"
            );
        }

        for (MixingStageDto requestedStage : requestedDetails.getStages()) {

            if (requestedStage == null || requestedStage.getStageType() == null) {
                throw new AcemException(
                        ErrorCode.MISSING_MANDATORY_FIELD,
                        "Stage type is required"
                );
            }

            if (requestedStage.getPremixes() == null
                    || requestedStage.getPremixes().isEmpty()) {
                throw new AcemException(
                        ErrorCode.MISSING_MANDATORY_FIELD,
                        "At least one entry is required for stage "
                                + requestedStage.getStageType()
                );
            }

            MixingStageDto savedStage = savedDetails.getStages().stream()
                    .filter(Objects::nonNull)
                    .filter(stage ->
                            stage.getStageType() == requestedStage.getStageType())
                    .findFirst()
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "Stage not found: " + requestedStage.getStageType()
                    ));

            for (PremixDto requestedPremix : requestedStage.getPremixes()) {

                if (requestedPremix == null
                        || requestedPremix.getPremixNo() == null) {
                    throw new AcemException(
                            ErrorCode.MISSING_MANDATORY_FIELD,
                            "Premix number is required"
                    );
                }

                Integer premixNo = requestedPremix.getPremixNo();

                if (premixNo < 1 || premixNo > totalRequiredPremixes) {
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "Premix number must be between 1 and "
                                    + totalRequiredPremixes
                    );
                }

                int savedIndex = IntStream.range(
                                0,
                                savedStage.getPremixes().size()
                        )
                        .filter(index -> Objects.equals(
                                savedStage.getPremixes()
                                        .get(index)
                                        .getPremixNo(),
                                premixNo
                        ))
                        .findFirst()
                        .orElseThrow(() -> new AcemException(
                                ErrorCode.INVALID_REQUEST,
                                requestedStage.getStageType()
                                        + " entry " + premixNo + " not found"
                        ));

                PremixDto savedPremix =
                        savedStage.getPremixes().get(savedIndex);

                Status savedStatus =
                        savedPremix.getPremixSubmissionStatus();

                /*
                 * This entry is present only because the frontend sent
                 * the complete fixed list. It was not actually saved.
                 */
                if (savedStatus == Status.WAITING_FOR_APPROVAL
                        || savedStatus == Status.APPROVED) {
                    continue;
                }

                Status submissionType =
                        requestedPremix.getPremixSubmissionType();

                if (submissionType == null) {
                    continue;
                }

                if (isUninitiatedPlaceholder(requestedPremix)) {
                    savedStage.getPremixes().set(
                            savedIndex,
                            createUninitiatedPremix(premixNo)
                    );
                    continue;
                }

                if (submissionType == Status.DRAFT) {
                    mainBatchGateValidator.assertMixingPremixCanBeSubmitted(batch, premixNo);
                    requestedPremix.setPremixSubmissionStatus(
                            Status.IN_PROGRESS
                    );

                } else if (submissionType == Status.SUBMIT) {
                    mainBatchGateValidator.assertMixingPremixCanBeSubmitted(batch, premixNo);
                    requestedPremix.setPremixSubmissionStatus(
                            Status.WAITING_FOR_APPROVAL
                    );

                } else {
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "premixSubmissionType must be DRAFT or SUBMIT"
                    );
                }

                requestedPremix.setActionBy(null);
                requestedPremix.setActionAt(null);
                requestedPremix.setRemarks(null);
                requestedPremix.setRejectionReason(null);

                savedStage.getPremixes().set(savedIndex, requestedPremix);
            }
        }
    }

    private PremixDto normalizePersistedPremix(PremixDto item) {

        if (item.getPremixSubmissionStatus() == null
                || item.getPremixSubmissionStatus()
                == Status.TO_BE_INITIATED) {

            return createUninitiatedPremix(
                    item.getPremixNo()
            );
        }

        return item;
    }

    private boolean isUninitiatedPlaceholder(PremixDto item) {
        return item.getPremixSubmissionType() == Status.DRAFT
                && item.getPremixSubmissionStatus() == Status.TO_BE_INITIATED;
    }

    private void validatePremixNumber(
            PremixDto item,
            int totalRequiredPremixes) {

        if (item == null
                || item.getPremixNo() == null
                || item.getPremixNo() < 1
                || item.getPremixNo() > totalRequiredPremixes) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "premixNo must be between 1 and " + totalRequiredPremixes
            );
        }
    }

    private long countStatus(
            List<PremixStatusResponse> statuses,
            Status status) {

        return statuses.stream()
                .filter(item -> item.getPremixSubmissionStatus() == status)
                .count();
    }

    private Status calculateBatchStatus(
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses) {

        List<PremixStatusResponse> allEntries =
                Stream.concat(
                        premixStatuses.stream(),
                        finalMixStatuses.stream()
                ).toList();

        boolean anyRejected = allEntries.stream()
                .anyMatch(entry ->
                        entry.getPremixSubmissionStatus() == Status.REJECTED);

        if (anyRejected) {
            return Status.REJECTED;
        }

        boolean anyWaiting = allEntries.stream()
                .anyMatch(entry ->
                        entry.getPremixSubmissionStatus()
                                == Status.WAITING_FOR_APPROVAL);

        boolean allApproved = !allEntries.isEmpty() && allEntries.stream()
                .allMatch(entry ->
                        entry.getPremixSubmissionStatus() == Status.APPROVED);

        if (allApproved) {
            return Status.COMPLETELY_APPROVED;
        }

        boolean anyApproved = allEntries.stream()
                .anyMatch(entry ->
                        entry.getPremixSubmissionStatus() == Status.APPROVED);

        if (anyWaiting) {
            return Status.WAITING_FOR_PARTIAL_APPROVAL;
        }
        if (anyApproved) {
            return Status.IN_PROGRESS;
        }
        boolean anyStarted = allEntries.stream()
                .anyMatch(entry ->
                        entry.getPremixSubmissionStatus() == Status.IN_PROGRESS);
        return anyStarted ? Status.IN_PROGRESS : Status.TO_BE_INITIATED;
    }

    private List<PremixStatusResponse> buildPremixStatuses(
            MixingDetailsDto details,
            StageType stageType,
            int totalRequiredPremixes) {

        if (stageType == null) {
            throw new AcemException(
                    ErrorCode.MISSING_MANDATORY_FIELD,
                    "Stage type is required"
            );
        }

        if (stageType != StageType.PREMIX
                && stageType != StageType.FINAL_MIX) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Only PREMIX and FINAL_MIX stages are allowed"
            );
        }

        Map<Integer, PremixDto> savedEntries = new HashMap<>();

        if (details != null && details.getStages() != null) {

            for (MixingStageDto stage : details.getStages()) {

                if (stage == null
                        || stage.getStageType() != stageType
                        || stage.getPremixes() == null) {
                    continue;
                }

                for (PremixDto premix : stage.getPremixes()) {

                    if (premix == null || premix.getPremixNo() == null) {
                        continue;
                    }

                    int premixNo = premix.getPremixNo();

                    if (premixNo < 1
                            || premixNo > totalRequiredPremixes) {
                        continue;
                    }

                    savedEntries.putIfAbsent(premixNo, premix);
                }
            }
        }

        List<PremixStatusResponse> statuses =
                new ArrayList<>(totalRequiredPremixes);

        for (int premixNo = 1;
             premixNo <= totalRequiredPremixes;
             premixNo++) {

            PremixDto savedEntry = savedEntries.get(premixNo);

            Status submissionType = savedEntry != null
                    ? savedEntry.getPremixSubmissionType()
                    : null;

            Status submissionStatus = savedEntry != null
                    ? savedEntry.getPremixSubmissionStatus()
                    : null;

            if (savedEntry != null) {

                if (savedEntry.getPremixSubmissionType() != null) {
                    submissionType =
                            savedEntry.getPremixSubmissionType();
                }

                /*
                 * The persisted status is the source of truth.
                 * Do not derive IN_PROGRESS merely because the
                 * submission type is DRAFT.
                 */
                if (savedEntry.getPremixSubmissionStatus() != null) {
                    submissionStatus =
                            savedEntry.getPremixSubmissionStatus();
                }
            }

            statuses.add(
                    PremixStatusResponse.builder()
                            .premixNo(premixNo)
                            .stageType(stageType)
                            .premixSubmissionType(submissionType)
                            .premixSubmissionStatus(submissionStatus)
                            .build()
            );
        }

        return statuses;
    }

    private void normalizeMixingStatusesForBatchSync(
            Batch batch,
            long subDepartmentId,
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses) {
        MainBatchStageProgressHelper.normalizePremixStatusesForSync(
                batch, subDepartmentId, premixStatuses, StageType.PREMIX);
        MainBatchStageProgressHelper.normalizePremixStatusesForSync(
                batch, subDepartmentId, finalMixStatuses, StageType.FINAL_MIX);
    }

    private boolean hasSubmittedMixItem(MixingDetailsDto details) {
        return details != null
                && details.getStages() != null
                && details.getStages().stream()
                .filter(Objects::nonNull)
                .filter(stage -> stage.getPremixes() != null)
                .flatMap(stage -> stage.getPremixes().stream())
                .filter(Objects::nonNull)
                .anyMatch(premix -> premix.getPremixSubmissionType() == Status.SUBMIT);
    }

    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverName) {

        Mixing entity = mixingRepository
                .findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "Mixing form not found: "
                                + request.getFormId()));

        ApprovalStatusUtil.requireLegacyFinalApprovalState(entity.getStatus());
        ApprovalStatusUtil.applyStatusChange(request.getActionType(), request.getSubDepartmentId(), request.getRemarks(), request.getRejectionReason(),
                approverName, status -> entity.setStatus(status),
                entity::setRemarks, entity::setRejectionReason, entity::setUpdatedBy, entity::setUpdatedAt
        );

        if (request.getActionType() == Status.APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(entity.getFormId(), entity.getSubDepartmentId());
        }

        mixingRepository.save(entity);
        batchService.updateBatchStatus(entity.getBatchId(), entity.getSubDepartmentId(), entity.getStatus(),
                entity.getRemarks(), entity.getRejectionReason(), approverName);
        return ApproverStatusResponse.builder()
                .formId(entity.getFormId())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .rejectionReason(entity.getRejectionReason())
                .actionAt(entity.getUpdatedAt())
                .actionBy(entity.getUpdatedBy())
                .build();
    }


}
