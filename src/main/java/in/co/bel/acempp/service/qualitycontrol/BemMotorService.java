package in.co.bel.acempp.service.qualitycontrol;


import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.general.StatusCountsDto;
import in.co.bel.acempp.dto.statictestingfacility.bem.BemMotorListItemDto;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.statictestingfacility.bem.*;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.qualitycontrol.BemMotor;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.FileSystemRepository;
import in.co.bel.acempp.repository.qualitycontrol.BemMotorRepository;
import in.co.bel.acempp.repository.qualitycontrol.StaticTestingRepository;
import in.co.bel.acempp.service.FileBindingService;
import in.co.bel.acempp.service.FileMetadataEnrichmentService;
import in.co.bel.acempp.service.FileReadOnlyService;
import in.co.bel.acempp.service.FormIdGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class BemMotorService {

    private final BemMotorRepository bemMotorRepository;
    private final MongoTemplate mongoTemplate;
    private final StaticTestingRepository stfRepo;
    private final FileBindingService fileBindingService;
    private final FormIdGeneratorService formIdGenerator;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final StaticTestingService stfService;

    public BemMotorActionResponseDto createBemMotor(BemMotorRequest request, String userId) {
        if (request.getSubType() != null && request.getSubType() != StfMotorSubType.BEM) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Other BEM subType must be BEM");
        }
        if (bemMotorRepository.existsByMotorIdAndIsDeletedFalse(request.getMotorId())) {
            throw new AcemException(ErrorCode.DUPLICATE_ISSUANCE, "BEM Motor ID already exists: " + request.getMotorId());
        }

        if (request.getStfTestNo() == null || request.getStfTestNo().trim().isEmpty()) {
            throw new AcemException(ErrorCode.MISSING_MANDATORY_FIELD, "stfTestNo is required");
        }

        if (request.getSubDepartmentId() == null) {
            throw new AcemException(ErrorCode.MISSING_MANDATORY_FIELD, "subDepartmentId is required");
        }

        String stfTestNo = request.getStfTestNo().trim();

        if (bemMotorRepository.existsByStfTestNoAndIsDeletedFalse(stfTestNo)) {
            throw new AcemException(
                    ErrorCode.MOTOR_ALREADY_EXISTS,
                    "stfTestNo '" + stfTestNo + "' already exists in BEM Motors");
        }

        if (stfRepo.existsByMotorsStfTestNoAndIsDeletedFalse(stfTestNo)) {
            throw new AcemException(
                    ErrorCode.MOTOR_ALREADY_EXISTS,
                    "stfTestNo '" + stfTestNo + "' already exists in Static Testing forms");
        }

        if (request.getFormSubmissionType() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "formSubmissionType is required"
            );
        }

        if (request.getFormSubmissionType() != Status.DRAFT
                && request.getFormSubmissionType() != Status.SUBMIT) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "formSubmissionType must be DRAFT or SUBMIT"
            );
        }

        boolean isDraft = request.getFormSubmissionType() == Status.DRAFT;
        Status status = isDraft ? Status.IN_PROGRESS : Status.WAITING_FOR_APPROVAL;

        String formId = formIdGenerator.generate("bem_motors", "formId", "STF_B", 3);
        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            promotedDocs = promoteBemMotorFiles(
                    request.getStaticTestingDetails(),
                    request.getSubDepartmentId(),
                    formId,
                    Collections.emptySet(),
                    request.getMotorId(),
                    stfTestNo
            );

            BemMotor motor = BemMotor.builder()
                    .formId(formId)
                    .motorId(request.getMotorId())
                    .stfTestNo(stfTestNo)
                    .subDepartmentId(request.getSubDepartmentId())
                    .subType(StfMotorSubType.BEM)
                    .status(status)
                    .formSubmissionType(request.getFormSubmissionType())
                    .staticTestingDetails(request.getStaticTestingDetails())
                    .isDeleted(false)
                    .createdBy(stfService.person(stfService.getUser(userId)))
                    .createdAt(LocalDateTime.now())
                    .updatedBy(stfService.person(stfService.getUser(userId)))
                    .updatedAt(LocalDateTime.now())
                    .build();

            motor = bemMotorRepository.save(motor);
            log.info("BEM Motor created with ID: {} and formId: {} by user: {}", motor.getMotorId(), formId, userId);

            return BemMotorActionResponseDto.builder()
                    .id(motor.getId())
                    .motorId(motor.getMotorId())
                    .stfTestNo(motor.getStfTestNo())
                    .subType(motor.getSubType())
                    .formSubmissionType(motor.getFormSubmissionType())
                    .status(motor.getStatus())
                    .actionBy(motor.getCreatedBy())
                    .actionAt(motor.getCreatedAt())
                    .build();

        } catch (Exception ex) {
            log.error("BEM Motor creation failed for motorId={}. Initiating file rollback...", request.getMotorId(), ex);
            rollbackPromotedFiles(promotedDocs);
            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to create BEM Motor. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public BemMotorActionResponseDto updateBemMotor(BemMotorRequest request, String userId) {
        BemMotor motor = bemMotorRepository.findByMotorIdAndIsDeletedFalse(request.getMotorId())
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "BEM Motor not found: " + request.getMotorId()));

        if (request.getStfTestNo() != null
                && !request.getStfTestNo().trim().equals(motor.getStfTestNo())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "stfTestNo cannot be modified once created for motor: " + motor.getMotorId());
        }

        if (request.getFormSubmissionType() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "formSubmissionType is required");
        }

        if (request.getFormSubmissionType() != Status.DRAFT
                && request.getFormSubmissionType() != Status.SUBMIT) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "formSubmissionType must be DRAFT or SUBMIT");
        }

        String formId = motor.getFormId();
        Long subDepartmentId = request.getSubDepartmentId() != null ? request.getSubDepartmentId() : motor.getSubDepartmentId();

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            Set<String> existingDbFileIds = extractBemFileIds(motor.getStaticTestingDetails());
            Set<String> incomingFileIds = extractBemFileIds(request.getStaticTestingDetails());

            List<String> removedFileIds = existingDbFileIds.stream()
                    .filter(fileId -> !incomingFileIds.contains(fileId))
                    .collect(Collectors.toList());

            promotedDocs = promoteBemMotorFiles(
                    request.getStaticTestingDetails(),
                    subDepartmentId,
                    formId,
                    existingDbFileIds,
                    motor.getMotorId(),
                    motor.getStfTestNo()
            );

            if (!removedFileIds.isEmpty()) {
                fileBindingService.purgeRemovedFilesFromDiskAndDb(formId, removedFileIds);
            }

            if (request.getStaticTestingDetails() != null) {
                motor.setStaticTestingDetails(request.getStaticTestingDetails());
            }

            boolean isDraft = request.getFormSubmissionType() == Status.DRAFT;
            Status status = isDraft ? Status.IN_PROGRESS : Status.WAITING_FOR_APPROVAL;

            motor.setSubDepartmentId(subDepartmentId);
            motor.setFormSubmissionType(request.getFormSubmissionType());
            motor.setStatus(status);
            motor.setRejectionReason(null);
            motor.setRemarks(null);
            motor.setUpdatedBy(stfService.person(stfService.getUser(userId)));
            motor.setUpdatedAt(LocalDateTime.now());

            bemMotorRepository.save(motor);

            return BemMotorActionResponseDto.builder()
                    .id(motor.getId())
                    .motorId(motor.getMotorId())
                    .stfTestNo(motor.getStfTestNo())
                    .subType(motor.getSubType())
                    .status(motor.getStatus())
                    .formSubmissionType(motor.getFormSubmissionType())
                    .actionBy(stfService.person(stfService.getUser(userId)))
                    .actionAt(motor.getUpdatedAt())
                    .build();

        } catch (Exception ex) {
            log.error("BEM Motor update failed for motorId={}, formId={}. Initiating file rollback...", request.getMotorId(), formId, ex);
            rollbackPromotedFiles(promotedDocs);
            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to update BEM Motor. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public BemMotorDetailsResponseDto getBemMotorDetails(String motorId) {
        BemMotor motor = bemMotorRepository.findByMotorIdAndIsDeletedFalse(motorId)
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "BEM Motor not found: " + motorId));

        enrichBemMotorFiles(motor.getStaticTestingDetails(), motor.getSubDepartmentId());

        return BemMotorDetailsResponseDto.builder()
                .formId(motor.getFormId())
                .motorId(motor.getMotorId())
                .stfTestNo(motor.getStfTestNo())
                .bemNo(motor.getMotorId())
                .subType(motor.getSubType())
                .status(motor.getStatus())
                .rejectionReason(motor.getRejectionReason())
                .staticTestingDetails(motor.getStaticTestingDetails())
                .createdBy(motor.getCreatedBy())
                .createdAt(motor.getCreatedAt())
                .updatedBy(motor.getUpdatedBy())
                .updatedAt(motor.getUpdatedAt())
                .build();
    }

    public BemMotorListResponseDto getBemMotorList(BemMotorRequest request) {

        int pageNo = request.getPage() != null && request.getPage() > 0
                ? request.getPage() - 1
                : 0;

        int pageSize = request.getLimit() != null && request.getLimit() > 0
                ? request.getLimit()
                : 10;

        Query query = new Query();

        query.addCriteria(Criteria.where("isDeleted").is(false));

        if (request.getCleanStatusFilter() != null) {
            query.addCriteria(
                    Criteria.where("status")
                            .in(request.getCleanStatusFilter())
            );
        }
        if (request.getCleanSearch() != null) {
            String searchTerm = Pattern.quote(request.getCleanSearch());

            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("motorId").regex(searchTerm, "i"),
                    Criteria.where("formId").regex(searchTerm, "i"),
                    Criteria.where("stfTestNo").regex(searchTerm, "i")
            ));
        }
        long totalRecords = mongoTemplate.count(query, BemMotor.class);

        query.with(PageRequest.of(
                pageNo,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        ));

        List<BemMotor> motors = mongoTemplate.find(query, BemMotor.class);

        List<BemMotorListItemDto> items = motors.stream()
                .map(m -> {
                    return BemMotorListItemDto.builder()
                            .formId(m.getFormId())
                            .motorId(m.getMotorId())
                            .stfTestNo(m.getStfTestNo())
                            .subType(m.getSubType())
                            .status(m.getStatus())
                            .createdBy(m.getCreatedBy())
                            .createdAt(m.getCreatedAt())
                            .build();
                })
                .toList();
        StatusCountsDto statusCounts = getBemMotorStatusCounts();


        return BemMotorListResponseDto.builder()
                .motors(items)
                .statusCounts(statusCounts)
                .pagination(PaginationDto.builder()
                        .page(pageNo + 1)
                        .pageSize(pageSize)
                        .totalRecords(totalRecords)
                        .totalPages((int) Math.ceil((double) totalRecords / pageSize))
                        .build())
                .build();
    }

    private StatusCountsDto getBemMotorStatusCounts() {

        long inProgress = mongoTemplate.count(
                Query.query(
                        Criteria.where("isDeleted").is(false)
                                .and("status").is(Status.IN_PROGRESS)
                ),
                BemMotor.class
        );

        long waitingForApproval = mongoTemplate.count(
                Query.query(
                        Criteria.where("isDeleted").is(false)
                                .and("status").is(Status.WAITING_FOR_APPROVAL)
                ),
                BemMotor.class
        );

        long approved = mongoTemplate.count(
                Query.query(
                        Criteria.where("isDeleted").is(false)
                                .and("status").is(Status.APPROVED)
                ),
                BemMotor.class
        );

        long rejected = mongoTemplate.count(
                Query.query(
                        Criteria.where("isDeleted").is(false)
                                .and("status").is(Status.REJECTED)
                ),
                BemMotor.class
        );

        return StatusCountsDto.builder()
                .inProgress(inProgress)
                .waitingForApproval(waitingForApproval)
                .approved(approved)
                .rejected(rejected)
                .build();
    }

    public ApproverStatusResponse changeStatus(
            ApproverBemStatusRequest request,
            String userId) {

        BemMotor motor = bemMotorRepository
                .findByMotorIdAndIsDeletedFalse(request.getMotorId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "BEM Motor not found: " + request.getMotorId()
                ));

        // Motor must be waiting for approval
        if (motor.getStatus() != Status.WAITING_FOR_APPROVAL) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "BEM Motor is not waiting for approval: "
                            + motor.getMotorId()
            );
        }

        // Validate action type
        if (request.getActionType() != Status.APPROVED
                && request.getActionType() != Status.REJECTED) {

            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Action type must be APPROVED or REJECTED"
            );
        }

        // Update status
        motor.setStatus(request.getActionType());

        // Update approval/rejection details
        motor.setRemarks(request.getRemarks());
        if (request.getActionType() == Status.REJECTED) {
            motor.setRejectionReason(request.getRejectionReason());
        } else {
            motor.setRejectionReason(null);
        }

        // Update audit details
        motor.setUpdatedBy(stfService.person(stfService.getUser(userId)));
        motor.setUpdatedAt(LocalDateTime.now());

        bemMotorRepository.save(motor);
        if (request.getActionType() == Status.APPROVED) {
            fileReadOnlyService.markReadOnly(
                    motor.getSubDepartmentId(),
                    extractBemFileIds(motor.getStaticTestingDetails()));
        }

        return ApproverStatusResponse.builder()
                .formId(motor.getMotorId())
                .status(motor.getStatus())
                .remarks(motor.getRemarks())
                .rejectionReason(motor.getRejectionReason())
                .actionBy(motor.getUpdatedBy())
                .actionAt(motor.getUpdatedAt())
                .build();
    }

    private List<DocumentDto> promoteBemMotorFiles(
            StfPayloads.StaticTestingDetails details,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds,
            String motorId,
            String stfTestNo) {

        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        if (details == null) {
            return newlyPromotedDocs;
        }

        // BEM sheet no longer carries ptCurveFile attachments.
        return newlyPromotedDocs;
    }

    /**
     * Promotes a single file attachment via FileBindingService and updates the DTO reference.
     * If the file is already bound to this form, its metadata is restored from the DB.
     */
    private void promoteSingleFile(
            DocumentDto report,
            Long deptId,
            String formId,
            Set<String> existingFileIds,
            List<DocumentDto> newlyPromotedList,
            String motorId,
            String stfTestNo) {

        if (report == null || report.getFileId() == null || report.getFileId().isBlank()) {
            return;
        }

        String fileId = report.getFileId().trim();

        // SCENARIO 1: File is already bound -> Enrich metadata from DB without re-promoting
        if (existingFileIds != null && existingFileIds.contains(fileId)) {
            fileMetadataEnrichmentService.enrichDocument(report, deptId);
            return; // Do NOT add to newlyPromotedList (protects existing files during transaction rollback)
        }

        // SCENARIO 2: New Temp File -> Promote & Bind to form
        try {
            FileAttachments savedFile = fileBindingService.promoteAndBindFile(
                    fileId,
                    deptId,
                    formId,
                    FileBindingScope.forBemMotor(motorId, stfTestNo)
            );

            // Update DocumentDto metadata with promoted file information
            report.setFileUrl(savedFile.getFilePath());
            report.setFileName(savedFile.getOriginalFileName());
            report.setStatus(savedFile.getStatus());
            report.setFileId(savedFile.getFileId());
            report.setMimeType(savedFile.getMimeType());
            report.setReadOnly(savedFile.isReadOnly());

            newlyPromotedList.add(report); // Track newly promoted file for rollback handling

        } catch (Exception e) {
            log.error("File promotion failed for fileId: {} bound to formId: {}", fileId, formId, e);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "File upload promotion failed for fileId " + fileId + ": " + e.getMessage()
            );
        }
    }


    private void enrichBemMotorFiles(StfPayloads.StaticTestingDetails details, Long subDepartmentId) {
        // BEM sheet no longer carries ptCurveFile attachments.
    }

    private void rollbackPromotedFiles(List<DocumentDto> promotedDocs) {
        if (promotedDocs == null || promotedDocs.isEmpty()) {
            return;
        }

        log.info("Initiating rollback for {} promoted file(s)...", promotedDocs.size());
        for (DocumentDto doc : promotedDocs) {
            if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
                try {
                    fileBindingService.rollbackOrUnbindFile(doc.getFileId());
                    log.info("Successfully rolled back fileId: {}", doc.getFileId());
                } catch (Exception e) {
                    log.error("Failed to execute file rollback for fileId: {}", doc.getFileId(), e);
                }
            }
        }
    }


    private Set<String> extractBemFileIds(StfPayloads.StaticTestingDetails details) {
        Set<String> fileIds = new HashSet<>();
        if (details == null) {
            return fileIds;
        }

        // BEM sheet no longer carries ptCurveFile attachments.
        return fileIds;
    }

}
