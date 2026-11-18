package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.general.*;
import in.co.bel.acempp.dto.rawmaterialsourcing.*;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.entity.sourcing.Sourcing;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.MaterialRepository;
import in.co.bel.acempp.repository.sourcing.LotRepository;
import in.co.bel.acempp.repository.sourcing.SourcingRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.service.FormIdGeneratorService;
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawMaterialSourcingService {

    private final SourcingRepository sourcingRepository;
    private final UserRepository userRepository;
    private final LotRepository lotRepository;
    private final MaterialRepository materialRepository;
    private final FormIdGeneratorService formIdGenerator;
    private final RawMaterialSourcingValidationSupport validationSupport;
    private final RawMaterialSourcingCertificateSupport certificateSupport;
    private final RawMaterialSourcingQuerySupport querySupport;
    private final RawMaterialSourcingDetailsMapper detailsMapper;
    private final RawMaterialSourcingLotMapper lotMapper;

    @Transactional(rollbackFor = Exception.class)
    public RmpFormBasicResponse createForm(CreateRmpFormRequest request, String userId) {
        validationSupport.validateSubmissionType(request.getSubmissionType());

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
            validationSupport.collectMaterialForTierErrors(
                    materialReq, request.getSubmissionType(), materialPrefix, createFieldErrors);

            if (materialReq == null || validationSupport.isBlank(materialReq.getMaterialCode())) {
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
                    RawMaterialSourcingValidationSupport.activeSpecifications(material.getSpecifications());
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
                if (lotReq != null && !validationSupport.isBlank(lotReq.getLotId())
                        && lotRepository.existsByLotId(lotReq.getLotId())) {
                    createFieldErrors.put(
                            lotPrefix + ".lotId",
                            "Lot ID already exists: " + lotReq.getLotId());
                }
                if (lotReq == null) {
                    continue;
                }
                validationSupport.validateLotSpecificationFormats(
                        lotReq.getSpecifications(),
                        masterSpecifications,
                        request.getSubmissionType(),
                        lotPrefix + ".specifications",
                        createFieldErrors);
                validationSupport.validatePreparationDetails(
                        material,
                        lotReq.getAdductPreparation(),
                        lotReq.getHtpbBlendingPreparation(),
                        request.getSubmissionType(),
                        lotPrefix,
                        createFieldErrors);
            }
        }
        validationSupport.throwValidationErrors(createFieldErrors);

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
                        RawMaterialSourcingValidationSupport.activeSpecifications(material.getSpecifications());
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

                    List<Lot.LotSpec> lotSpecs = lotMapper.buildLotSpecs(
                            lotReq.getSpecifications(), masterSpecifications, false);

                    List<CertificateRequest> lotCerts = certificateSupport.promoteCertificatesForCreate(
                            lotReq.getCertificates(),
                            request.getSubDepartmentId(),
                            sourcingId,
                            lotReq.getLotId(),
                            promotedFileIds);

                    Lot lot = Lot.builder()
                            .lotId(lotReq.getLotId())
                            .sourcingId(sourcingId)
                            .subDepartmentId(request.getSubDepartmentId())
                            .materialCode(material.getMaterialCode())
                            .materialName(material.getMaterialName())
                            .grade(null)
                            .supplyOrderNo(lotMapper.normalizeOptionalText(materialReq.getSupplyOrderNo()))
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
            certificateSupport.rollbackPromotedFiles(promotedFileIds);

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
        validationSupport.collectUpdateLotForTierErrors(request, updateFieldErrors);

        MaterialMaster materialForValidation = null;
        List<MaterialSpecification> masterSpecsForValidation = null;
        if (!validationSupport.isBlank(request.getMaterialCode())) {
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
                            RawMaterialSourcingValidationSupport.activeSpecifications(
                                    materialForValidation.getSpecifications());
                    if (masterSpecsForValidation == null || masterSpecsForValidation.isEmpty()) {
                        updateFieldErrors.putIfAbsent(
                                "materialCode",
                                "No specifications configured for material "
                                        + materialForValidation.getMaterialCode());
                    } else {
                        validationSupport.validateLotSpecificationFormats(
                                request.getSpecifications(),
                                masterSpecsForValidation,
                                submissionType,
                                "lot.specifications",
                                updateFieldErrors);
                        validationSupport.validatePreparationDetails(
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
        validationSupport.throwValidationErrors(updateFieldErrors);

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
                    RawMaterialSourcingValidationSupport.activeSpecifications(material.getSpecifications());

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

            List<Lot.LotSpec> newSpecs = lotMapper.buildLotSpecs(
                    request.getSpecifications(), masterSpecifications, true);

            // -----------------------------------------------------------------------------
            // 8. Process Certificate Updates (Compare Payload vs DB Files)
            // -----------------------------------------------------------------------------

            List<CertificateRequest> updatedCerts = certificateSupport.processCertificateUpdates(
                    lot, request.getCertificates(), promotedFileIds);

            // 9. Update lot fields
            lot.setMaterialCode(material.getMaterialCode());
            lot.setMaterialName(material.getMaterialName());
            lot.setGrade(null);
            lot.setSupplyOrderNo(lotMapper.normalizeOptionalText(request.getSupplyOrderNo()));
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
            certificateSupport.rollbackPromotedFiles(promotedFileIds);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to update Lot. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public LotDetailsResponse getLotDetails(LotDetailsRequest request) {
        return detailsMapper.toLotDetails(request);
    }

    public LotListResponse getLotList(LotListRequest request) {
        return querySupport.getLotList(request);
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
            certificateSupport.markCertificatesReadOnly(entity, request.getSubDepartmentId());
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
        return querySupport.getLotStats(subDepartmentId);
    }
}