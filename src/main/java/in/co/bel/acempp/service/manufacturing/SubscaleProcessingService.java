package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.subscale.*;
import in.co.bel.acempp.dto.batch.BatchMetadata;
import in.co.bel.acempp.dto.subscale.subscalecreation.CastingDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.CastingTableDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.CreateSubscaleProcessingRequest;
import in.co.bel.acempp.dto.subscale.subscalecreation.CuringDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.CuringTableDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.HardwareArticleSelectionDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.HardwarePreparationDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.InhibitionDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.InhibitionTableDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.NdtDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.NdtTableDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.StaticTestingDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.StaticTestingTableDTO;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.batch.BatchIdentificationSheet;
import in.co.bel.acempp.entity.batch.SubscaleArticleMaster;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.manufacturing.*;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.*;
import in.co.bel.acempp.repository.manufacturing.*;
import in.co.bel.acempp.service.BatchService;
import in.co.bel.acempp.service.FileBindingService;
import in.co.bel.acempp.service.FileMetadataEnrichmentService;
import in.co.bel.acempp.service.FileReadOnlyService;
import in.co.bel.acempp.service.FormIdGeneratorService;
import in.co.bel.acempp.service.SubscaleMetadataResolver;
import in.co.bel.acempp.service.batch.MainBatchGateValidator;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscaleProcessingService {

    private final SubscaleProcessingRepository repository;
    private final BatchRepository batchRepo;
    private final UserRepository userRepo;
    private final SubscaleArticleRepository subscaleArticleRepository;
    private final BatchService batchService;
    private final FormIdGeneratorService formIdGenerator;
    private final FileBindingService fileBindingService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final MainBatchGateValidator mainBatchGateValidator;
    private final SubscaleMetadataResolver subscaleMetadataResolver;
    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;
    private final RawMaterialPrepService rawMaterialPrepService;

    public SubscaleProcessingResponse createForm(CreateSubscaleProcessingRequest request, String userId) {

        // 1. validate batch
        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(request.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId " + request.getBatchId()));

        // 2. batch must be at the Subscale stage (subDeptId = 6)
        batchService.assertStageUnlockedAndActive(
                batch, request.getSubDepartmentId(), "Subscale Processing");
        mainBatchGateValidator.assertSubscaleCanBeStarted(batch);

        // 3. validate batch type consistency
        if (batch.getBatchType() != null && !batch.getBatchType().equalsIgnoreCase(request.getBatchType())) {
            throw new AcemException(ErrorCode.INVALID_STATE,
                    "Batch type does not match: expected " + batch.getBatchType() + " but got " + request.getBatchType());
        }

        // 4. duplicate check
        if (repository.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(ErrorCode.FORM_ALREADY_EXISTS,
                    "Subscale Processing form already submitted for this batch");
        }

        backfillCastingBemNos(
                request.getCastingDetails(),
                request.getCuringDetails(),
                request.getNdtDetails());
        assertUniqueCastingBemNos(request.getCastingDetails() != null
                ? request.getCastingDetails().getCastingTable()
                : null);
        enrichHardwareArticleIsBem(request.getHardwarePreparationDetails());

        // 5. determine status
        boolean isDraft = request.getFormSubmissionType() == Status.DRAFT;
        Status status = isDraft
                ? Status.IN_PROGRESS
                : Status.WAITING_FOR_APPROVAL;

        // 6. generate form ID
        String formId = formIdGenerator.generate(
                "subscale_processing",
                "formId",
                "SSP",
                3
        );

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            // Promote file attachments and collect successfully promoted ones
            promotedDocs = promoteStaticTestingGraphFiles(
                    request.getStaticTestingDetails(),
                    request.getSubDepartmentId(),
                    formId,
                    Collections.emptySet()
            );

            // 7. build entity
            User creator = userRepo.findByUserId(userId)
                    .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));

            LocalDateTime now = LocalDateTime.now();
            PersonInfo creatorInfo = PersonInfo.builder()
                    .id(userId)
                    .fullName(creator.getUsername())
                    .build();
            SubscaleProcessing entity = SubscaleProcessing.builder()
                    .formId(formId)
                    .batchId(request.getBatchId())
                    .batchType(request.getBatchType().toUpperCase())
                    .subDepartmentId(request.getSubDepartmentId())
                    .formSubmissionType(request.getFormSubmissionType())
                    .status(status)
                    .subscaleDetails(request.getSubscaleDetails())
                    .hardwarePreparationDetails(request.getHardwarePreparationDetails())
                    .hardwarePreparationTable(request.getHardwarePreparationTable())
                    .castingDetails(request.getCastingDetails())
                    .curingDetails(request.getCuringDetails())
                    .ndtDetails(request.getNdtDetails())
                    .trimmingDetails(request.getTrimmingDetails())
                    .inhibitionDetails(normalizeInhibitionDetails(request.getInhibitionDetails()))
                    .staticTestingDetails(request.getStaticTestingDetails())
                    .mechanicalInterfaceProperties(request.getMechanicalInterfaceProperties())
                    .createdBy(creatorInfo)
                    .createdAt(now)
                    .submittedBy(isDraft ? null : creatorInfo)
                    .submittedOn(isDraft ? null : now)
                    .build();

            entity = repository.save(entity);

            batchService.syncStageProgress(
                    batch.getBatchId(), request.getSubDepartmentId(), status,
                    null, null, null,
                    isDraft ? null : creatorInfo,
                    now, false, null, null);

            log.info("Subscale Processing form {} created by {}", formId, userId);

            if (batch.getBatchType() != null && batch.getBatchType().equalsIgnoreCase("SUBSCALE")) {
                Batch lockedBatch = batchRepo.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                        .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                                "No batch exists for batchId " + batch.getBatchId()));
                lockedBatch.setLockedForEdit(true);
                batchRepo.save(lockedBatch);
            }
            // 9. build response
            SubscaleProcessingResponse.SubscaleProcessingResponseBuilder builder = SubscaleProcessingResponse.builder()
                    .formId(formId)
                    .batchId(request.getBatchId())
                    .batchType(entity.getBatchType())
                    .status(entity.getStatus());

            if (isDraft) {
                builder.createdBy(userId)
                        .createdOn(entity.getCreatedAt());
            } else {
                builder.submittedBy(userId)
                        .submittedOn(now);
            }

            return builder.build();

        } catch (Exception ex) {
            log.error("Subscale Processing form creation failed for formId={}. Initiating file promotion rollback...", formId, ex);

            // Rollback promoted files if DB operation or entity creation failed
            rollbackPromotedFiles(promotedDocs);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to create Subscale Processing form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public UpdateSubscaleProcessingResponse updateForm(CreateSubscaleProcessingRequest request, String userId) {

        // 1. Find existing form
        SubscaleProcessing form = repository.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(ErrorCode.FORM_NOT_FOUND,
                        "Subscale Processing form not found for formId: " + request.getFormId()));

        // 2. Block if already submitted/approved
        Status status = form.getStatus();

        if (status == Status.WAITING_FOR_APPROVAL || status == Status.COMPLETELY_APPROVED) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Submitted forms cannot be modified"
            );
        }

        // 3. Validate batch
        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(request.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId: " + request.getBatchId()));

        // 4. Batch must be at Subscale Processing stage (subDeptId = 6)
        batchService.assertStageUnlockedAndActive(
                batch, request.getSubDepartmentId(), "Subscale Processing");
        mainBatchGateValidator.assertSubscaleCanBeStarted(batch);

        // 5. Batch type consistency
        if (batch.getBatchType() != null && !batch.getBatchType().equalsIgnoreCase(request.getBatchType())) {
            throw new AcemException(ErrorCode.INVALID_STATE,
                    "Batch type does not match: expected " + batch.getBatchType() + " but got " + request.getBatchType());
        }

        backfillCastingBemNos(
                request.getCastingDetails(),
                request.getCuringDetails(),
                request.getNdtDetails());
        assertUniqueCastingBemNos(request.getCastingDetails() != null
                ? request.getCastingDetails().getCastingTable()
                : null);
        enrichHardwareArticleIsBem(request.getHardwarePreparationDetails());

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            // 1. Extract DB file IDs and Payload file IDs
            Set<String> existingDbFileIds = extractStaticTestingFileIds(form.getStaticTestingDetails());
            Set<String> incomingFileIds = extractStaticTestingFileIds(request.getStaticTestingDetails());

            // 2. Identify removed file IDs (present in DB, absent in Payload)
            List<String> removedFileIds = existingDbFileIds.stream()
                    .filter(fileId -> !incomingFileIds.contains(fileId))
                    .collect(Collectors.toList());

            // 3. Promote only new files
            promotedDocs = promoteStaticTestingGraphFiles(
                    request.getStaticTestingDetails(),
                    request.getSubDepartmentId(),
                    request.getFormId(),
                    existingDbFileIds
            );

            // 4. Purge removed files permanently from disk and DB
            if (!removedFileIds.isEmpty()) {
                fileBindingService.purgeRemovedFilesFromDiskAndDb(request.getFormId(), removedFileIds);
            }
            // 6. Determine new status
            boolean isDraft = request.getFormSubmissionType() == Status.DRAFT;
            Status newStatus = isDraft
                    ? Status.IN_PROGRESS
                    : Status.WAITING_FOR_APPROVAL;

            // 7. Update form
            form.setFormSubmissionType(request.getFormSubmissionType());
            form.setStatus(newStatus);
            form.setSubscaleDetails(request.getSubscaleDetails());
            form.setHardwarePreparationDetails(request.getHardwarePreparationDetails());
            form.setHardwarePreparationTable(request.getHardwarePreparationTable());
            form.setCastingDetails(request.getCastingDetails());
            form.setCuringDetails(request.getCuringDetails());
            form.setNdtDetails(request.getNdtDetails());
            form.setTrimmingDetails(request.getTrimmingDetails());
            form.setInhibitionDetails(normalizeInhibitionDetails(request.getInhibitionDetails()));
            form.setStaticTestingDetails(request.getStaticTestingDetails());
            form.setMechanicalInterfaceProperties(request.getMechanicalInterfaceProperties());   // full replacement

            User updater = userRepo.findByUserId(userId)
                    .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
            LocalDateTime now = LocalDateTime.now();
            PersonInfo updaterInfo = PersonInfo.builder()
                    .id(userId)
                    .fullName(updater.getUsername())
                    .build();
            form.setUpdatedBy(updaterInfo);
            form.setUpdatedAt(now);
            if (!isDraft) {
                form.setSubmittedBy(updaterInfo);
                form.setSubmittedOn(now);
            }

            form = repository.save(form);

            batchService.syncStageProgress(
                    batch.getBatchId(), form.getSubDepartmentId(), newStatus,
                    null, null, null,
                    isDraft ? null : updaterInfo,
                    now, false, form.getRemarks(), form.getRejectionReason());

            log.info("Subscale Processing form {} updated by {}", form.getFormId(), userId);

            // 9. Build response
            UpdateSubscaleProcessingResponse.UpdateSubscaleProcessingResponseBuilder builder =
                    UpdateSubscaleProcessingResponse.builder()
                            .formId(form.getFormId())
                            .batchId(form.getBatchId())
                            .batchType(form.getBatchType())
                            .status(form.getStatus());

            if (isDraft) {
                builder.updatedBy(userId)
                        .updatedAt(form.getUpdatedAt());
            } else {
                builder.submittedBy(userId)
                        .submittedOn(now);
            }

            return builder.build();

        } catch (Exception ex) {
            log.error("Subscale Processing form update failed for formId={}. Initiating file promotion rollback...", request.getFormId(), ex);

            // Rollback promoted files if form update failed
            rollbackPromotedFiles(promotedDocs);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to update Subscale Processing form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    private List<DocumentDto> promoteStaticTestingGraphFiles(
            StaticTestingDetailsDTO staticTestingDetails,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds) {

        List<DocumentDto> promotedDocs = new ArrayList<>();

        if (staticTestingDetails == null || staticTestingDetails.getStaticTestingTable() == null) {
            return promotedDocs;
        }

        for (StaticTestingTableDTO row : staticTestingDetails.getStaticTestingTable()) {
            if (row == null) {
                continue;
            }

            DocumentDto graph = row.getGraph();
            if (graph != null && graph.getFileId() != null && !graph.getFileId().isBlank()) {

                // Skip re-promoting files that already exist in DB
                if (existingFileIds.contains(graph.getFileId())) {
                    fileMetadataEnrichmentService.enrichDocument(graph, subDeptId);
                    continue;
                }

                try {
                    // Reusable promotion call
                    FileAttachments savedFile = fileBindingService.promoteAndBindFile(
                            graph.getFileId(),
                            subDeptId,
                            formId
                    );
                    graph.setFileId(savedFile.getFileId());
                    graph.setFileUrl(savedFile.getFilePath());
                    graph.setStatus(savedFile.getStatus());
                    graph.setFileName(savedFile.getOriginalFileName());
                    graph.setMimeType(savedFile.getMimeType());
                    graph.setReadOnly(savedFile.isReadOnly());
                    promotedDocs.add(graph);

                } catch (Exception e) {
                    log.error("File promotion failed for fileId: {}", graph.getFileId(), e);
                    throw new AcemException(
                            ErrorCode.FILE_TRANSFER_ERROR,
                            "File upload failed, please try again."
                    );
                }
            }
        }
        return promotedDocs;
    }

    private void rollbackPromotedFiles(List<DocumentDto> promotedDocs) {
        if (promotedDocs == null || promotedDocs.isEmpty()) {
            return;
        }
        log.info("Starting rollback for {} promoted static testing graph files...", promotedDocs.size());
        for (DocumentDto doc : promotedDocs) {
            if (doc != null && doc.getFileId() != null) {
                try {
                    fileBindingService.rollbackOrUnbindFile(doc.getFileId());
                } catch (Exception e) {
                    log.error("Failed to execute file rollback for fileId={}", doc.getFileId(), e);
                }
            }
        }
    }

    private Set<String> extractStaticTestingFileIds(StaticTestingDetailsDTO staticTestingDetails) {
        Set<String> fileIds = new HashSet<>();
        if (staticTestingDetails == null || staticTestingDetails.getStaticTestingTable() == null) {
            return fileIds;
        }

        for (StaticTestingTableDTO row : staticTestingDetails.getStaticTestingTable()) {
            if (row != null && row.getGraph() != null) {
                DocumentDto graph = row.getGraph();
                if (graph.getFileId() != null && !graph.getFileId().isBlank()) {
                    fileIds.add(graph.getFileId());
                }
            }
        }
        return fileIds;
    }

    private void enrichStaticTestingGraphs(StaticTestingDetailsDTO staticTestingDetails, Long subDeptId) {
        if (staticTestingDetails == null || staticTestingDetails.getStaticTestingTable() == null) {
            return;
        }
        for (StaticTestingTableDTO row : staticTestingDetails.getStaticTestingTable()) {
            if (row != null && row.getGraph() != null) {
                fileMetadataEnrichmentService.enrichDocument(row.getGraph(), subDeptId);
            }
        }
    }

    public SubscaleProcessingDetailsResponse getFormDetails(String formId) {
        SubscaleProcessing form = repository.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(ErrorCode.FORM_NOT_FOUND,
                        "No Subscale Processing form found for formId: " + formId));
        Batch batch = batchRepo.findByBatchId(form.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch not found"));

        StaticTestingDetailsDTO staticTestingDetails = form.getStaticTestingDetails();
        enrichStaticTestingGraphs(staticTestingDetails, form.getSubDepartmentId());

        // Older drafts may lack casting.bemNo even when curing/NDT still hold the ID.
        boolean castingBemBackfilled = backfillCastingBemNos(
                form.getCastingDetails(),
                form.getCuringDetails(),
                form.getNdtDetails());
        if (castingBemBackfilled) {
            repository.save(form);
        }

        return SubscaleProcessingDetailsResponse.builder()
                .subscaleProcessingId(form.getFormId())
                .batchId(form.getBatchId())
                .batchType(form.getBatchType())
                .subDepartmentId(form.getSubDepartmentId())
                .formSubmissionType(form.getFormSubmissionType())
                .status(form.getStatus())
                .createdBy(batch.getCreatedBy() != null ? batch.getCreatedBy().getId() : null)
                .createdAt(batch.getCreatedAt())
                .submittedBy(form.getSubmittedBy() != null
                        ? form.getSubmittedBy()
                        : form.getFormSubmissionType() == Status.SUBMIT ? form.getCreatedBy() : null)
                .submittedAt(form.getSubmittedOn() != null
                        ? form.getSubmittedOn()
                        : form.getFormSubmissionType() == Status.SUBMIT && form.getCreatedAt() != null
                        ? form.getCreatedAt()
                        : null)
                .approvedBy(form.getApprovedBy())
                .approvedOn(form.getApprovedOn())

                .subscaleDetails(form.getSubscaleDetails())
                .hardwarePreparationDetails(form.getHardwarePreparationDetails())
                .hardwarePreparationTable(form.getHardwarePreparationTable())
                .castingDetails(form.getCastingDetails())
                .curingDetails(form.getCuringDetails())
                .ndtDetails(form.getNdtDetails())
                .trimmingDetails(form.getTrimmingDetails())
                .inhibitionDetails(form.getInhibitionDetails())
                .staticTestingDetails(staticTestingDetails)
                .mechanicalInterfaceProperties(form.getMechanicalInterfaceProperties())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverName) {

        SubscaleProcessing entity = repository.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "Not found",
                        "Subscale Processing form not found: " + request.getFormId()));
        Batch batch = batchRepo.findByBatchId(entity.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch not found"));
        if (entity.getStatus() != Status.WAITING_FOR_APPROVAL) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Form is not waiting for approval");
        }

        User approver = userRepo.findByUserId(approverName)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        PersonInfo approverInfo = PersonInfo.builder()
                .id(approver.getUserId())
                .fullName(approver.getUsername())
                .build();

        ApprovalStatusUtil.applyStatusChange(request.getActionType(), request.getSubDepartmentId(), request.getRemarks(), request.getRejectionReason(),
                approver.getUsername(), status -> entity.setStatus(status),
                entity::setRemarks, entity::setRejectionReason, entity::setUpdatedBy, entity::setUpdatedAt
        );
        entity.setUpdatedBy(approverInfo);
        entity.setApprovedBy(approverInfo);
        entity.setApprovedOn(entity.getUpdatedAt());
        boolean isSubscaleBatch = "SUBSCALE".equalsIgnoreCase(batch.getBatchType());
        LocalDateTime now = entity.getUpdatedAt(); // capture timestamp for audit
        if (request.getActionType() == Status.APPROVED) {
            try {
                if (isSubscaleBatch) {
                    Map<String, Object> batchBlockchainPayload = rawMaterialPrepService.buildBatchBlockchainPayload(batch);
                    approvalBlockchainAssetService.createBatchAssetOnBlockchain(
                            entity.getBatchId(),
                            batchBlockchainPayload,
                            approverName
                    );
                }

                Map<String, Object> blockchainPayload = buildSubscaleBlockchainPayload(entity, approverName, now);
                approvalBlockchainAssetService.createAssetOnBlockchain(
                        entity.getSubDepartmentId(),
                        entity.getBatchId(),
                        entity.getFormId(),
                        blockchainPayload,
                        null,
                        approverName);
            } catch (Exception e) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new AcemException(ErrorCode.INVALID_REQUEST, "Blockchain sync failed: " + e.getMessage());
            }
        }
        if (request.getActionType() == Status.APPROVED
                || request.getActionType() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(entity.getFormId(), entity.getSubDepartmentId());
        }

        enrichHardwareArticleIsBem(entity.getHardwarePreparationDetails());
        repository.save(entity);
        if (entity.getStatus() == Status.COMPLETELY_APPROVED) {
            persistApprovedSubScaleMetadata(entity);
        }
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

    private Map<String, Object> buildSubscaleBlockchainPayload(SubscaleProcessing form, String approverName, LocalDateTime approvalTime) {
        Map<String, Object> rootPayload = new LinkedHashMap<>();

        // 1. Audit / Metadata Object (Separated created & approved info)
        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getSubmittedBy() != null) {
                auditMap.put("createdBy", form.getSubmittedBy().getFullName());
            }
            if (form.getCreatedAt() != null) {
                auditMap.put("createdAt", form.getCreatedAt());
            }
        }

        // Fallback or explicit injection of approval details
        if (approverName != null) {
            auditMap.put("approvedBy", approverName);
            auditMap.put("approvedAt", approvalTime);
        } else if (form != null && form.getApprovedBy() != null) {
            auditMap.put("approvedBy", form.getApprovedBy().getFullName());
            auditMap.put("approvedAt", form.getApprovedOn());
        }
        rootPayload.put("audit", auditMap);

        // 2. Subscale Technical & Process Details Object
        Map<String, Object> subscaleDetailsMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getSubscaleDetails() != null) {
                var subscale = form.getSubscaleDetails();
                Map<String, Object> subscaleMap = new LinkedHashMap<>();
                subscaleMap.put("batchSize", subscale.getBatchSize());
                subscaleMap.put("premixDate", subscale.getPremixDate());
                subscaleMap.put("finalMixDate", subscale.getFinalMixDate());
                subscaleMap.put("mixerType", subscale.getMixerType());
                subscaleMap.put("bldgNo", subscale.getBldgNo());

                if (subscale.getMixingCycle() != null) {
                    var cycle = subscale.getMixingCycle();
                    Map<String, Object> cycleMap = new LinkedHashMap<>();
                    cycleMap.put("motorStage", cycle.getMotorStage());
                    cycleMap.put("mixingCycleCode", cycle.getMixingCycleCode());
                    cycleMap.put("mixingCycleName", cycle.getMixingCycleName());
                    cycleMap.put("premixParticulars", cycle.getPremixParticulars() == null ? Collections.emptyList() : cycle.getPremixParticulars());
                    cycleMap.put("finalMixParticulars", cycle.getFinalMixParticulars() == null ? Collections.emptyList() : cycle.getFinalMixParticulars());
                    subscaleMap.put("mixingCycle", cycleMap);
                }
                subscaleDetailsMap.put("subscaleDetails", subscaleMap);
            }

            // Hardware Preparation Details & Table
            if (form.getHardwarePreparationDetails() != null || form.getHardwarePreparationTable() != null) {
                Map<String, Object> hardwareMap = new LinkedHashMap<>();
                hardwareMap.put("details", form.getHardwarePreparationDetails());
                hardwareMap.put("table", form.getHardwarePreparationTable() == null ? Collections.emptyList() : form.getHardwarePreparationTable());
                subscaleDetailsMap.put("hardwarePreparation", hardwareMap);
            }

            // Casting Details
            if (form.getCastingDetails() != null) {
                var casting = form.getCastingDetails();
                Map<String, Object> castingMap = new LinkedHashMap<>();
                castingMap.put("dateOfCasting", casting.getDateOfCastingDateTime());
                castingMap.put("castingTable", casting.getCastingTable() == null ? Collections.emptyList() : casting.getCastingTable());
                subscaleDetailsMap.put("castingDetails", castingMap);
            }

            // Curing Details
            if (form.getCuringDetails() != null) {
                var curing = form.getCuringDetails();
                Map<String, Object> curingMap = new LinkedHashMap<>();
                curingMap.put("curingTable", curing.getCuringTable() == null ? Collections.emptyList() : curing.getCuringTable());
                subscaleDetailsMap.put("curingDetails", curingMap);
            }

            // NDT Details
            if (form.getNdtDetails() != null) {
                var ndt = form.getNdtDetails();
                Map<String, Object> ndtMap = new LinkedHashMap<>();
                ndtMap.put("ndtTable", ndt.getNdtTable() == null ? Collections.emptyList() : ndt.getNdtTable());
                subscaleDetailsMap.put("ndtDetails", ndtMap);
            }

            // Trimming Details
            if (form.getTrimmingDetails() != null) {
                var trimming = form.getTrimmingDetails();
                Map<String, Object> trimmingMap = new LinkedHashMap<>();
                trimmingMap.put("trimmingTable", trimming.getTrimmingTable() == null ? Collections.emptyList() : trimming.getTrimmingTable());
                subscaleDetailsMap.put("trimmingDetails", trimmingMap);
            }

            // Inhibition Details
            if (form.getInhibitionDetails() != null) {
                var inhibition = form.getInhibitionDetails();
                Map<String, Object> inhibitionMap = new LinkedHashMap<>();
                inhibitionMap.put("irBatchNo", inhibition.getIrBatchNo());
                inhibitionMap.put("dateOfManufacturing", inhibition.getDateOfManufacturingDateTime());
                inhibitionMap.put("inhibitionTable", inhibition.getInhibitionTable() == null ? Collections.emptyList() : inhibition.getInhibitionTable());
                subscaleDetailsMap.put("inhibitionDetails", inhibitionMap);
            }

            // Static Testing Details
            if (form.getStaticTestingDetails() != null) {
                var staticTesting = form.getStaticTestingDetails();
                Map<String, Object> staticTestingMap = new LinkedHashMap<>();
                staticTestingMap.put("staticTestingTable", staticTesting.getStaticTestingTable() == null ? Collections.emptyList() : staticTesting.getStaticTestingTable());
                subscaleDetailsMap.put("staticTestingDetails", staticTestingMap);
            }

            // Mechanical Interface Properties
            if (form.getMechanicalInterfaceProperties() != null) {
                var mechanical = form.getMechanicalInterfaceProperties();
                Map<String, Object> mechanicalMap = new LinkedHashMap<>();
                mechanicalMap.put("mechanicalPropertiesTable", mechanical.getMechanicalPropertiesTable() == null ? Collections.emptyList() : mechanical.getMechanicalPropertiesTable());
                subscaleDetailsMap.put("mechanicalInterfaceProperties", mechanicalMap);
            }
        }

        rootPayload.put("subscaleDetails", subscaleDetailsMap);

        return rootPayload;
    }
    private void enrichHardwareArticleIsBem(HardwarePreparationDetailsDTO hardware) {
        if (hardware == null || hardware.getArticles() == null || hardware.getArticles().isEmpty()) {
            return;
        }
        for (HardwareArticleSelectionDTO article : hardware.getArticles()) {
            if (article == null) {
                continue;
            }
            SubscaleArticleMaster master = null;
            if (article.getSubscaleArticleId() != null && article.getSubscaleArticleId() > 0) {
                master = subscaleArticleRepository
                        .findBySubscaleArticleId(article.getSubscaleArticleId())
                        .orElse(null);
            }
            if (master == null
                    && article.getSubscaleArticleCode() != null
                    && !article.getSubscaleArticleCode().isBlank()) {
                master = subscaleArticleRepository
                        .findBySubscaleArticleCode(article.getSubscaleArticleCode().trim())
                        .orElse(null);
            }
            if (master == null) {
                continue;
            }
            boolean isBem = Boolean.TRUE.equals(master.getIsBem());
            article.setIsBem(isBem);
            if (!isBem) {
                article.setNoOfParts(0);
            }
        }
    }

    /**
     * Persist {@code identificationSheet.metadata.subScale} (including bemMotors) when the form
     * becomes COMPLETELY_APPROVED — same pattern as Mixing/RMP metadata writes.
     */
    private void persistApprovedSubScaleMetadata(SubscaleProcessing entity) {
        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(entity.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId " + entity.getBatchId()));

        BatchIdentificationSheet sheet = batch.getIdentificationSheet();
        if (sheet == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Batch identification sheet is not available");
        }

        BatchMetadata metadata = sheet.getMetadata();
        if (metadata == null) {
            metadata = new BatchMetadata();
            sheet.setMetadata(metadata);
        }

        metadata.setSubScale(subscaleMetadataResolver.buildMetadata(entity));
        batchRepo.save(batch);
        log.info(
                "Persisted subscale metadata.bemMotors for batch {} from form {}",
                entity.getBatchId(),
                entity.getFormId());
    }

    /**
     * Keep section {@code dateOfApplication} populated when the UI only sends per-row dates.
     */
    private InhibitionDetailsDTO normalizeInhibitionDetails(InhibitionDetailsDTO details) {
        if (details == null) {
            return null;
        }

        List<InhibitionTableDTO> rows = details.getInhibitionTable();
        if (rows == null) {
            return details;
        }
        for (InhibitionTableDTO row : rows) {
            if (row == null || row.getDateOfApplicationDateTime() == null) {
                continue;
            }
            break;
        }
        return details;
    }

    /**
     * Article No / BEM No must be unique within the same article type (casting rows).
     * The same BEM may appear under different article types. Casting mould no
     * ({@code bemMouldNo}) is non-unique — never validated here.
     */
    private void assertUniqueCastingBemNos(List<CastingTableDTO> castingRows) {
        if (castingRows == null || castingRows.isEmpty()) {
            return;
        }
        Set<String> uniqueKeys = new HashSet<>();
        for (CastingTableDTO row : castingRows) {
            if (row == null) {
                continue;
            }
            String bemNo = row.getBemNo();
            if (bemNo == null || bemNo.isBlank()) {
                continue;
            }
            String normalizedBem = bemNo.trim();
            String typeKey = normalizeCastingArticleType(row.getArticleType());
            String composite = typeKey + "\0" + normalizedBem;
            if (!uniqueKeys.add(composite)) {
                String typeLabel = typeKey.isEmpty() ? "(unspecified)" : typeKey;
                throw new AcemException(
                        ErrorCode.FORM_ALREADY_EXISTS,
                        "Duplicate Article No / BEM No '" + normalizedBem
                                + "' found for article type '" + typeLabel + "'.");
            }
        }
    }

    /** Align casting article-type keys for uniqueness (trim, collapse spaces, case-fold). */
    private static String normalizeCastingArticleType(String articleType) {
        if (articleType == null) {
            return "";
        }
        return articleType.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /**
     * Fill casting.bemNo when missing, using curing.bemNo then ndt.bemNo
     * (UI syncs casting Article No into those fields).
     */
    /**
     * @return true if any casting row was updated
     */
    private boolean backfillCastingBemNos(
            CastingDetailsDTO castingDetails,
            CuringDetailsDTO curingDetails,
            NdtDetailsDTO ndtDetails) {
        if (castingDetails == null || castingDetails.getCastingTable() == null) {
            return false;
        }
        List<CastingTableDTO> castingRows = castingDetails.getCastingTable();
        List<CuringTableDTO> curingRows = curingDetails != null ? curingDetails.getCuringTable() : null;
        List<NdtTableDTO> ndtRows = ndtDetails != null ? ndtDetails.getNdtTable() : null;
        boolean changed = false;

        for (int i = 0; i < castingRows.size(); i++) {
            CastingTableDTO casting = castingRows.get(i);
            if (casting == null) {
                continue;
            }
            if (casting.getBemNo() != null && !casting.getBemNo().isBlank()) {
                continue;
            }
            String fromCuring = null;
            if (curingRows != null && i < curingRows.size() && curingRows.get(i) != null) {
                fromCuring = curingRows.get(i).getBemNo();
            }
            if (fromCuring != null && !fromCuring.isBlank()) {
                casting.setBemNo(fromCuring.trim());
                changed = true;
                continue;
            }
            String fromNdt = null;
            if (ndtRows != null && i < ndtRows.size() && ndtRows.get(i) != null) {
                fromNdt = ndtRows.get(i).getBemNo();
            }
            if (fromNdt != null && !fromNdt.isBlank()) {
                casting.setBemNo(fromNdt.trim());
                changed = true;
            }
        }
        return changed;
    }
}