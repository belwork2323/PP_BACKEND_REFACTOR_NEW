package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.general.*;
import in.co.bel.acempp.dto.rawmaterialsourcing.*;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.AdductPreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.BlendingStylePreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.HtpbBlendingPreparationDetails;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.entity.sourcing.Sourcing;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.MaterialRepository;
import in.co.bel.acempp.repository.sourcing.LotRepository;
import in.co.bel.acempp.repository.sourcing.SourcingRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.service.FileBindingService;
import in.co.bel.acempp.service.FileMetadataEnrichmentService;
import in.co.bel.acempp.service.FileReadOnlyService;
import in.co.bel.acempp.service.FormIdGeneratorService;
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import in.co.bel.acempp.util.SpecResultFormatValidator;
import in.co.bel.acempp.util.FileIdExtractionUtil;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawMaterialSourcingService {

    private final SourcingRepository sourcingRepository;
    private final UserRepository userRepository;
    private final LotRepository lotRepository;
    private final MaterialRepository materialRepository;
    private final MongoTemplate mongoTemplate;
    private final FormIdGeneratorService formIdGenerator;
    private final FileBindingService filePromotionService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;

    @Transactional(rollbackFor = Exception.class)
    public RmpFormBasicResponse createForm(CreateRmpFormRequest request, String userId) {
        validateSubmissionType(request.getSubmissionType());

        if (request.getMaterials() == null || request.getMaterials().isEmpty()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "subDepartmentId and materials are required"
            );
        }

        // Aggregate ALL field errors (tier + format + prep + duplicate lot) before persist.
        Map<String, String> createFieldErrors = new LinkedHashMap<>();
        for (int materialIndex = 0; materialIndex < request.getMaterials().size(); materialIndex++) {
            MaterialRequest materialReq = request.getMaterials().get(materialIndex);
            String materialPrefix = "materials[" + materialIndex + "]";
            collectMaterialForTierErrors(
                    materialReq, request.getSubmissionType(), materialPrefix, createFieldErrors);

            if (materialReq == null || isBlank(materialReq.getMaterialCode())) {
                continue;
            }

            Optional<MaterialMaster> materialOpt =
                    materialRepository.findFirstByMaterialCode(materialReq.getMaterialCode());
            if (materialOpt.isEmpty()) {
                createFieldErrors.putIfAbsent(
                        materialPrefix + ".materialCode",
                        "Material not found: " + materialReq.getMaterialCode());
                continue;
            }
            MaterialMaster material = materialOpt.get();
            if (!MaterialMasterDataService.isActive(material)) {
                createFieldErrors.putIfAbsent(
                        materialPrefix + ".materialCode",
                        "Material is inactive: " + materialReq.getMaterialCode());
                continue;
            }

            List<MaterialSpecification> masterSpecifications =
                    activeSpecifications(material.getSpecifications());
            if (masterSpecifications == null || masterSpecifications.isEmpty()) {
                createFieldErrors.putIfAbsent(
                        materialPrefix + ".materialCode",
                        "No specifications configured for material " + material.getMaterialCode());
                continue;
            }

            if (materialReq.getLots() == null) {
                continue;
            }
            for (int lotIndex = 0; lotIndex < materialReq.getLots().size(); lotIndex++) {
                LotRequest lotReq = materialReq.getLots().get(lotIndex);
                String lotPrefix = materialPrefix + ".lots[" + lotIndex + "]";
                if (lotReq != null && !isBlank(lotReq.getLotId())
                        && lotRepository.existsByLotId(lotReq.getLotId())) {
                    createFieldErrors.put(
                            lotPrefix + ".lotId",
                            "Lot ID already exists: " + lotReq.getLotId());
                }
                if (lotReq == null) {
                    continue;
                }
                validateLotSpecificationFormats(
                        lotReq.getSpecifications(),
                        masterSpecifications,
                        request.getSubmissionType(),
                        lotPrefix + ".specifications",
                        createFieldErrors);
                validatePreparationDetails(
                        material,
                        lotReq.getAdductPreparation(),
                        lotReq.getHtpbBlendingPreparation(),
                        request.getSubmissionType(),
                        lotPrefix,
                        createFieldErrors);
            }
        }
        throwValidationErrors(createFieldErrors);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));

        String username = user.getUsername();
        boolean isDraft = request.getSubmissionType() == Status.DRAFT;

        Status status = isDraft
                ? Status.IN_PROGRESS
                : Status.WAITING_FOR_APPROVAL;

        String sourcingId = formIdGenerator.generate(
                "sourcing",
                "sourcingId",
                "RMP",
                4
        );

        // List to collect successfully promoted file IDs for rollback capability
        List<String> promotedFileIds = new ArrayList<>();

        try {
            Sourcing sourcing = Sourcing.builder()
                    .sourcingId(sourcingId)
                    .subDepartmentId(request.getSubDepartmentId())
                    .submissionType(request.getSubmissionType())
                    .status(status)
                    .createdBy(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            sourcingRepository.save(sourcing);

            for (int materialIndex = 0; materialIndex < request.getMaterials().size(); materialIndex++) {
                MaterialRequest materialReq = request.getMaterials().get(materialIndex);

                MaterialMaster material = materialRepository
                        .findFirstByMaterialCode(materialReq.getMaterialCode())
                        .orElseThrow(() ->
                                new AcemException(
                                        ErrorCode.MATERIAL_NOT_FOUND,
                                        "Material not found: " + materialReq.getMaterialCode()));
                if (!MaterialMasterDataService.isActive(material)) {
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "Material is inactive: " + materialReq.getMaterialCode());
                }

                List<MaterialSpecification> masterSpecifications =
                        activeSpecifications(material.getSpecifications());
                if (masterSpecifications == null || masterSpecifications.isEmpty()) {
                    throw new AcemException(
                            ErrorCode.SPECIFICATION_NOT_FOUND,
                            "No specifications configured for material " + material.getMaterialCode());
                }

                LocalDate receiptDate = materialReq.getReceiptDate();

                if (materialReq.getLots() == null || materialReq.getLots().isEmpty()) {
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "At least one lot required for material "
                                    + materialReq.getMaterialCode()
                    );
                }

                for (int lotIndex = 0; lotIndex < materialReq.getLots().size(); lotIndex++) {
                    LotRequest lotReq = materialReq.getLots().get(lotIndex);

                    // Duplicate / format / prep already validated above — fail closed if race.
                    if (lotRepository.existsByLotId(lotReq.getLotId())) {
                        throw new AcemException(
                                ErrorCode.DUPLICATE_LOT,
                                "Lot ID already exists: " + lotReq.getLotId()
                        );
                    }

                    List<Lot.LotSpec> lotSpecs = new ArrayList<>();

                    if (lotReq.getSpecifications() != null) {

                        for (SpecificationRequest specReq : lotReq.getSpecifications()) {

                            MaterialSpecification masterSpec = masterSpecifications.stream()
                                    .filter(s ->
                                            s.getSpecificationCode()
                                                    .equals(specReq.getSpecificationCode()))
                                    .findFirst()
                                    .orElseThrow(() ->
                                            new AcemException(
                                                    ErrorCode.SPECIFICATION_NOT_FOUND,
                                                    "Specification not found: "
                                                            + specReq.getSpecificationCode()));

                            Double minValue = null;
                            Double maxValue = null;
                            Long unitId = null;
                            String unit = null;
                            if (masterSpec.getReferenceRange() != null) {
                                minValue = masterSpec.getReferenceRange().getMinValue();
                                maxValue = masterSpec.getReferenceRange().getMaxValue();
                                unitId = masterSpec.getReferenceRange().getUnitId();
                                unit = masterSpec.getReferenceRange().getUnit();
                            }
                            String persistedResult = SpecResultFormatValidator.normalizeForPersist(
                                    specReq.getAnalysedResult(), minValue, maxValue);

                            boolean outOfRange = SpecResultFormatValidator.isOutOfRange(
                                    persistedResult,
                                    minValue,
                                    maxValue
                            );

                            String specStatus = outOfRange
                                    ? "Failed"
                                    : "Passed";

                            lotSpecs.add(
                                    Lot.LotSpec.builder()
                                            .specificationCode(masterSpec.getSpecificationCode())
                                            .specificationName(masterSpec.getSpecificationName())
                                            .minValue(minValue)
                                            .maxValue(maxValue)
                                            .unitId(unitId)
                                            .unit(unit)
                                            .analysedResult(persistedResult)
                                            .isOutOfRange(outOfRange)
                                            .acemQcResult(SpecResultFormatValidator.normalizeForPersist(
                                                    specReq.getAcemQcResult(), minValue, maxValue))
                                            .status(specStatus)
                                            .build()
                            );
                        }
                    }

                    List<CertificateRequest> lotCerts = new ArrayList<>();

                    if (lotReq.getCertificates() != null) {

                        for (CertificateRequest certReq : lotReq.getCertificates()) {
                            try {
                                FileAttachments savedFile = filePromotionService.promoteAndBindFile(
                                        certReq.getFileId(),
                                        request.getSubDepartmentId(),
                                        sourcingId,
                                        FileBindingScope.forLot(lotReq.getLotId())
                                );

                                // Track successfully promoted file ID
                                promotedFileIds.add(savedFile.getFileId());

                                lotCerts.add(CertificateRequest.builder()
                                        .fileId(savedFile.getFileId())
                                        .fileName(savedFile.getOriginalFileName())
                                        .fileUrl(savedFile.getFilePath())
                                        .mimeType(savedFile.getMimeType())
                                        .certificateType(certReq.getCertificateType())
                                        .status(savedFile.getStatus())
                                        .readOnly(savedFile.isReadOnly())
                                        .build());

                            } catch (Exception e) {
                                throw new AcemException(
                                        ErrorCode.FILE_TRANSFER_ERROR,
                                        "File upload failed for cert fileId: " + certReq.getFileId()
                                );
                            }
                        }
                    }

                    Lot lot = Lot.builder()
                            .lotId(lotReq.getLotId())
                            .sourcingId(sourcingId)
                            .subDepartmentId(request.getSubDepartmentId())
                            .materialCode(material.getMaterialCode())
                            .materialName(material.getMaterialName())
                            .grade(null)
                            .supplyOrderNo(normalizeOptionalText(materialReq.getSupplyOrderNo()))
                            .receiptDate(receiptDate)
                            .manufacturerName(materialReq.getManufacturerName())
                            .status(status)
                            .submissionType(request.getSubmissionType())
                            .rawMaterialType(MaterialMasterDataService
                                    .resolveRawMaterialCategory(material.getRawMaterialType()))
                            .preparationType(material.getPreparationType())
                            .adductPreparation(lotReq.getAdductPreparation())
                            .htpbBlendingPreparation(lotReq.getHtpbBlendingPreparation())
                            .specifications(lotSpecs)
                            .certificates(lotCerts)
                            .createdByUserId(userId)
                            .createdByUsername(username)
                            .createdAt(LocalDateTime.now())
                            .build();

                    lotRepository.save(lot);
                }
            }

            return RmpFormBasicResponse.builder()
                    .sourcingId(sourcingId)
                    .status(status)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to create RMP form for sourcingId={}. Rolling back promoted files...", sourcingId, ex);

            // Rollback all promoted files in case of failure
            rollbackPromotedFiles(promotedFileIds);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to create RMP form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public UpdateLotResponse updateLot(UpdateLotRequest request, String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        String username = user.getUsername();

        Lot lot = lotRepository.findByLotId(request.getLotId())
                .orElseThrow(() -> new AcemException(ErrorCode.LOT_NOT_FOUND,
                        "Lot not found with ID: " + request.getLotId()));

        // 1. Prevent changes on deleted lots
        if (lot.getStatus() == Status.DELETED) {
            throw new AcemException(ErrorCode.LOT_NOT_EDITABLE,
                    "Cannot update a deleted lot. This lot has been soft-deleted and is no longer editable.");
        }

        // 2. Validate lot is editable
        if (lot.getStatus() == Status.WAITING_FOR_APPROVAL || lot.getStatus() == Status.APPROVED) {
            throw new AcemException(ErrorCode.LOT_NOT_EDITABLE,
                    "Lot is currently " + lot.getStatus() + " and cannot be edited.");
        }

        // 3. Validate submission type (DRAFT, UPDATE, or SUBMIT)
        Status submissionType = request.getSubmissionType();
        if (submissionType == null || !List.of(Status.DRAFT, Status.UPDATE, Status.SUBMIT).contains(submissionType)) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "submissionType must be DRAFT, UPDATE, or SUBMIT"
            );
        }

        // Aggregate tier + format + prep into one map before persist.
        Map<String, String> updateFieldErrors = new LinkedHashMap<>();
        collectUpdateLotForTierErrors(request, updateFieldErrors);

        MaterialMaster materialForValidation = null;
        List<MaterialSpecification> masterSpecsForValidation = null;
        if (!isBlank(request.getMaterialCode())) {
            Optional<MaterialMaster> materialOpt =
                    materialRepository.findFirstByMaterialCode(request.getMaterialCode());
            if (materialOpt.isEmpty()) {
                updateFieldErrors.putIfAbsent(
                        "materialCode",
                        "Material not found with code: " + request.getMaterialCode());
            } else {
                materialForValidation = materialOpt.get();
                if (!MaterialMasterDataService.isActive(materialForValidation)) {
                    updateFieldErrors.putIfAbsent(
                            "materialCode",
                            "Material is inactive: " + request.getMaterialCode());
                } else {
                    masterSpecsForValidation =
                            activeSpecifications(materialForValidation.getSpecifications());
                    if (masterSpecsForValidation == null || masterSpecsForValidation.isEmpty()) {
                        updateFieldErrors.putIfAbsent(
                                "materialCode",
                                "No specifications configured for material "
                                        + materialForValidation.getMaterialCode());
                    } else {
                        validateLotSpecificationFormats(
                                request.getSpecifications(),
                                masterSpecsForValidation,
                                submissionType,
                                "lot.specifications",
                                updateFieldErrors);
                        validatePreparationDetails(
                                materialForValidation,
                                request.getAdductPreparation(),
                                request.getHtpbBlendingPreparation(),
                                submissionType,
                                "lot",
                                updateFieldErrors);
                    }
                }
            }
        }
        throwValidationErrors(updateFieldErrors);

        List<String> promotedFileIds = new ArrayList<>();

        try {
            // 4. Resolve material & specifications (already validated above)
            MaterialMaster material = materialRepository.findFirstByMaterialCode(request.getMaterialCode())
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.MATERIAL_NOT_FOUND,
                            "Material not found with code: " + request.getMaterialCode()));
            if (!MaterialMasterDataService.isActive(material)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Material is inactive: " + request.getMaterialCode());
            }

            List<MaterialSpecification> masterSpecifications =
                    activeSpecifications(material.getSpecifications());

            if (masterSpecifications == null || masterSpecifications.isEmpty()) {
                throw new AcemException(
                        ErrorCode.SPECIFICATION_NOT_FOUND,
                        "No specifications configured for material " + material.getMaterialCode());
            }

            LocalDate receiptDate = request.getReceiptDate();

            // 6. Target Statuses before processing files
            boolean isSubmit = submissionType == Status.UPDATE || submissionType == Status.SUBMIT;
            Status targetStatus = isSubmit ? Status.WAITING_FOR_APPROVAL : lot.getStatus();
            Status targetSubmissionType = isSubmit ? Status.SUBMIT : Status.DRAFT;

            List<Lot.LotSpec> newSpecs = new ArrayList<>();
            if (request.getSpecifications() != null) {
                for (SpecificationRequest specReq : request.getSpecifications()) {
                    MaterialSpecification masterSpec = masterSpecifications.stream()
                            .filter(s -> s.getSpecificationCode().equals(specReq.getSpecificationCode()))
                            .findFirst()
                            .orElseThrow(() -> new AcemException(
                                    ErrorCode.SPECIFICATION_NOT_FOUND,
                                    "Specification code not found for material: " + specReq.getSpecificationCode()));

                    Double minValue = null;
                    Double maxValue = null;
                    Long unitId = null;
                    String unit = null;
                    if (masterSpec.getReferenceRange() != null) {
                        minValue = masterSpec.getReferenceRange().getMinValue();
                        maxValue = masterSpec.getReferenceRange().getMaxValue();
                        unitId = masterSpec.getReferenceRange().getUnitId();
                        unit = masterSpec.getReferenceRange().getUnit();
                    }
                    String persistedResult = SpecResultFormatValidator.normalizeForPersist(
                            specReq.getAnalysedResult(), minValue, maxValue);

                    boolean outOfRange = SpecResultFormatValidator.isOutOfRange(
                            persistedResult,
                            minValue,
                            maxValue);

                    newSpecs.add(Lot.LotSpec.builder()
                            .specificationCode(masterSpec.getSpecificationCode())
                            .specificationName(masterSpec.getSpecificationName())
                            .minValue(minValue)
                            .maxValue(maxValue)
                            .unitId(unitId)
                            .unit(unit)
                            .analysedResult(persistedResult)
                            .isOutOfRange(outOfRange)
                            .acemQcResult(SpecResultFormatValidator.normalizeForPersist(
                                    specReq.getAcemQcResult(), minValue, maxValue))
                            .status(outOfRange ? "Failed" : "Passed")
                            .build());
                }
            }

            // -----------------------------------------------------------------------------
            // 8. Process Certificate Updates (Compare Payload vs DB Files)
            // -----------------------------------------------------------------------------

            // Collect existing file IDs from Database
            Set<String> existingDbFileIds = (lot.getCertificates() != null)
                    ? lot.getCertificates().stream()
                    .map(CertificateRequest::getFileId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                    : Collections.emptySet();

            // Collect incoming file IDs from Request Payload
            Set<String> incomingFileIds = (request.getCertificates() != null)
                    ? request.getCertificates().stream()
                    .map(CertificateRequest::getFileId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                    : Collections.emptySet();

            // Identify files removed by user (present in DB, absent in Payload)
            List<String> removedFileIds = existingDbFileIds.stream()
                    .filter(fileId -> !incomingFileIds.contains(fileId))
                    .collect(Collectors.toList());

            // Process certificates list
            List<CertificateRequest> updatedCerts = new ArrayList<>();

            if (request.getCertificates() != null) {
                for (CertificateRequest certReq : request.getCertificates()) {
                    String fileId = certReq.getFileId();

                    // Scenario A: Existing File -> Keep as is (already promoted)
                    if (existingDbFileIds.contains(fileId)) {
                        CertificateRequest existingCert = lot.getCertificates().stream()
                                .filter(c -> fileId.equals(c.getFileId()))
                                .findFirst()
                                .orElse(null);

                        if (existingCert != null) {
                            // Build a fresh object retaining ALL previous file metadata from DB
                            DocumentDto enrichedCert = existingCert.toDocumentDto();
                            fileMetadataEnrichmentService.enrichDocument(enrichedCert, lot.getSubDepartmentId());
                            updatedCerts.add(CertificateRequest.fromDocumentDto(
                                    enrichedCert, certReq.getCertificateType()));
                        }
                    }
                    // Scenario B: New File -> Promote & Bind
                    else {
                        try {
                            FileAttachments savedFile = filePromotionService.promoteAndBindFile(
                                    fileId,
                                    lot.getSubDepartmentId(),
                                    lot.getSourcingId(),
                                    FileBindingScope.forLot(lot.getLotId())
                            );

                            promotedFileIds.add(savedFile.getFileId());

                            updatedCerts.add(CertificateRequest.builder()
                                    .fileId(savedFile.getFileId())
                                    .fileName(savedFile.getOriginalFileName())
                                    .fileUrl(savedFile.getFilePath())
                                    .certificateType(certReq.getCertificateType())
                                    .mimeType(savedFile.getMimeType())
                                    .status(savedFile.getStatus())
                                    .readOnly(savedFile.isReadOnly())
                                    .build());

                        } catch (Exception e) {
                            log.error("Failed to promote new fileId={}", fileId, e);
                            throw new AcemException(
                                    ErrorCode.FILE_TRANSFER_ERROR,
                                    "File upload failed for new attachment, please try again."
                            );
                        }
                    }
                }
            }

            // Purge removed files permanently from Disk & Mongo DB
            if (!removedFileIds.isEmpty()) {
                filePromotionService.purgeRemovedFilesFromDiskAndDb(lot.getSourcingId(), removedFileIds);
            }

            // 9. Update lot fields
            lot.setMaterialCode(material.getMaterialCode());
            lot.setMaterialName(material.getMaterialName());
            lot.setGrade(null);
            lot.setSupplyOrderNo(normalizeOptionalText(request.getSupplyOrderNo()));
            lot.setReceiptDate(receiptDate);
            lot.setManufacturerName(request.getManufacturerName());
            lot.setStatus(targetStatus);
            lot.setSubmissionType(targetSubmissionType);
            lot.setRawMaterialType(MaterialMasterDataService
                    .resolveRawMaterialCategory(material.getRawMaterialType()));
            lot.setPreparationType(material.getPreparationType());
            lot.setAdductPreparation(request.getAdductPreparation());
            lot.setHtpbBlendingPreparation(request.getHtpbBlendingPreparation());
            lot.setSpecifications(newSpecs);
            lot.setCertificates(updatedCerts); // Set updated certificates list
            lot.setUpdatedBy(PersonInfo.builder()
                    .id(userId)
                    .fullName(username)
                    .build());
            lot.setUpdatedAt(LocalDateTime.now());
            lotRepository.save(lot);

            // 10. Update sourcing status if submitting
            if (isSubmit) {
                Sourcing sourcing = sourcingRepository
                        .findBySourcingId(lot.getSourcingId())
                        .orElseThrow(() -> new AcemException(ErrorCode.LOT_NOT_FOUND,
                                "Sourcing entity not found for this lot"));

                sourcing.setStatus(Status.WAITING_FOR_APPROVAL);
                sourcing.setSubmissionType(Status.SUBMIT);
                sourcing.setUpdatedBy(userId);
                sourcing.setUpdatedAt(LocalDateTime.now());
                sourcingRepository.save(sourcing);
            }

            // 11. Build response
            return UpdateLotResponse.builder()
                    .formId(lot.getSourcingId())
                    .lotId(lot.getLotId())
                    .status(targetStatus)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to update Lot ID={}. Rolling back newly promoted files...", request.getLotId(), ex);

            // Rollback only newly promoted files in this execution round
            rollbackPromotedFiles(promotedFileIds);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to update Lot. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    // Helper method to roll back promoted files
    private void rollbackPromotedFiles(List<String> promotedFileIds) {
        if (promotedFileIds == null || promotedFileIds.isEmpty()) {
            return;
        }
        log.info("Starting rollback for {} promoted files...", promotedFileIds.size());
        for (String fileId : promotedFileIds) {
            if (fileId != null && !fileId.isBlank()) {
                try {
                    filePromotionService.rollbackOrUnbindFile(fileId);
                } catch (Exception e) {
                    log.error("Failed to execute file rollback for fileId={}", fileId, e);
                }
            }
        }
    }

    public LotDetailsResponse getLotDetails(LotDetailsRequest request) {

        // 1. Find the lot
        Lot lot = lotRepository.findByLotId(request.getLotId())
                .orElseThrow(() -> new AcemException(ErrorCode.LOT_NOT_FOUND,
                        "Lot not found with ID: " + request.getLotId()));

        if (lot.getStatus() == Status.DELETED) {
            throw new AcemException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Lot not found");
        }

        // 2. Fetch the material (for total specs count)
        MaterialMaster material = materialRepository
                .findFirstByMaterialCode(lot.getMaterialCode())
                .orElse(null);

        MaterialGrade gradeDto = null;

        if (lot.getGrade() != null) {
            gradeDto = MaterialGrade.builder()
                    .gradeId(lot.getGrade().getGradeId())
                    .gradeCode(lot.getGrade().getGradeCode())
                    .gradeName(lot.getGrade().getGradeName())
                    .build();
        }

        // Sourcing header is optional. Approved lots already carry subDepartmentId,
        // and QC revalidation must still list identification-sheet lots (including AP)
        // when the parent sourcing document is absent.
        Sourcing sourcing = lot.getSourcingId() == null || lot.getSourcingId().isBlank()
                ? null
                : sourcingRepository.findBySourcingId(lot.getSourcingId()).orElse(null);
        Long subDepartmentId = sourcing != null && sourcing.getSubDepartmentId() != null
                ? sourcing.getSubDepartmentId()
                : lot.getSubDepartmentId();

        List<Lot.LotSpec> lotSpecs = lot.getSpecifications() == null
                ? List.of()
                : lot.getSpecifications();

        // 4. Build specification responses (from embedded lot specs)
        List<SpecificationResponse> specResponses = lotSpecs.stream()
                .map(s -> SpecificationResponse.builder()
                        .specificationCode(s.getSpecificationCode())
                        .specificationName(s.getSpecificationName())
                        .referenceRange(ReferenceRange.builder()
                                .minValue(s.getMinValue())
                                .maxValue(s.getMaxValue())
                                .unitId(s.getUnitId())
                                .unit(s.getUnit())
                                .build())
                        .analysedResult(s.getAnalysedResult())
                        .isOutOfRange(s.isOutOfRange())
                        .acemQcResult(s.getAcemQcResult())
                        .status(s.getStatus())
                        .build())
                .collect(Collectors.toList());

        // 5. Build certificate responses
        List<CertificateResponse> certResponses = new ArrayList<>();
        if (lot.getCertificates() != null) {
            for (CertificateRequest cert : lot.getCertificates()) {
                DocumentDto enriched = cert.toDocumentDto();
                fileMetadataEnrichmentService.enrichDocument(enriched, subDepartmentId);
                certResponses.add(CertificateResponse.builder()
                        .fileId(enriched.getFileId())
                        .fileName(enriched.getFileName())
                        .fileUrl(enriched.getFileUrl())
                        .status(enriched.getStatus())
                        .mimeType(enriched.getMimeType())
                        .certificateType(cert.getCertificateType())
                        .readOnly(enriched.getReadOnly())
                        .build());
            }
        }

        // 6. Progress insights
        List<MaterialSpecification> configuredSpecifications =
                material != null && material.getSpecifications() != null
                ? material.getSpecifications()
                : Collections.emptyList();
        int totalSpecs = configuredSpecifications.size();
        int filledSpecs = lotSpecs.size();

        int completionPct = totalSpecs > 0
                ? (filledSpecs * 100 / totalSpecs)
                : 0;

        // Fetch all lots belonging to this sourcing. Fall back to the current lot
        // when the sourcing id is missing so a single AP lot can still be shown.
        List<Lot> sourcingLots = lot.getSourcingId() == null || lot.getSourcingId().isBlank()
                ? List.of(lot)
                : lotRepository.findBySourcingId(lot.getSourcingId());
        if (sourcingLots.isEmpty()) {
            sourcingLots = List.of(lot);
        }

        // All lots are considered entered only if every lot is submitted
        boolean allLotsEntered = sourcingLots.stream()
                .allMatch(l -> l.getSubmissionType() == Status.SUBMIT);

        // Current lot is ready for approval if it has been submitted
        // and is currently waiting for approval
        boolean isReadyForApproval =
                lot.getSubmissionType() == Status.SUBMIT
                        && lot.getStatus() == Status.WAITING_FOR_APPROVAL;

        RmpProgressInsightsDto progressInsights = RmpProgressInsightsDto.builder()
                .materialsCount(1)
                .totalSpecs(totalSpecs)
                .filledSpecs(filledSpecs)
                .completionPercentage(completionPct)
                .allLotsEntered(allLotsEntered)
                .isReadyForApproval(isReadyForApproval)
                .build();

        // 7. Quality insights
        long passedCount = lotSpecs.stream()
                .filter(s -> "Passed".equals(s.getStatus())).count();
        long failedCount = lotSpecs.stream()
                .filter(s -> "Failed".equals(s.getStatus())).count();
        int totalTests = lotSpecs.size();
        double passRate = totalTests > 0 ? ((double) passedCount / totalTests) * 100 : 0;

        List<String> nonConformances = lotSpecs.stream()
                .filter(s -> s.isOutOfRange())
                .map(s -> s.getSpecificationName())
                .collect(Collectors.toList());

        RmpQualityInsightsDto qualityInsights = RmpQualityInsightsDto.builder()
                .totalTests(totalTests)
                .passed((int) passedCount)
                .failed((int) failedCount)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .nonConformances(nonConformances)
                .build();

        // 8. Workflow insights
        RmpWorkflowInsightsDto workflowInsights = RmpWorkflowInsightsDto.builder()
                .currentStatus(String.valueOf(lot.getStatus()))
                .rejectionReason(null)   // you may add a rejectionReason field to Lot if needed
                .approvalPending(lot.getStatus() == Status.WAITING_FOR_APPROVAL)
                .reworkRequired(lot.getStatus() == Status.REJECTED)
                .resubmissionCount(0)
                .build();

        RawMaterialCategory rawMaterialType = lot.getRawMaterialType();
        PreparationType preparationType = lot.getPreparationType();
        if (rawMaterialType == null && material != null) {
            rawMaterialType = MaterialMasterDataService.resolveRawMaterialCategory(material.getRawMaterialType());
            preparationType = material.getPreparationType();
        }

        return LotDetailsResponse.builder()
                .lotId(lot.getLotId())
                .submissionType(lot.getSubmissionType())
                .subDepartmentId(subDepartmentId)
                .supplyOrderNo(lot.getSupplyOrderNo())
                .receiptDate(lot.getReceiptDate())
                .manufacturerName(lot.getManufacturerName())
                .materialCode(lot.getMaterialCode())
                .grade(gradeDto)
                .rawMaterialType(rawMaterialType)
                .preparationType(preparationType)
                .adductPreparation(lot.getAdductPreparation())
                .htpbBlendingPreparation(lot.getHtpbBlendingPreparation())
                .specifications(specResponses)
                .certificates(certResponses)
                .progressInsights(progressInsights)
                .qualityInsights(qualityInsights)
                .workflowInsights(workflowInsights)
                .status(lot.getStatus())
                .rejectionReason(lot.getRejectionReason())
                .build();
    }

    public LotListResponse getLotList(LotListRequest request) {

        Long subDeptId = request.getSubDepartmentId();

        // 1. Status counts – direct repository calls (identical to old JPA logic)
        StatusCountsDto statusCounts = StatusCountsDto.builder()
                .initiated(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.TO_BE_INITIATED))
                .inProgress(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.IN_PROGRESS))
                .waitingForApproval(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.WAITING_FOR_APPROVAL))
                .approved(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.APPROVED))
                .rejected(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.REJECTED))
                .build();

        // 2. Dynamic filter using MongoTemplate (unchanged from previous answer)
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("subDepartmentId").is(subDeptId));

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            criteriaList.add(Criteria.where("status").in(request.getStatus()));
        }
        if (request.getMaterialCode() != null && !request.getMaterialCode().isEmpty()) {
            criteriaList.add(Criteria.where("materialCode").in(request.getMaterialCode()));
        }
        if (request.getManufacturerName() != null && !request.getManufacturerName().isBlank()) {
            criteriaList.add(Criteria.where("manufacturerName").is(request.getManufacturerName()));
        }
        if (request.getSearch() != null && !request.getSearch().isBlank()) {
            String pattern = request.getSearch().trim();
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("lotId").regex(pattern, "i"),
                    Criteria.where("supplyOrderNo").regex(pattern, "i")
            ));
        }
        if (request.getFromDate() != null && !request.getFromDate().isBlank()) {
            LocalDate from = DateTimeConversionUtil.parseLocalDate(request.getFromDate());
            criteriaList.add(Criteria.where("receiptDate").gte(from));
        }
        if (request.getToDate() != null && !request.getToDate().isBlank()) {
            LocalDate to = DateTimeConversionUtil.parseLocalDate(request.getToDate());
            criteriaList.add(Criteria.where("receiptDate").lte(to));
        }
        // Exclude soft‑deleted lots
        criteriaList.add(
                Criteria.where("status").ne(Status.DELETED.name())
        );

        Criteria finalCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = Query.query(finalCriteria).with(Sort.by(Sort.Direction.DESC, "createdAt"));

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit());
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Lot.class);
        query.with(pageable);
        List<Lot> lots = mongoTemplate.find(query, Lot.class);
        Page<Lot> lotPage = PageableExecutionUtils.getPage(lots, pageable, () -> total);

        // 3. Map to DTOs
        List<LotSummary> lotSummaries = lotPage.getContent().stream()
                .map(lot -> {
                    MaterialGrade grade = lot.getGrade();

                    return LotSummary.builder()
                            .lotId(lot.getLotId())
                            .sourcingId(lot.getSourcingId())
                            .materialCode(lot.getMaterialCode())
                            .grade(grade)
                            .materialName(lot.getMaterialName())
                            .supplyOrderNo(lot.getSupplyOrderNo())
                            .receiptDate(lot.getReceiptDate())
                            .manufacturerName(lot.getManufacturerName())
                            .status(lot.getStatus())
                            .createdBy(PersonInfo.builder()
                                    .id(lot.getCreatedByUserId())
                                    .fullName(lot.getCreatedByUsername())
                                    .build())
                            .createdOn(lot.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return LotListResponse.builder()
                .statusCounts(statusCounts)
                .lots(lotSummaries)
                .pagination(PaginationDto.builder()
                        .page(request.getPage())
                        .pageSize(request.getLimit())
                        .totalRecords(lotPage.getTotalElements())
                        .totalPages(lotPage.getTotalPages())
                        .build())
                .build();
    }

    public DeleteLotResponse softDeleteLot(DeleteLotRequest request, String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        String username = user.getUsername();
        Lot lot = lotRepository.findByLotId(request.getLotId())
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND,
                        "The specified lotId '" + request.getLotId() + "' does not exist in the system."));

        if (lot.getStatus() == Status.DELETED) {
            throw new AcemException(ErrorCode.INVALID_STATE,
                    "Lot is already deleted");
        }
        if (lot.getStatus() != Status.IN_PROGRESS) {
            throw new AcemException(ErrorCode.INVALID_STATE,
                    "Only lots with an 'IN_PROGRESS' status can be deleted. This lot is currently '"
                            + lot.getStatus() + "' and cannot be removed.");
        }

        lot.setStatus(Status.DELETED);
        lot.setUpdatedBy(PersonInfo.builder().id(userId).fullName(username).build());
        lot.setUpdatedAt(LocalDateTime.now());
        lotRepository.save(lot);

        return DeleteLotResponse.builder()
                .lotId(lot.getLotId())
                .status(Status.DELETED)
                .build();
    }

    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverName) {

        Lot entity = lotRepository.findByLotIdAndSubDepartmentId(
                        request.getFormId(),
                        request.getSubDepartmentId()
                )
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "Lot not found: " + request.getFormId()
                ));

        ApprovalStatusUtil.applyStatusChange(
                request.getActionType(),
                request.getSubDepartmentId(),
                request.getRemarks(),
                request.getRejectionReason(),
                approverName,
                entity::setStatus,
                entity::setRemarks,
                entity::setRejectionReason,
                entity::setUpdatedBy,
                entity::setUpdatedAt
        );

        if (request.getActionType() == Status.APPROVED) {
            List<DocumentDto> certDocs = entity.getCertificates() == null
                    ? List.of()
                    : entity.getCertificates().stream()
                            .filter(Objects::nonNull)
                            .map(CertificateRequest::toDocumentDto)
                            .toList();
            fileReadOnlyService.markReadOnly(
                    request.getSubDepartmentId(),
                    FileIdExtractionUtil.extractFromDocuments(certDocs));
        }

        lotRepository.save(entity);
        return ApproverStatusResponse.builder()
                .formId(entity.getLotId())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .rejectionReason(entity.getRejectionReason())
                .actionBy(entity.getUpdatedBy())
                .actionAt(entity.getUpdatedAt())
                .build();
    }

    public LotStatsResponse getLotStats(Long subDepartmentId) {
        long createdLots = lotRepository.countBySubDepartmentIdAndStatusNot(subDepartmentId, Status.DELETED);
        long pendingLots = lotRepository.countBySubDepartmentIdAndStatusIn(subDepartmentId, List.of(
                Status.TO_BE_INITIATED, Status.IN_PROGRESS));
        long waitingForApprovalLots = lotRepository.countBySubDepartmentIdAndStatus(subDepartmentId,
                Status.WAITING_FOR_APPROVAL);
        long approvedLots = lotRepository.countBySubDepartmentIdAndStatus(subDepartmentId, Status.APPROVED);
        long rejectedLots = lotRepository.countBySubDepartmentIdAndStatus(subDepartmentId, Status.REJECTED);
        return LotStatsResponse.builder()
                .createdLots(createdLots)
                .pendingLots(pendingLots)
                .waitingForApprovalLots(waitingForApprovalLots)
                .approvedLots(approvedLots)
                .rejectedLots(rejectedLots)
                .build();
    }
//    -----------------------HELPERS------------------------------

    private boolean isSubmitIntent(Status submissionType) {
        return submissionType == Status.SUBMIT || submissionType == Status.UPDATE;
    }

    private void validateMaterialForTier(MaterialRequest material, Status submissionType) {
        Map<String, String> errors = new LinkedHashMap<>();
        collectMaterialForTierErrors(material, submissionType, "", errors);
        throwValidationErrors(errors);
    }

    private void collectMaterialForTierErrors(
            MaterialRequest material,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        String p = prefix == null || prefix.isBlank() ? "" : prefix + ".";
        if (material == null) {
            errors.put(prefix.isBlank() ? "materials" : prefix, "material is required");
            return;
        }

        SpecResultFormatValidator.validateAlphaNumField(
                material.getManufacturerName(), p + "manufacturerName", true, errors);
        SpecResultFormatValidator.validateAlphaNumField(
                material.getSupplyOrderNo(), p + "supplyOrderNo", false, errors);
        if (material.getLots() == null || material.getLots().isEmpty()) {
            errors.put(p + "lots", "lots list cannot be empty");
        } else {
            for (int i = 0; i < material.getLots().size(); i++) {
                LotRequest lot = material.getLots().get(i);
                validateLotForTier(lot, submissionType, p + "lots[" + i + "]", errors);
            }
        }
    }

    private void collectUpdateLotForTierErrors(UpdateLotRequest request, Map<String, String> errors) {
        SpecResultFormatValidator.validateAlphaNumField(
                request.getManufacturerName(), "manufacturerName", true, errors);
        SpecResultFormatValidator.validateAlphaNumField(
                request.getSupplyOrderNo(), "supplyOrderNo", false, errors);
        LotRequest lotShape = LotRequest.builder()
                .lotId(request.getLotId())
                .specifications(request.getSpecifications())
                .certificates(request.getCertificates())
                .build();
        validateLotForTier(lotShape, request.getSubmissionType(), "lot", errors);
    }

    private void validatePreparationDetails(
            MaterialMaster material,
            AdductPreparationDetails adductPreparation,
            HtpbBlendingPreparationDetails htpbBlendingPreparation,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        RawMaterialCategory category = MaterialMasterDataService.resolveRawMaterialCategory(
                material.getRawMaterialType());

        if (category == RawMaterialCategory.NORMAL) {
            rejectPreparationFieldIfPresent(
                    adductPreparation, "adductPreparation", prefix, errors,
                    "adductPreparation is not allowed for normal raw materials");
            rejectPreparationFieldIfPresent(
                    htpbBlendingPreparation, "htpbBlendingPreparation", prefix, errors,
                    "htpbBlendingPreparation is not allowed for normal raw materials");
            return;
        }

        PreparationType preparationType = material.getPreparationType();
        if (preparationType == PreparationType.ADDUCT) {
            rejectUnsupportedPreparationFields(
                    preparationType,
                    prefix,
                    errors,
                    adductPreparation,
                    htpbBlendingPreparation);
            validateAdductPreparation(
                    material, adductPreparation, submissionType, prefix, errors);
        } else if (preparationType == PreparationType.HTPB_BLENDING) {
            rejectUnsupportedPreparationFields(
                    preparationType,
                    prefix,
                    errors,
                    adductPreparation,
                    htpbBlendingPreparation);
            validateBlendingStylePreparation(
                    htpbBlendingPreparation,
                    "htpbBlendingPreparation",
                    submissionType,
                    prefix,
                    errors);
        } else {
            rejectUnsupportedPreparationFields(
                    preparationType,
                    prefix,
                    errors,
                    adductPreparation,
                    htpbBlendingPreparation);
            if (isSubmitIntent(submissionType)) {
                errors.put(prefix + ".preparationDetails",
                        "Preparation details are not yet supported for preparation type " + preparationType);
            }
        }
    }

    private void rejectPreparationFieldIfPresent(
            Object value,
            String fieldName,
            String prefix,
            Map<String, String> errors,
            String message) {
        if (value != null) {
            errors.put(prefix + "." + fieldName, message);
        }
    }

    private void rejectUnsupportedPreparationFields(
            PreparationType preparationType,
            String prefix,
            Map<String, String> errors,
            AdductPreparationDetails adductPreparation,
            HtpbBlendingPreparationDetails htpbBlendingPreparation) {
        if (preparationType != PreparationType.ADDUCT && adductPreparation != null) {
            errors.put(prefix + ".adductPreparation",
                    "adductPreparation is not supported for preparation type " + preparationType);
        }
        if (preparationType != PreparationType.HTPB_BLENDING && htpbBlendingPreparation != null) {
            errors.put(prefix + ".htpbBlendingPreparation",
                    "htpbBlendingPreparation is not supported for preparation type " + preparationType);
        }
    }

    private void validateAdductPreparation(
            MaterialMaster material,
            AdductPreparationDetails adductPreparation,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        if (!isSubmitIntent(submissionType)) {
            return;
        }
        if (adductPreparation == null) {
            errors.put(prefix + ".adductPreparation", "adductPreparation is required");
            return;
        }
        validateAdductPreparationFields(adductPreparation, prefix + ".adductPreparation", errors);
    }

    private void validateBlendingStylePreparation(
            BlendingStylePreparationDetails preparationDetails,
            String fieldName,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        if (!isSubmitIntent(submissionType)) {
            return;
        }
        if (preparationDetails == null) {
            errors.put(prefix + "." + fieldName, fieldName + " is required");
            return;
        }
        validateBlendingStylePreparationFields(
                preparationDetails, prefix + "." + fieldName, errors);
    }

    private void validateAdductPreparationFields(
            AdductPreparationDetails details,
            String prefix,
            Map<String, String> errors) {
        if (isBlank(details.getAdductBatchPrepDate())) {
            errors.put(prefix + ".adductBatchPrepDate", "adductBatchPrepDate is required");
        }
        SpecResultFormatValidator.validateAlphaNumField(
                details.getTmpMfgLotNo(), prefix + ".tmpMfgLotNo", true, errors);
        if (details.getTmpTotalQtyGm() == null) {
            errors.put(prefix + ".tmpTotalQtyGm", "tmpTotalQtyGm is required");
        }
        SpecResultFormatValidator.validateAlphaNumField(
                details.getNbdMfgLotNo(), prefix + ".nbdMfgLotNo", true, errors);
        if (details.getNbdTotalQtyGm() == null) {
            errors.put(prefix + ".nbdTotalQtyGm", "nbdTotalQtyGm is required");
        }
        if (details.getRpm() == null) {
            errors.put(prefix + ".rpm", "rpm is required");
        }
        if (details.getProcessTemp() == null) {
            errors.put(prefix + ".processTemp", "processTemp is required");
        }
        if (details.getJacketTemp() == null) {
            errors.put(prefix + ".jacketTemp", "jacketTemp is required");
        }
        if (isBlank(details.getProcessStartTime())) {
            errors.put(prefix + ".processStartTime", "processStartTime is required");
        }
        if (isBlank(details.getProcessEndTime())) {
            errors.put(prefix + ".processEndTime", "processEndTime is required");
        }
        if (details.getFinalAdductQty() == null) {
            errors.put(prefix + ".finalAdductQty", "finalAdductQty is required");
        }
        if (isBlank(details.getDispatchDateTime())) {
            errors.put(prefix + ".dispatchDateTime", "dispatchDateTime is required");
        }
    }

    private void validateBlendingStylePreparationFields(
            BlendingStylePreparationDetails details,
            String prefix,
            Map<String, String> errors) {
        SpecResultFormatValidator.validateAlphaNumField(
                details.getMfgBatchLotNo(), prefix + ".mfgBatchLotNo", true, errors);
        if (details.getTotalQty() == null) {
            errors.put(prefix + ".totalQty", "totalQty is required");
        }
        SpecResultFormatValidator.validateAlphaNumField(
                details.getEquipmentId(), prefix + ".equipmentId", true, errors);
        if (details.getAgitatorRpm() == null) {
            errors.put(prefix + ".agitatorRpm", "agitatorRpm is required");
        }
        if (details.getProcessTemp() == null) {
            errors.put(prefix + ".processTemp", "processTemp is required");
        }
        if (details.getJacketTemp() == null) {
            errors.put(prefix + ".jacketTemp", "jacketTemp is required");
        }
        if (isBlank(details.getProcessStartTime())) {
            errors.put(prefix + ".processStartTime", "processStartTime is required");
        }
        if (isBlank(details.getProcessEndTime())) {
            errors.put(prefix + ".processEndTime", "processEndTime is required");
        }
    }

    private void validateLotForTier(
            LotRequest lot,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        if (lot == null) {
            errors.put(prefix, "lot is required");
            return;
        }

        SpecResultFormatValidator.validateAlphaNumField(
                lot.getLotId(), prefix + ".lotId", true, errors);

        if (!isSubmitIntent(submissionType)) {
            return;
        }

        List<SpecificationRequest> specifications = lot.getSpecifications();
        if (specifications == null || specifications.isEmpty()) {
            errors.put(prefix + ".specifications", "specifications are required");
        }
        // analysedResult required + type: validateLotSpecificationFormats (single source)

        List<CertificateRequest> certificates = lot.getCertificates();
        if (certificates == null || certificates.isEmpty()) {
            errors.put(prefix + ".certificates", "certificates are required");
        } else {
            for (int i = 0; i < certificates.size(); i++) {
                CertificateRequest cert = certificates.get(i);
                if (cert == null || isBlank(cert.getFileId())) {
                    errors.put(prefix + ".certificates[" + i + "].fileId", "fileId is required");
                }
                SpecResultFormatValidator.validateAlphaNumField(
                        cert == null ? null : cert.getCertificateType(),
                        prefix + ".certificates[" + i + "].certificateType",
                        true,
                        errors);
            }
        }
    }

    private String normalizeOptionalText(String value) {
        return SpecResultFormatValidator.normalizeOptionalText(value);
    }

    private boolean isBlank(String value) {
        return SpecResultFormatValidator.isBlank(value);
    }

    private void throwValidationErrors(Map<String, String> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        throw new AcemException(ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
    }

    private void validateSubmissionType(Status submissionType) {
        if (submissionType == null ||
                (submissionType != Status.DRAFT
                        && submissionType != Status.SUBMIT)) {

            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "submissionType must be DRAFT or SUBMIT"
            );
        }
    }

    private void validateLotSpecificationFormats(
            List<SpecificationRequest> specifications,
            List<MaterialSpecification> masterSpecifications,
            Status submissionType,
            String specificationsPrefix,
            Map<String, String> errors) {
        if (specifications == null || specifications.isEmpty()) {
            return;
        }

        boolean required = isSubmitIntent(submissionType);
        for (int i = 0; i < specifications.size(); i++) {
            SpecificationRequest specReq = specifications.get(i);
            if (specReq == null) {
                continue;
            }

            String fieldPath = specificationsPrefix + "[" + i + "]";
            MaterialSpecification masterSpec = masterSpecifications.stream()
                    .filter(spec -> spec.getSpecificationCode().equals(specReq.getSpecificationCode()))
                    .findFirst()
                    .orElse(null);
            if (masterSpec == null) {
                errors.put(fieldPath + ".specificationCode", "specificationCode is invalid");
                continue;
            }

            SpecResultFormatValidator.validateSpecResultFormat(
                    specReq.getAnalysedResult(), masterSpec, fieldPath, "analysedResult", required, errors);
            SpecResultFormatValidator.validateSpecResultFormat(
                    specReq.getAcemQcResult(), masterSpec, fieldPath, "acemQcResult", false, errors);
        }
    }

    private static List<MaterialSpecification> activeSpecifications(List<MaterialSpecification> specifications) {
        if (specifications == null || specifications.isEmpty()) {
            return Collections.emptyList();
        }
        return specifications.stream()
                .filter(MaterialMasterDataService::isSpecificationActive)
                .toList();
    }
}
