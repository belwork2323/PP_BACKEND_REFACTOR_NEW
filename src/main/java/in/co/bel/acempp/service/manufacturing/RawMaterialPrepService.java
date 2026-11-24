package in.co.bel.acempp.service.manufacturing;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import in.co.bel.acempp.entity.batch.BatchIdentificationSheet;
import in.co.bel.acempp.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import in.co.bel.acempp.dto.batch.BatchMetadata;
import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.rawmaterialpreparation.ApCoarseProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.CreatePrepRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.DryingTrayOvenDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.LotDetailDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.MaterialMetadata;
import in.co.bel.acempp.dto.rawmaterialpreparation.MaterialProcessRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.OperationParameterRowDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.ParticleSizeRowDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixApprovalRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixApprovalResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixMetadata;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PreparationDetailsRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.PreparationFormDetailsResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PreparationResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.RMPMetadata;
import in.co.bel.acempp.dto.rawmaterialpreparation.SievingDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.TimedOperationParameterRowDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.UpdatePreparationRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.ValidationRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.WeightmentDetailRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.WeightmentPremixSheetRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.WeightmentSheetRequest;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.manufacturing.MaterialProcess;
import in.co.bel.acempp.entity.manufacturing.Premix;
import in.co.bel.acempp.entity.manufacturing.PreparationDetails;
import in.co.bel.acempp.entity.manufacturing.RawMaterialPreparation;
import in.co.bel.acempp.entity.manufacturing.SectionData;
import in.co.bel.acempp.entity.manufacturing.Validation;
import in.co.bel.acempp.entity.manufacturing.WeightmentDetail;
import in.co.bel.acempp.entity.manufacturing.WeightmentPremixSheet;
import in.co.bel.acempp.entity.manufacturing.WeightmentSheet;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.RmpFormTemplate;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.MaterialRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.repository.manufacturing.RawMaterialPreparationRepository;
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawMaterialPrepService {

    private final RawMaterialPreparationRepository preparationRepo;
    private final BatchRepository batchRepo;
    private final MaterialRepository materialRepo;
    private final UserRepository userRepo;
    private final BatchStageSequence batchStageSequence;
    private final BatchService batchService;
    private final FormIdGeneratorService formIdGenerator;
    private final FileReadOnlyService fileReadOnlyService;
    private final ApprovalBlockchainAssetService approvalBlockchainAssetService;
    private final ValidateMaster validateMaster;


    //    -----------------------------------------------CREATE FORM----------------------------------------------------------------------
    @Transactional
    public PreparationResponse createPreparation(CreatePrepRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Batch batch = batchRepo.findByBatchId(request.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId " + request.getBatchId()));
        if (batch.isDeleted()) {
            throw new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch is deleted");
        }
        batchService.assertStageUnlockedAndActive(
                batch, request.getSubDepartmentId(), "Raw Material Preparation");

        if (preparationRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(ErrorCode.FORM_ALREADY_EXISTS,
                    "Preparation form already submitted for this batch");
        }

        validateMaterials(request.getPreparationDetails());
        if (request.getPreparationDetails() != null) {
            validateMaster.validateAndResolveWeightmentBuilding(request.getPreparationDetails().getWeightmentSheet());
        }
        int totalRequiredPremixes = batch.getIdentificationSheet().getNumberOfPremix();

        List<Premix> premixes = request.getPreparationDetails().getPremixes().stream()
                .map(this::mapPremix)
                .toList();

        // Derive overall batch status dynamically based on premixes
        Status batchStatus = calculateBatchStatus(premixes, totalRequiredPremixes);

        String formId = generatePreparationFormId();

        User creator = userRepo.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "Creator not found"));

        LocalDateTime now = LocalDateTime.now();

        RawMaterialPreparation entity = RawMaterialPreparation.builder()
                .formId(formId)
                .batchId(request.getBatchId())
                .subDepartmentId(request.getSubDepartmentId())
                .formSubmissionType(request.getFormSubmissionType())
                .status(batchStatus)
                .preparationDetails(
                        PreparationDetails.builder()
                                .premixes(premixes)
                                .weightmentSheet(mapWeightmentSheet(request.getPreparationDetails().getWeightmentSheet()))
                                .build()
                )
                .submittedBy(PersonInfo.builder().id(userId).fullName(creator.getUsername()).build())
                .submittedAt(now)
                .build();

        entity = preparationRepo.save(entity);

        // Build response with full N-sized list
        List<PremixStatusResponse> premixStatuses = buildFullPremixStatuses(premixes, totalRequiredPremixes);
        batchService.syncStageProgress(
                batch.getBatchId(),
                request.getSubDepartmentId(),
                batchStatus,
                premixStatuses,
                null,
                null,
                (request.getFormSubmissionType() == Status.SUBMIT
                        || hasSubmittedPremix(request.getPreparationDetails().getPremixes()))
                        ? PersonInfo.builder().id(userId).fullName(creator.getUsername()).build()
                        : null,
                now,
                false,
                null,
                null);

        long pendingCount = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.WAITING_FOR_APPROVAL).count();
        long approvedCount = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.APPROVED).count();
        long rejectedCount = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.REJECTED).count();
        long inProgressCount = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.IN_PROGRESS).count();

        PreparationDetailsRequest details = request.getPreparationDetails();
        int solidCount = details.getPremixes().stream()
                .flatMap(p -> p.getSolidProcess() != null ? p.getSolidProcess().stream() : Stream.empty())
                .mapToInt(m -> 1).sum();
        int liquidCount = details.getPremixes().stream()
                .flatMap(p -> p.getLiquidProcess() != null ? p.getLiquidProcess().stream() : Stream.empty())
                .mapToInt(m -> 1).sum();
        // Reload after syncStageProgress — saving the method-scoped batch would overwrite
        // the synced stageProgress/currentStage with the stale TO_BE_INITIATED snapshot.
        Batch lockedBatch = batchRepo.findByBatchId(batch.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId " + batch.getBatchId()));
        lockedBatch.setLockedForEdit(true);
        batchRepo.save(lockedBatch);
        return PreparationResponse.builder()
                .batchId(request.getBatchId())
                .formId(formId)
                .formSubmissionType(entity.getFormSubmissionType())
                .status(batchStatus)
                .premixCount(premixes.size())
                .pendingPremixCount((int) pendingCount)
                .approvedPremixCount((int) approvedCount)
                .rejectedPremixCount((int) rejectedCount)
                .inProgressPremixCount((int) inProgressCount)
                .totalPremixCount(totalRequiredPremixes)
                .premixStatuses(premixStatuses)
                .totalSolidMaterials(solidCount)
                .totalLiquidMaterials(liquidCount)
                .weightmentSheetIncluded(details.getWeightmentSheet() != null)
                .submittedBy(userId)
                .submittedAt(now)
                .build();
    }

    //    -----------------------------------------------UPDATE FORM----------------------------------------------------------------------
    public PreparationResponse updatePreparation(UpdatePreparationRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());

        // 1. Fetch form & batch
        RawMaterialPreparation form = preparationRepo
                .findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(ErrorCode.FORM_NOT_FOUND,
                        "No preparation form exists for formId " + request.getFormId()));

        validateMaterials(request.getPreparationDetails());
        if (request.getPreparationDetails() != null) {
            validateMaster.validateAndResolveWeightmentBuilding(request.getPreparationDetails().getWeightmentSheet());
        }
        Batch batch = batchRepo.findByBatchId(form.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch not found"));
        batchService.assertStageUnlockedAndActive(
                batch, form.getSubDepartmentId(), "Raw Material Preparation");

        int totalRequiredPremixes = batch.getIdentificationSheet().getNumberOfPremix();

        Map<Integer, Premix> existingPremixMap = form.getPreparationDetails()
                .getPremixes()
                .stream()
                .collect(Collectors.toMap(Premix::getPremixNo, Function.identity()));

        boolean isFinalFormSubmit = (request.getFormSubmissionType() == Status.SUBMIT);

        // 2. Validate modification rules
        // Only throw modification errors during DRAFT / Partial saves if editing locked premixes.
        if (!isFinalFormSubmit) {
            for (PremixRequest premixRequest : request.getPreparationDetails().getPremixes()) {
                Premix existing = existingPremixMap.get(premixRequest.getPremixNo());
                if (existing != null) {
                    Status currentStatus = existing.getPremixSubmissionStatus();
                    if (currentStatus == Status.WAITING_FOR_APPROVAL || currentStatus == Status.APPROVED) {
                        throw new AcemException(ErrorCode.FORM_ALREADY_EXISTS,
                                "Premix " + premixRequest.getPremixNo() + " cannot be modified.");
                    }
                }
            }
        }

        // 3. Update existing or append new premixes safely
        List<Premix> updatedPremixes = new ArrayList<>(form.getPreparationDetails().getPremixes());

        for (PremixRequest premixRequest : request.getPreparationDetails().getPremixes()) {
            Premix existing = existingPremixMap.get(premixRequest.getPremixNo());

            if (existing != null) {
                // Preserve existing APPROVED/WAITING status during final form submission
                if (!isFinalFormSubmit || (existing.getPremixSubmissionStatus() != Status.APPROVED
                        && existing.getPremixSubmissionStatus() != Status.WAITING_FOR_APPROVAL)) {

                    Status newPremixStatus = premixRequest.getPremixSubmissionType() == Status.DRAFT
                            ? Status.IN_PROGRESS
                            : Status.WAITING_FOR_APPROVAL;

                    existing.setPremixDate(premixRequest.getPremixDate());
                    existing.setMaterialType(premixRequest.getMaterialType());
                    List<MaterialProcess> inboundSolid =
                            mapMaterialProcessList(premixRequest.getSolidProcess());
                    List<MaterialProcess> inboundLiquid =
                            mapMaterialProcessList(premixRequest.getLiquidProcess());
                    // Don't wipe stored process rows with an empty client payload.
                    if (!inboundSolid.isEmpty()
                            || existing.getSolidProcess() == null
                            || existing.getSolidProcess().isEmpty()) {
                        existing.setSolidProcess(inboundSolid);
                    }
                    if (!inboundLiquid.isEmpty()
                            || existing.getLiquidProcess() == null
                            || existing.getLiquidProcess().isEmpty()) {
                        existing.setLiquidProcess(inboundLiquid);
                    }
                    existing.setPremixSubmissionType(premixRequest.getPremixSubmissionType());
                    existing.setPremixSubmissionStatus(newPremixStatus);
                    existing.setRejectionReason(null);
                }
            } else {
                Status newPremixStatus = premixRequest.getPremixSubmissionType() == Status.DRAFT
                        ? Status.IN_PROGRESS
                        : Status.WAITING_FOR_APPROVAL;

                Premix newPremix = Premix.builder()
                        .premixNo(premixRequest.getPremixNo())
                        .premixDate(premixRequest.getPremixDate())
                        .materialType(premixRequest.getMaterialType())
                        .premixSubmissionType(premixRequest.getPremixSubmissionType())
                        .premixSubmissionStatus(newPremixStatus)
                        .rejectionReason(null)
                        .solidProcess(mapMaterialProcessList(premixRequest.getSolidProcess()))
                        .liquidProcess(mapMaterialProcessList(premixRequest.getLiquidProcess()))
                        .build();
                updatedPremixes.add(newPremix);
            }
        }

        form.setRejectionReason(resolveFormRejectionReasonFromPremixes(updatedPremixes));

        // 4. Calculate dynamic batch status
        // If the user triggered final form SUBMIT, force status to WAITING_FOR_COMPLETE_APPROVAL
        Status batchStatus = calculateBatchStatus(updatedPremixes, totalRequiredPremixes);

        // 5. Update form details
        form.setPreparationDetails(
                PreparationDetails.builder()
                        .premixes(updatedPremixes)
                        .weightmentSheet(mapWeightmentSheet(request.getPreparationDetails().getWeightmentSheet()))
                        .build());
        form.setFormSubmissionType(request.getFormSubmissionType());
        form.setStatus(batchStatus);

        User updater = userRepo.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        LocalDateTime now = LocalDateTime.now();
        form.setUpdatedBy(PersonInfo.builder().id(userId).fullName(updater.getUsername()).build());
        form.setUpdatedAt(now);

        if (isFinalFormSubmit) {
            form.setSubmittedBy(PersonInfo.builder().id(userId).fullName(updater.getUsername()).build());
            form.setSubmittedAt(now);
        }

        RawMaterialPreparation savedForm = preparationRepo.save(form);

        // 7. Calculate response counters & build full N-sized status list
        List<PremixStatusResponse> premixStatuses = buildFullPremixStatuses(updatedPremixes, totalRequiredPremixes);
        batchService.syncStageProgress(
                batch.getBatchId(),
                savedForm.getSubDepartmentId(),
                batchStatus,
                premixStatuses,
                null,
                null,
                isFinalFormSubmit || hasSubmittedPremix(request.getPreparationDetails().getPremixes())
                        ? PersonInfo.builder().id(userId).fullName(updater.getUsername()).build()
                        : null,
                now,
                false,
                form.getRemarks(),
                form.getRejectionReason());

        long pending = updatedPremixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.WAITING_FOR_APPROVAL).count();
        long approved = updatedPremixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.APPROVED).count();
        long rejected = updatedPremixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.REJECTED).count();
        long inProgress = updatedPremixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.IN_PROGRESS).count();

        PreparationDetailsRequest details = request.getPreparationDetails();
        int solidCount = details.getPremixes().stream()
                .flatMap(p -> p.getSolidProcess() != null ? p.getSolidProcess().stream() : Stream.empty())
                .mapToInt(m -> 1).sum();
        int liquidCount = details.getPremixes().stream()
                .flatMap(p -> p.getLiquidProcess() != null ? p.getLiquidProcess().stream() : Stream.empty())
                .mapToInt(m -> 1).sum();

        String submittedById = savedForm.getSubmittedBy() != null ? savedForm.getSubmittedBy().getId() : userId;
        LocalDateTime submittedAt =
                savedForm.getSubmittedAt() != null ? savedForm.getSubmittedAt() : now;

        return PreparationResponse.builder()
                .formId(savedForm.getFormId())
                .batchId(savedForm.getBatchId())
                .formSubmissionType(savedForm.getFormSubmissionType())
                .status(batchStatus)
                .premixCount(updatedPremixes.size())
                .pendingPremixCount((int) pending)
                .approvedPremixCount((int) approved)
                .rejectedPremixCount((int) rejected)
                .inProgressPremixCount((int) inProgress)
                .totalPremixCount(totalRequiredPremixes)
                .premixStatuses(premixStatuses)
                .totalSolidMaterials(solidCount)
                .totalLiquidMaterials(liquidCount)
                .weightmentSheetIncluded(details.getWeightmentSheet() != null)
                .submittedBy(submittedById)
                .submittedAt(submittedAt)
                .lastUpdatedBy(userId)
                .lastUpdatedAt(now)
                .build();
    }

    //    -----------------------------------------------FORM DETAILS----------------------------------------------------------------------
    public PreparationFormDetailsResponse getFormDetails(String formId) {
        RawMaterialPreparation form = preparationRepo
                .findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(ErrorCode.FORM_NOT_FOUND,
                        "No preparation form exists for formId " + formId));

        Batch batch = batchRepo.findByBatchId(form.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch not found"));

        int totalRequiredPremixes = batch.getIdentificationSheet().getNumberOfPremix();
        List<Premix> premixes = form.getPreparationDetails().getPremixes();

        // Build complete list of size N
        List<PremixStatusResponse> premixStatuses = buildFullPremixStatuses(premixes, totalRequiredPremixes);

        long pending = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.WAITING_FOR_APPROVAL).count();
        long approved = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.APPROVED).count();
        long rejected = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.REJECTED).count();
        long inProgress = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.IN_PROGRESS).count();

        return PreparationFormDetailsResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .subDepartmentId(form.getSubDepartmentId())
                .formSubmissionType(form.getFormSubmissionType())
                .status(form.getStatus())
                .createdBy(batch.getCreatedBy().getId())
                .createdAt(batch.getCreatedAt())
                .submittedBy(form.getSubmittedBy())
                .submittedAt(form.getSubmittedAt())
                .lastUpdatedBy(form.getUpdatedBy() != null ? form.getUpdatedBy().getId() : null)
                .lastUpdatedAt(form.getUpdatedAt())
                .pendingPremixCount((int) pending)
                .approvedPremixCount((int) approved)
                .rejectedPremixCount((int) rejected)
                .inProgressPremixCount((int) inProgress)
                .totalPremixCount(totalRequiredPremixes)
                .premixStatuses(premixStatuses)
                .preparationDetails(mapToPreparationDetailsRequest(form.getPreparationDetails()))
                .build();
    }

    // Mapper method – converts entity back to request‑style DTO for response

    private String generatePreparationFormId() {
        return formIdGenerator.generate(
                "raw_material_preparations",
                "formId",
                "RMPF",
                3
        );
    }

    private PreparationDetailsRequest mapToPreparationDetailsRequest(PreparationDetails details) {
        if (details == null) return null;
        return PreparationDetailsRequest.builder()
                .premixes(details.getPremixes().stream().map(p -> {
                    PremixRequest pr = new PremixRequest();
                    pr.setPremixNo(p.getPremixNo());
                    pr.setPremixDate(p.getPremixDate());
                    pr.setMaterialType(p.getMaterialType());
                    pr.setPremixSubmissionType(p.getPremixSubmissionType());
                    pr.setPremixSubmissionStatus(p.getPremixSubmissionStatus());
                    pr.setRejectionReason(p.getRejectionReason());
                    pr.setSolidProcess(mapMaterialProcessListToEntity(p.getSolidProcess()));
                    pr.setLiquidProcess(mapMaterialProcessListToEntity(p.getLiquidProcess()));
                    return pr;
                }).collect(Collectors.toList()))
                .weightmentSheet(details.getWeightmentSheet() != null
                        ? mapWeightmentSheet(details.getWeightmentSheet())
                        : null)
                .build();
    }

    private List<MaterialProcessRequest> mapMaterialProcessListToEntity(List<MaterialProcess> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::mapMaterialProcessToRequest).collect(Collectors.toList());
    }

    /**
     * Entity → API. Prefer typed fields; migrate legacy {@code sections} when typed fields are empty.
     */
    private MaterialProcessRequest mapMaterialProcessToRequest(MaterialProcess mp) {
        MaterialProcessRequest mpr = new MaterialProcessRequest();
        mpr.setMaterialId(mp.getMaterialId());
        mpr.setMaterialCode(mp.getMaterialCode());
        mpr.setMaterialName(mp.getMaterialName());
        mpr.setGradeId(mp.getGradeId());
        mpr.setGradeCode(mp.getGradeCode());
        mpr.setProcessType(mp.getProcessType());
        mpr.setLotDetails(mp.getLotDetails() != null ? mp.getLotDetails() : new ArrayList<>());
        mpr.setDrying(mp.getDrying());
        mpr.setSieving(mp.getSieving());
        mpr.setApCoarse(mp.getApCoarse());
        mpr.setApFine(mp.getApFine());
        mpr.setApUltraFine(mp.getApUltraFine());
        mpr.setAluminum(mp.getAluminum());
        mpr.setDoa(mp.getDoa());

        boolean needsLegacy = (mpr.getLotDetails() == null || mpr.getLotDetails().isEmpty())
                && mpr.getDrying() == null
                && mpr.getSieving() == null
                && mpr.getApCoarse() == null
                && mpr.getApFine() == null
                && mpr.getApUltraFine() == null
                && mpr.getAluminum() == null
                && mpr.getDoa() == null
                && mp.getSections() != null
                && !mp.getSections().isEmpty();
        if (needsLegacy) {
            migrateLegacySectionsIntoTyped(mpr, mp.getSections());
        }
        if (mpr.getProcessType() == null || mpr.getProcessType().isBlank()) {
            if (mpr.getApCoarse() != null) {
                mpr.setProcessType("AP_COARSE");
            } else if (mpr.getApFine() != null) {
                mpr.setProcessType("AP_FINE");
            } else if (mpr.getApUltraFine() != null) {
                mpr.setProcessType("AP_ULTRA_FINE");
            } else if (mpr.getAluminum() != null) {
                mpr.setProcessType("ALUMINUM");
            } else if (mpr.getDoa() != null) {
                mpr.setProcessType("DOA");
            } else if (mpr.getDrying() != null || mpr.getSieving() != null) {
                mpr.setProcessType("DEFAULT_SOLID");
            } else {
                mpr.setProcessType("DEFAULT_LIQUID");
            }
        }
        return mpr;
    }

    @SuppressWarnings("unchecked")
    private void migrateLegacySectionsIntoTyped(MaterialProcessRequest mpr, List<SectionData> sections) {
        for (SectionData sec : sections) {
            if (sec == null || sec.getSectionId() == null) continue;
            String id = sec.getSectionId().trim();
            List<Map<String, Object>> rows = sec.getSectionData();
            Map<String, Object> first = (rows != null && !rows.isEmpty()) ? rows.get(0) : Collections.emptyMap();

            if ("lotDetails".equalsIgnoreCase(id) || "lotDetail".equalsIgnoreCase(id)) {
                List<LotDetailDto> lots = new ArrayList<>();
                if (rows != null) {
                    for (Map<String, Object> row : rows) {
                        if (row == null) continue;
                        Object lotId = row.get("lotId");
                        if (lotId == null) lotId = row.get("lotNumber");
                        Object qty = row.get("quantity");
                        LotDetailDto dto = LotDetailDto.builder()
                                .lotId(lotId != null ? String.valueOf(lotId) : null)
                                .quantity(toDouble(qty))
                                .build();
                        lots.add(dto);
                    }
                }
                mpr.setLotDetails(lots);
            } else if ("dryingTrayOven".equalsIgnoreCase(id) || "drying".equalsIgnoreCase(id)) {
                mpr.setDrying(DryingTrayOvenDto.builder()
                        .ovenType(strVal(first.get("ovenType")))
                        .ovenNumber(strVal(first.get("ovenNumber")))
                        .ovenSetTemperature(strVal(first.get("ovenSetTemperature")))
                        .startDatetime(dateTimeVal(first.get("startDatetime")))
                        .endDatetime(dateTimeVal(first.get("endDatetime")))
                        .moisture(strVal(first.get("moisture")))
                        .observation(strVal(first.get("observation")))
                        .build());
            } else if ("sieving".equalsIgnoreCase(id)) {
                mpr.setSieving(SievingDto.builder()
                        .sievingDispatchDatetime(dateTimeVal(first.get("sievingDispatchDatetime")))
                        .sievedQuantity(strVal(first.get("sievedQuantity")))
                        .sieveMeshSize(strVal(first.get("sieveMeshSize")))
                        .observation(strVal(first.get("observation")))
                        .build());
            } else if ("blendingDryingParameters".equalsIgnoreCase(id)
                    || "dryingOperationRvd".equalsIgnoreCase(id)
                    || "particleSizeDistribution".equalsIgnoreCase(id)) {
                ApCoarseProcessDto ap = mpr.getApCoarse() != null
                        ? mpr.getApCoarse()
                        : ApCoarseProcessDto.builder().build();
                if ("blendingDryingParameters".equalsIgnoreCase(id) && rows != null) {
                    List<OperationParameterRowDto> list = new ArrayList<>();
                    for (Map<String, Object> row : rows) {
                        if (row == null) continue;
                        list.add(OperationParameterRowDto.builder()
                                .operation(strVal(row.get("operation")))
                                .setParameter(strVal(row.get("setParameter")))
                                .actualParameter(strVal(row.get("actualParameter")))
                                .build());
                    }
                    ap.setBlendingDryingParameters(list);
                } else if ("dryingOperationRvd".equalsIgnoreCase(id) && rows != null) {
                    List<TimedOperationParameterRowDto> list = new ArrayList<>();
                    for (Map<String, Object> row : rows) {
                        if (row == null) continue;
                        Object startDateTime = row.containsKey("startDateTime")
                                ? row.get("startDateTime")
                                : row.get("startTime");
                        Object endDateTime = row.containsKey("endDateTime")
                                ? row.get("endDateTime")
                                : row.get("endTime");
                        list.add(TimedOperationParameterRowDto.builder()
                                .operation(strVal(row.get("operation")))
                                .setParameter(strVal(row.get("setParameter")))
                                .actualParameter(strVal(row.get("actualParameter")))
                                .startDateTime(dateTimeVal(startDateTime))
                                .endDateTime(dateTimeVal(endDateTime))
                                .build());
                    }
                    ap.setDryingOperationRvd(list);
                } else if ("particleSizeDistribution".equalsIgnoreCase(id) && rows != null) {
                    List<ParticleSizeRowDto> list = new ArrayList<>();
                    for (Map<String, Object> row : rows) {
                        if (row == null) continue;
                        list.add(ParticleSizeRowDto.builder()
                                .psdRequirement(strVal(row.get("psdRequirement")))
                                .specification(strVal(row.get("specification")))
                                .result(strVal(row.get("result")))
                                .build());
                    }
                    ap.setParticleSizeDistribution(list);
                }
                mpr.setApCoarse(ap);
            }
        }
    }

    private static String strVal(Object v) {
        return v == null ? null : DateTimeConversionUtil.formatLocalDateTimeString(String.valueOf(v));
    }

    private static LocalDateTime dateTimeVal(Object value) {
        if (value == null) return null;
        if (value instanceof String text) {
            return DateTimeConversionUtil.parseLocalDateTime(text);
        }
        LocalDateTime dateTime = DateTimeConversionUtil.toLocalDateTime(value);
        if (dateTime == null) {
            throw new IllegalArgumentException("Invalid legacy raw material preparation date-time: " + value);
        }
        return dateTime;
    }

    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private WeightmentSheetRequest mapWeightmentSheet(WeightmentSheet ws) {
        WeightmentSheetRequest wsr = new WeightmentSheetRequest();
        wsr.setMixerBuildingNumber(ws.getMixerBuildingNumber());
        wsr.setWeightmentDetails(ws.getWeightmentDetails() == null
                ? Collections.emptyList()
                : ws.getWeightmentDetails().stream().map(wd -> {
            WeightmentDetailRequest wdr = new WeightmentDetailRequest();
            wdr.setMaterialCode(wd.getMaterialCode());
            wdr.setMaterialName(wd.getMaterialName());
            wdr.setGradeCode(wd.getGradeCode());
            wdr.setGradeName(wd.getGradeName());
            wdr.setPremixNo(wd.getPremixNo());
            wdr.setPercentage(wd.getPercentage());
            wdr.setWeightTransferred(wd.getWeightTransferred());
            wdr.setContainerType(wd.getContainerType());
            wdr.setContainerNumber(wd.getContainerNumber());
            wdr.setWeighScaleNumber(wd.getWeighScaleNumber());
            wdr.setWeighingDateTime(wd.getWeighingDateTime());
            return wdr;
        }).collect(Collectors.toList()));
        if (ws.getValidation() != null) {
            ValidationRequest v = new ValidationRequest();
            v.setCompareWithIdentificationSheet(ws.getValidation().getCompareWithIdentificationSheet());
            v.setDeviationFound(ws.getValidation().getDeviationFound());
            v.setDeviationMessage(ws.getValidation().getDeviationMessage());
            wsr.setValidation(v);
        }
        if (ws.getPremixSheets() != null) {
            wsr.setPremixSheets(ws.getPremixSheets().stream().map(ps -> {
                WeightmentPremixSheetRequest req = new WeightmentPremixSheetRequest();
                req.setPremixNo(ps.getPremixNo());
                req.setMixerBuildingNumber(ps.getMixerBuildingNumber());
                if (ps.getValidation() != null) {
                    ValidationRequest v = new ValidationRequest();
                    v.setCompareWithIdentificationSheet(ps.getValidation().getCompareWithIdentificationSheet());
                    v.setDeviationFound(ps.getValidation().getDeviationFound());
                    v.setDeviationMessage(ps.getValidation().getDeviationMessage());
                    req.setValidation(v);
                }
                return req;
            }).collect(Collectors.toList()));
        }
        return wsr;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverName) {

        RawMaterialPreparation entity = preparationRepo
                .findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Raw material preparation form not found: " + request.getFormId()));

        ApprovalStatusUtil.requireLegacyFinalApprovalState(entity.getStatus());

        // Apply overall batch/form level action
        ApprovalStatusUtil.applyStatusChange(
                request.getActionType(),
                request.getSubDepartmentId(),
                request.getRemarks(),
                request.getRejectionReason(),
                approverName,
                status -> entity.setStatus(status),
                entity::setRemarks,
                entity::setRejectionReason,
                entity::setUpdatedBy,
                entity::setUpdatedAt
        );

        if (request.getActionType() == Status.APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(entity.getFormId(), entity.getSubDepartmentId());
        }

        preparationRepo.save(entity);

        // Sync status back to main batch entity
        batchService.updateBatchStatus(
                entity.getBatchId(),
                entity.getSubDepartmentId(),
                entity.getStatus(),
                entity.getRemarks(),
                entity.getRejectionReason(),
                approverName
        );

        return ApproverStatusResponse.builder()
                .formId(entity.getFormId())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .rejectionReason(entity.getRejectionReason())
                .actionAt(entity.getUpdatedAt())
                .actionBy(entity.getUpdatedBy())
                .build();
    }   
    @Transactional(rollbackFor = Exception.class)
    public void updateRmpMetadata(RawMaterialPreparation form) {

        Batch batch = batchRepo.findByBatchId(form.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "Batch not found"));

        if (batch.getIdentificationSheet().getMetadata() == null) {
            batch.getIdentificationSheet().setMetadata(new BatchMetadata());
        }

        BatchMetadata metadata = batch.getIdentificationSheet().getMetadata();

        RMPMetadata rmp = new RMPMetadata();
        rmp.setFormId(form.getFormId());

        List<PremixMetadata> premixMetadataList = new ArrayList<>();

        for (Premix premix : form.getPreparationDetails().getPremixes()) {

            // Save only approved premixes
            if (premix.getPremixSubmissionStatus() != Status.APPROVED) {
                continue;
            }

            PremixMetadata premixMetadata = new PremixMetadata();

            premixMetadata.setPremixNo(premix.getPremixNo());
            premixMetadata.setPremixDate(premix.getPremixDate());
            premixMetadata.setMaterialType(premix.getMaterialType());

            List<MaterialMetadata> materials = new ArrayList<>();

            // Solid Materials
            if (premix.getSolidProcess() != null) {
                for (MaterialProcess process : premix.getSolidProcess()) {

                    materials.add(
                            MaterialMetadata.builder()
                                    .materialId(process.getMaterialId())
                                    .materialCode(process.getMaterialCode())
                                    .materialName(process.getMaterialName())
                                    .gradeId(process.getGradeId())
                                    .gradeCode(process.getGradeCode())
                                    .materialType("Solid")
                                    .build()
                    );
                }
            }

            // Liquid Materials
            if (premix.getLiquidProcess() != null) {
                for (MaterialProcess process : premix.getLiquidProcess()) {

                    materials.add(
                            MaterialMetadata.builder()
                                    .materialId(process.getMaterialId())
                                    .materialCode(process.getMaterialCode())
                                    .materialName(process.getMaterialName())
                                    .gradeId(process.getGradeId())
                                    .gradeCode(process.getGradeCode())
                                    .materialType("Liquid")
                                    .build()
                    );
                }
            }

            premixMetadata.setMaterials(materials);

            premixMetadataList.add(premixMetadata);
        }

        rmp.setPremixes(premixMetadataList);

        // Weightment Sheet
        WeightmentSheet ws = form.getPreparationDetails().getWeightmentSheet();

        if (ws != null) {

            WeightmentSheetRequest weightment = new WeightmentSheetRequest();

            weightment.setMixerBuildingNumber(ws.getMixerBuildingNumber());

            if (ws.getValidation() != null) {

                weightment.setValidation(
                        ValidationRequest.builder()
                                .compareWithIdentificationSheet(
                                        ws.getValidation().getCompareWithIdentificationSheet())
                                .deviationFound(
                                        ws.getValidation().getDeviationFound())
                                .deviationMessage(
                                        ws.getValidation().getDeviationMessage())
                                .build()
                );
            }

            if (ws.getPremixSheets() != null) {
                weightment.setPremixSheets(ws.getPremixSheets().stream().map(ps -> {
                    WeightmentPremixSheetRequest req = new WeightmentPremixSheetRequest();
                    req.setPremixNo(ps.getPremixNo());
                    req.setMixerBuildingNumber(ps.getMixerBuildingNumber());
                    if (ps.getValidation() != null) {
                        req.setValidation(
                                ValidationRequest.builder()
                                        .compareWithIdentificationSheet(
                                                ps.getValidation().getCompareWithIdentificationSheet())
                                        .deviationFound(ps.getValidation().getDeviationFound())
                                        .deviationMessage(ps.getValidation().getDeviationMessage())
                                        .build());
                    }
                    return req;
                }).collect(Collectors.toList()));
            }

            rmp.setWeightmentSheet(weightment);
        }

        metadata.setRawMaterialPreparation(rmp);

        batchRepo.save(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public PremixApprovalResponse changeStatusOfPremix(PremixApprovalRequest request, String approverName) {
        if (request.getActionType() == Status.REJECTED &&
                (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new AcemException(ErrorCode.REJECTION_REASON_REQUIRED, "rejectionReason is required for rejected premix.");
        }

        RawMaterialPreparation form = preparationRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(ErrorCode.FORM_NOT_FOUND, "Preparation form not found"));

        Premix premix = form.getPreparationDetails().getPremixes().stream()
                .filter(p -> p.getPremixNo() == request.getPremixNo())
                .findFirst()
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST, "Premix not found"));

        if (premix.getPremixSubmissionStatus() != Status.WAITING_FOR_APPROVAL) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Premix is not waiting for approval.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (request.getActionType() == Status.APPROVED) {
            updateRmpMetadata(form);
        }

        form.setUpdatedBy(PersonInfo.builder().fullName(approverName).build());
        form.setUpdatedAt(now);
        form.setRemarks(request.getRemarks());

        Batch batch = batchRepo.findByBatchId(form.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch not found"));

        int totalRequiredPremixes = batch.getIdentificationSheet().getNumberOfPremix();
        List<Premix> premixes = form.getPreparationDetails().getPremixes();

        long currentlyApprovedCount = premixes.stream()
                .filter(p -> p.getPremixSubmissionStatus() == Status.APPROVED)
                .count();
        boolean isFirstPremixApproval = (currentlyApprovedCount == 0);
        premix.setPremixSubmissionStatus(request.getActionType());
        if (request.getActionType() == Status.REJECTED) {
            premix.setRejectionReason(request.getRejectionReason());
        } else {
            premix.setRejectionReason(null);
        }
        form.setRejectionReason(resolveFormRejectionReasonFromPremixes(premixes));
        Status batchStatus = calculateBatchStatus(premixes, totalRequiredPremixes);
        form.setStatus(batchStatus);

        if (batchStatus == Status.COMPLETELY_APPROVED) {
            form.setFormSubmissionType(Status.SUBMIT);
            form.setSubmittedBy(form.getUpdatedBy());
            form.setSubmittedAt(now);
            form.setRejectionReason(null);
        }

        if (request.getActionType() == Status.APPROVED && batchStatus == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }
        if (request.getActionType() == Status.APPROVED) {
            try {
                if (isFirstPremixApproval) {
                    Map<String, Object> batchBlockchainPayload = buildBatchBlockchainPayload(batch);
                    approvalBlockchainAssetService.createBatchAssetOnBlockchain(
                            form.getBatchId(),
                            batchBlockchainPayload,
                            approverName
                    );
                }

                Map<String, Object> blockchainPayload = buildPremixBlockchainPayload(form, premix, approverName, now);
                approvalBlockchainAssetService.createAssetOnBlockchain(
                        form.getSubDepartmentId(),
                        form.getBatchId(),
                        form.getFormId(),
                        blockchainPayload,
                        "PREMIX" + request.getPremixNo(),
                        approverName);
            } catch (Exception e) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new AcemException(ErrorCode.INVALID_REQUEST, "Blockchain sync failed: " + e.getMessage());
            }
        }
        List<PremixStatusResponse> premixStatuses = buildFullPremixStatuses(premixes, totalRequiredPremixes);
        preparationRepo.save(form);

        batchService.syncStageApproval(
                form.getBatchId(),
                form.getSubDepartmentId(),
                batchStatus,
                premixStatuses,
                null,
                null,
                request.getActionType() == Status.APPROVED,
                form.getRemarks(),
                form.getRejectionReason(),
                approverName,
                now);

        long pending = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.WAITING_FOR_APPROVAL).count();
        long approved = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.APPROVED).count();
        long rejected = premixes.stream().filter(p -> p.getPremixSubmissionStatus() == Status.REJECTED).count();

        return PremixApprovalResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .premixNo(request.getPremixNo())
                .actionType(request.getActionType())
                .status(request.getActionType())
                .premixSubmissionStatus(premix.getPremixSubmissionStatus())
                .batchStatus(batchStatus)
                .allPremixesApproved(approved == totalRequiredPremixes)
                .pendingPremixCount((int) pending)
                .approvedPremixCount((int) approved)
                .rejectedPremixCount((int) rejected)
                .totalPremixCount(totalRequiredPremixes)
                .remarks(form.getRemarks())
                .rejectionReason(form.getRejectionReason())
                .actionBy(approverName)
                .actionAt(now)
                .build();
    }

    private Map<String, Object> buildPremixBlockchainPayload(RawMaterialPreparation form, Premix premix, String approverName, LocalDateTime approvalTime) {
        Map<String, Object> rootPayload = new LinkedHashMap<>();

        // 1. Audit / Metadata Object (Using current approval details directly)
        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getSubmittedBy() != null) {
                auditMap.put("createdBy", form.getSubmittedBy().getFullName());
            }
            if (form.getSubmittedAt() != null) {
                auditMap.put("createdAt", form.getSubmittedAt());
            }
            // Use the passed approverName and timestamp since form fields aren't set yet
            if (form.getStatus() == Status.APPROVED || approverName != null) {
                auditMap.put("approvedBy", approverName);
                auditMap.put("approvedAt", approvalTime);
            }
        }
        rootPayload.put("audit", auditMap);
        Map<String, Object> premixMap = new LinkedHashMap<>();
        if (premix != null) {
            premixMap.put("premixNo", premix.getPremixNo());

            if (form != null && form.getPreparationDetails() != null && form.getPreparationDetails().getWeightmentSheet() != null) {
                WeightmentSheet sheet = form.getPreparationDetails().getWeightmentSheet();
                String mixerBuildingNumber = sheet.getMixerBuildingNumber();
                if (sheet.getPremixSheets() != null) {
                    mixerBuildingNumber = sheet.getPremixSheets().stream()
                            .filter(item -> Objects.equals(item.getPremixNo(), premix.getPremixNo()))
                            .map(WeightmentPremixSheet::getMixerBuildingNumber)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(mixerBuildingNumber);
                }
                if (mixerBuildingNumber != null) {
                    premixMap.put("mixerBuildingNumber", mixerBuildingNumber);
                }

                if (sheet.getWeightmentDetails() != null) {
                    WeightmentDetail weightmentDetail = sheet.getWeightmentDetails().stream()
                            .filter(item -> Objects.equals(item.getPremixNo(), premix.getPremixNo()))
                            .findFirst()
                            .orElse(null);
                    if (weightmentDetail != null) {
                        Map<String, Object> weightmentDetails = new LinkedHashMap<>();
                        weightmentDetails.put("weightTransferred", weightmentDetail.getWeightTransferred());
                        weightmentDetails.put("containerType", weightmentDetail.getContainerType());
                        weightmentDetails.put("containerNumber", weightmentDetail.getContainerNumber());
                        weightmentDetails.put("weighScaleNumber", weightmentDetail.getWeighScaleNumber());
                        if (weightmentDetail.getWeighingDateTime() != null) {
                            weightmentDetails.put(
                                    "weighingDateTime",
                                    weightmentDetail.getWeighingDateTime());
                        }
                        premixMap.put("weightmentDetails", weightmentDetails);
                    }
                }
            }

            premixMap.put("solidProcess", premix.getSolidProcess() == null ? Collections.emptyList() : premix.getSolidProcess());
            premixMap.put("liquidProcess", premix.getLiquidProcess() == null ? Collections.emptyList() : premix.getLiquidProcess());
        }
        rootPayload.put("premixData", premixMap);

        return rootPayload;
    }
    public Map<String, Object> buildBatchBlockchainPayload( Batch batch) {
        Map<String, Object> payload = new HashMap<>();
        if (batch.getId() != null) payload.put("id", batch.getId());
        if (batch.getBatchId() != null) payload.put("batchId", batch.getBatchId());
        if (batch.getFormId() != null) payload.put("formId", batch.getFormId());
        if (batch.getBatchType() != null) payload.put("batchType", batch.getBatchType());
        if (batch.getSubBatchType() != null) payload.put("subBatchType", batch.getSubBatchType());
        if (batch.getProjectId() != null) payload.put("projectId", batch.getProjectId());
        if (batch.getProjectName() != null) payload.put("projectName", batch.getProjectName());
        if (batch.getMotorStage() != null) payload.put("motorStage", batch.getMotorStage());
        if (batch.getNumberOfMotors() != null) payload.put("numberOfMotors", batch.getNumberOfMotors());

        if (batch.getMotorIds() != null && !batch.getMotorIds().isEmpty()) {
            payload.put("motorIds", batch.getMotorIds());
        }

        if (batch.getArticles() != null && !batch.getArticles().isEmpty()) {
            payload.put("articles", batch.getArticles());
        }

        if (batch.getObjective() != null) payload.put("objective", batch.getObjective());
        if (batch.getCreatedAt() != null) payload.put("createdAt", batch.getCreatedAt());
        if (batch.getSystemManager() != null) payload.put("systemManager", batch.getSystemManager());
        if (batch.getCreatedBy() != null) payload.put("createdBy", batch.getCreatedBy());

        // Map embedded BatchIdentificationSheet with null-field exclusion
        if (batch.getIdentificationSheet() != null) {
            Map<String, Object> sheetMap = new HashMap<>();
            BatchIdentificationSheet sheet = batch.getIdentificationSheet();

            if (sheet.getDate() != null) sheetMap.put("date", sheet.getDate());
            if (sheet.getBatchSize() != null) sheetMap.put("batchSize", sheet.getBatchSize());
            if (sheet.getBondingSheetNo() != null) sheetMap.put("bondingSheetNo", sheet.getBondingSheetNo());
            if (sheet.getMixerType() != null) sheetMap.put("mixerType", sheet.getMixerType());
            if (sheet.getNumberOfPremix() != null) sheetMap.put("numberOfPremix", sheet.getNumberOfPremix());
            if (sheet.getRemarks() != null) sheetMap.put("remarks", sheet.getRemarks());
            if (sheet.getPrcApprovalDate() != null) sheetMap.put("prcApprovalDate", sheet.getPrcApprovalDate());
            if (sheet.getMetadata() != null) sheetMap.put("metadata", sheet.getMetadata());

            // Handle materials list inside identification sheet
            if (sheet.getMaterials() != null && !sheet.getMaterials().isEmpty()) {
                sheetMap.put("materials", sheet.getMaterials());
            }

            if (!sheetMap.isEmpty()) {
                payload.put("identificationSheet", sheetMap);
            }
        }

        if (batch.getIdentificationSheetStatus() != null) {
            payload.put("identificationSheetStatus", batch.getIdentificationSheetStatus());
        }
        return payload;
    }


    private Status calculateBatchStatus(List<Premix> savedPremixes, int totalRequiredPremixes) {
        if (savedPremixes == null || savedPremixes.isEmpty()) {
            return Status.TO_BE_INITIATED;
        }

        // 1. Any rejection fails the batch status immediately
        boolean anyRejected = savedPremixes.stream()
                .anyMatch(p -> p.getPremixSubmissionStatus() == Status.REJECTED);
        if (anyRejected) {
            return Status.REJECTED;
        }

        // Counts for specific states
        long waitingCount = savedPremixes.stream()
                .filter(p -> p.getPremixSubmissionStatus() == Status.WAITING_FOR_APPROVAL)
                .count();

        long approvedCount = savedPremixes.stream()
                .filter(p -> p.getPremixSubmissionStatus() == Status.APPROVED)
                .count();

        // All required premixes approved: the form completes automatically.
        if (totalRequiredPremixes > 0
                && approvedCount == totalRequiredPremixes && waitingCount == 0) {
            return Status.COMPLETELY_APPROVED;
        }

        if (waitingCount > 0) {
            if (totalRequiredPremixes == 1 || (totalRequiredPremixes <= 0 && savedPremixes.size() == 1)) {
                return Status.WAITING_FOR_APPROVAL;
            }
            return Status.WAITING_FOR_PARTIAL_APPROVAL;
        }
        if (approvedCount > 0) {
            return Status.IN_PROGRESS;
        }
        boolean anyStarted = savedPremixes.stream()
                .anyMatch(p -> p.getPremixSubmissionStatus() == Status.IN_PROGRESS
                        || p.getPremixSubmissionType() == Status.DRAFT);
        if (anyStarted) {
            return Status.IN_PROGRESS;
        }

        return Status.TO_BE_INITIATED;
    }

    private List<PremixStatusResponse> buildFullPremixStatuses(List<Premix> savedPremixes, int totalRequiredPremixes) {
        List<PremixStatusResponse> responseList = new ArrayList<>();

        // Map saved premixes by premixNo for quick lookup
        Map<Integer, Premix> savedMap = savedPremixes != null
                ? savedPremixes.stream().collect(Collectors.toMap(Premix::getPremixNo, Function.identity()))
                : Collections.emptyMap();

        for (int i = 1; i <= totalRequiredPremixes; i++) {
            if (savedMap.containsKey(i)) {
                Premix p = savedMap.get(i);
                Status status = p.getPremixSubmissionStatus();
                responseList.add(
                        PremixStatusResponse.builder()
                                .premixNo(p.getPremixNo())
                                .stageType(StageType.PREMIX)
                                .premixSubmissionType(p.getPremixSubmissionType())
                                .premixSubmissionStatus(status)
                                .locked(isPremixFormLocked(status))
                                .build()
                );
            } else {
                responseList.add(
                        PremixStatusResponse.builder()
                                .premixNo(i)
                                .stageType(StageType.PREMIX)
                                .premixSubmissionType(Status.DRAFT)
                                .premixSubmissionStatus(Status.TO_BE_INITIATED)
                                .locked(false)
                                .build()
                );
            }
        }
        return responseList;
    }

    /** Form-level edit lock — approved / waiting premixes are read-only for the user. */
    private boolean isPremixFormLocked(Status premixSubmissionStatus) {
        return premixSubmissionStatus == Status.WAITING_FOR_APPROVAL
                || premixSubmissionStatus == Status.APPROVED;
    }

    private boolean hasSubmittedPremix(List<PremixRequest> premixes) {
        return premixes != null && premixes.stream()
                .filter(Objects::nonNull)
                .anyMatch(premix -> premix.getPremixSubmissionType() == Status.SUBMIT);
    }

    private Premix mapPremix(PremixRequest request) {

        Status premixStatus =
                request.getPremixSubmissionType() == Status.DRAFT
                        ? Status.IN_PROGRESS
                        : Status.WAITING_FOR_APPROVAL;

        return Premix.builder()
                .premixNo(request.getPremixNo())
                .premixDate(request.getPremixDate())
                .materialType(request.getMaterialType())

                .premixSubmissionType(request.getPremixSubmissionType())
                .premixSubmissionStatus(premixStatus)
                .rejectionReason(null)

                .solidProcess(mapMaterialProcessList(request.getSolidProcess()))
                .liquidProcess(mapMaterialProcessList(request.getLiquidProcess()))
                .build();
    }

    private static String resolveFormRejectionReasonFromPremixes(List<Premix> premixes) {
        if (premixes == null) {
            return null;
        }
        return premixes.stream()
                .filter(p -> p != null && p.getPremixSubmissionStatus() == Status.REJECTED)
                .map(Premix::getRejectionReason)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElse(null);
    }

    private void validateMaterials(PreparationDetailsRequest details) {
        if (details.getPremixes() != null) {
            for (PremixRequest premix : details.getPremixes()) {
                validateMaterialList(premix.getSolidProcess());
                validateMaterialList(premix.getLiquidProcess());
            }
        }
    }

    private void validateMaterialList(List<MaterialProcessRequest> processes) {
        if (processes == null) return;
        for (MaterialProcessRequest mp : processes) {
            var material = materialRepo.findByMaterialIdAndMaterialCode(mp.getMaterialId(), mp.getMaterialCode())
                    .orElseThrow(() -> new AcemException(ErrorCode.MATERIAL_NOT_FOUND,
                            "Material not found: id=" + mp.getMaterialId() + " code=" + mp.getMaterialCode()));
            if (!MaterialMasterDataService.isActive(material)) {
                throw new AcemException(ErrorCode.INVALID_REQUEST,
                        "Material is inactive: " + mp.getMaterialCode());
            }
            assertProcessTypeMatchesTemplate(material, mp.getProcessType());
        }
    }

    private void assertProcessTypeMatchesTemplate(
            in.co.bel.acempp.entity.MaterialMaster material,
            String processType) {
        if (processType == null || processType.isBlank()) {
            return;
        }
        RmpFormTemplate template = MaterialMasterDataService.resolveRmpFormTemplate(material);
        String pt = processType.trim().toUpperCase(Locale.ROOT);
        boolean allowed = switch (template) {
            case AP -> pt.equals("AP_COARSE") || pt.equals("AP_FINE") || pt.equals("AP_ULTRA_FINE");
            case ALUMINUM -> pt.equals("ALUMINUM") || pt.equals("ALUMINIUM");
            case DOA -> pt.equals("DOA");
            case HTPB -> pt.equals("HTPB") || pt.equals("DEFAULT_LIQUID");
            case TDI -> pt.equals("TDI");
            case CC -> pt.equals("CC");
            case IO -> pt.equals("IO");
            case NONOX_D -> pt.equals("NONOX_D") || pt.equals("NONOXD");
            case DEFAULT -> pt.equals("DEFAULT_SOLID") || pt.equals("DEFAULT_LIQUID");
        };
        if (!allowed) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "processType " + processType + " is not allowed for material "
                            + material.getMaterialCode() + " (rmpFormTemplate=" + template + ")");
        }
    }

    private PreparationDetails mapPreparationDetails(PreparationDetailsRequest req) {
        return PreparationDetails.builder()
                .premixes(req.getPremixes().stream().map(this::mapPremix).collect(Collectors.toList()))
                .weightmentSheet(req.getWeightmentSheet() != null
                        ? mapWeightmentSheet(req.getWeightmentSheet()) : null)
                .build();
    }


    private List<MaterialProcess> mapMaterialProcessList(List<MaterialProcessRequest> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(mp -> {
            validateLotDetails(mp);
            String processType = mp.getProcessType() == null ? "" : mp.getProcessType();
            boolean hasTypedApBlock =
                    mp.getApCoarse() != null || mp.getApFine() != null || mp.getApUltraFine() != null;
            // Specialty typed processes clear top-level drying/sieving (except CC which keeps both).
            // Keep drying/sieving on AP when the typed AP block was not sent, so draft data is not wiped.
            boolean clearTopLevelDryingSieving =
                    (("AP_COARSE".equalsIgnoreCase(processType)
                            || "AP_FINE".equalsIgnoreCase(processType)
                            || "AP_ULTRA_FINE".equalsIgnoreCase(processType)) && hasTypedApBlock)
                            || "ALUMINUM".equalsIgnoreCase(processType)
                            || "ALUMINIUM".equalsIgnoreCase(processType)
                            || "DOA".equalsIgnoreCase(processType)
                            || "HTPB".equalsIgnoreCase(processType)
                            || "TDI".equalsIgnoreCase(processType)
                            || "IO".equalsIgnoreCase(processType)
                            || "NONOX_D".equalsIgnoreCase(processType)
                            || "NONOXD".equalsIgnoreCase(processType);
            return MaterialProcess.builder()
                    .materialId(mp.getMaterialId())
                    .materialCode(mp.getMaterialCode())
                    .materialName(mp.getMaterialName())
                    .gradeId(mp.getGradeId())
                    .gradeCode(mp.getGradeCode())
                    .processType(mp.getProcessType())
                    .lotDetails(mp.getLotDetails() != null ? mp.getLotDetails() : new ArrayList<>())
                    .drying(clearTopLevelDryingSieving ? null : mp.getDrying())
                    .sieving(clearTopLevelDryingSieving ? null : mp.getSieving())
                    .apCoarse(mp.getApCoarse())
                    .apFine(mp.getApFine())
                    .apUltraFine(mp.getApUltraFine())
                    .aluminum(mp.getAluminum())
                    .doa(mp.getDoa())
                    .sections(null)
                    .build();
        }).collect(Collectors.toList());
    }

    private void validateLotDetails(MaterialProcessRequest mp) {
        List<LotDetailDto> lots = mp.getLotDetails();
        if (lots == null || lots.isEmpty()) return;
        Set<String> seen = new HashSet<>();
        for (LotDetailDto lot : lots) {
            if (lot == null
                    || lot.getLotId() == null
                    || lot.getLotId().isBlank()
                    || lot.getQuantity() == null
                    || lot.getQuantity() <= 0) {
                throw new AcemException(ErrorCode.INVALID_REQUEST,
                        "Each lot detail requires lotId and a positive quantity for material "
                                + mp.getMaterialCode());
            }
            String key = lot.getLotId().trim();
            if (!seen.add(key)) {
                throw new AcemException(ErrorCode.INVALID_REQUEST,
                        "Duplicate lotId in lot details for material " + mp.getMaterialCode()
                                + ": " + key);
            }
        }
    }

    private WeightmentSheet mapWeightmentSheet(WeightmentSheetRequest ws) {
        if (ws == null) {
            return null;
        }

        // Safely handle weightmentDetails when UI sends an empty weightmentSheet {}
        List<WeightmentDetail> mappedDetails = (ws.getWeightmentDetails() == null)
                ? Collections.emptyList()
                : ws.getWeightmentDetails().stream()
                .map(wd -> WeightmentDetail.builder()
                        .materialCode(wd.getMaterialCode())
                        .materialName(wd.getMaterialName())
                        .gradeCode(wd.getGradeCode())
                        .gradeName(wd.getGradeName())
                        .premixNo(wd.getPremixNo())
                        .percentage(wd.getPercentage())
                        .weightTransferred(wd.getWeightTransferred())
                        .containerType(wd.getContainerType())
                        .containerNumber(wd.getContainerNumber())
                        .weighScaleNumber(wd.getWeighScaleNumber())
                        .weighingDateTime(wd.getWeighingDateTime())
                        .build())
                .collect(Collectors.toList());

        return WeightmentSheet.builder()
                .mixerBuildingNumber(ws.getMixerBuildingNumber())
                .weightmentDetails(mappedDetails)
                .validation(ws.getValidation() != null ? Validation.builder()
                        .compareWithIdentificationSheet(ws.getValidation().isCompareWithIdentificationSheet())
                        .deviationFound(ws.getValidation().isDeviationFound())
                        .deviationMessage(ws.getValidation().getDeviationMessage())
                        .build() : null)
                .premixSheets(ws.getPremixSheets() == null
                        ? null
                        : ws.getPremixSheets().stream()
                        .map(ps -> WeightmentPremixSheet.builder()
                                .premixNo(ps.getPremixNo())
                                .mixerBuildingNumber(ps.getMixerBuildingNumber())
                                .validation(ps.getValidation() != null ? Validation.builder()
                                        .compareWithIdentificationSheet(
                                                ps.getValidation().isCompareWithIdentificationSheet())
                                        .deviationFound(ps.getValidation().isDeviationFound())
                                        .deviationMessage(ps.getValidation().getDeviationMessage())
                                        .build() : null)
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
