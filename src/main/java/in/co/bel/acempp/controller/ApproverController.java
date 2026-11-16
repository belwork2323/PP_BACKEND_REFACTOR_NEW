package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.batch.SubDeptStatsRequest;
import in.co.bel.acempp.dto.batch.SubDeptStatsResponse;
import in.co.bel.acempp.dto.form.FormPdfRequest;
import in.co.bel.acempp.dto.form.FormPdfResponse;
import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.casepreparation.*;
import in.co.bel.acempp.dto.rawmaterialpreparation.*;
import in.co.bel.acempp.dto.statictestingfacility.bem.ApproverBemStatusRequest;
import in.co.bel.acempp.dto.statictestingfacility.bem.BemMotorListResponseDto;
import in.co.bel.acempp.dto.qcdivision.QcDivisionApprovalRequest;
import in.co.bel.acempp.dto.qcdivision.QcDivisionFormResponse;
import in.co.bel.acempp.dto.statictestingfacility.bem.BemMotorRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotListRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotListResponse;
import in.co.bel.acempp.dto.rocketmotorcasing.*;
import in.co.bel.acempp.dto.user.operations.UserBatchListRequest;
import in.co.bel.acempp.dto.user.operations.UserBatchListResponse;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.BatchService;
import in.co.bel.acempp.service.dispatch.DispatchService;
import in.co.bel.acempp.service.FormPdfService;
import in.co.bel.acempp.service.manufacturing.*;
import in.co.bel.acempp.service.qualitycontrol.BemMotorService;
import in.co.bel.acempp.service.qualitycontrol.NdtService;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionService;
import in.co.bel.acempp.service.qualitycontrol.StaticTestingService;
import in.co.bel.acempp.service.sourcing.RawMaterialSourcingService;
import in.co.bel.acempp.service.sourcing.RocketMotorCasingService;
import in.co.bel.acempp.service.UserOperationService;
import in.co.bel.acempp.validation.groups.ListGroup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/approver")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('Approver')")
public class ApproverController {

    private final RocketMotorCasingService rmcService;
    private final RawMaterialSourcingService rmpService;
    private final RawMaterialPrepService rawMaterialPrepService;
    private final CasePreparationService casePreparationService;
    private final MixingService mixingService;
    private final CastingCuringService castingCuringService;
    private final PostCureOptService postCureOptService;
    private final SubscaleProcessingService subscaleProcessingService;
    private final TrimmingService trimmingService;
    private final NdtService ndtService;
    private final QcDivisionService qcDivisionService;
    private final StaticTestingService staticTestingService;
    private final DispatchService dispatchService;
    private final BatchService batchService;
    private final FormPdfService formPdfService;
    private final BemMotorService bemMotorService;
    private final UserOperationService userOptService;

    private String getUserId(Authentication auth) {
        return (String) auth.getDetails();
    }

    // Generate PDF (Admin / Centre Head / System Manager for Motor Management read-only views)
    @PreAuthorize("hasAnyAuthority('Approver', 'Admin', 'Centre Head', 'System Manager')")
    @PostMapping("/form/pdf")
    public ResponseEntity<AcemResponse<FormPdfResponse>> generatePdf(
            @Valid @RequestBody FormPdfRequest request,
            Authentication authentication) {

        FormPdfResponse pdf = formPdfService.generatePdf(
                request,getUserId(authentication));

        // If download=true, set Content-Disposition header for browser download
        if (request.isDownload()) {
            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename=\"" + pdf.getFileName() + "\"")
                    .body(AcemResponse.<FormPdfResponse>builder()
                            .success(true).code(200)
                            .message("PDF generated successfully")
                            .timestamp(LocalDateTime.now().toString())
                            .data(pdf).build());
        }

        return AcemTemplate.success(pdf, "PDF generated successfully");
    }
    @PostMapping("/form/premix/change-status")
    public ResponseEntity<AcemResponse<PremixApprovalResponse>> changeStatusOfPremix(
            @Valid @RequestBody PremixApprovalRequest request,
            Authentication authentication) {

        String approverName = getUserId(authentication);
        log.info("Approver premix approval request: subDepartmentId={}, formId={}, actionType={}, premixNo={}",
                request.getSubDepartmentId(), request.getFormId(), request.getActionType(), request.getPremixNo());

        PremixApprovalResponse response = switch (request.getSubDepartmentId().intValue()) {
            case 3 -> rawMaterialPrepService.changeStatusOfPremix(
                    request,
                    approverName
            );

            case 5 -> mixingService.changeStatusOfPremix(
                    request,
                    approverName
            );

            case 11 -> qcDivisionService.changeStatusOfPremix(request, approverName);

            default -> throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Premix approval is not supported for subDepartmentId: "
                            + request.getSubDepartmentId()
            );
        };

        String message = request.getActionType() == Status.APPROVED
                ? "Premix approved successfully"
                : "Premix rejected successfully";

        return AcemTemplate.success(response, message);
    }
    @PostMapping("/form/motor/change-status")
    public ResponseEntity<AcemResponse<MotorApprovalResponse>> changeStatusOfMotor(
            @Valid @RequestBody MotorApprovalRequest request,
            Authentication authentication) {

        if (request.getSubDepartmentId() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "subDepartmentId is required for motor approval");
        }

        String approverUserId = getUserId(authentication);
        log.info("Approver motor approval request: subDepartmentId={}, formId={}, actionType={}, motorId={},",
                request.getSubDepartmentId(), request.getFormId(), request.getActionType(), request.getMotorId());
        MotorApprovalResponse response = switch (request.getSubDepartmentId().intValue()) {
            case 4 -> casePreparationService.changeStatusOfMotor(request, approverUserId);
            case 6 -> castingCuringService.changeStatusOfMotor(request, approverUserId);
            case 7 -> postCureOptService.changeStatusOfMotor(request, approverUserId);
            case 9 -> trimmingService.changeStatusOfMotor(request, approverUserId);
            case 10 -> ndtService.changeStatusOfMotor(request, approverUserId);
            case 11 -> qcDivisionService.changeStatusOfMotor(request, approverUserId);
            case 12 -> staticTestingService.changeStatusOfMotor(request, approverUserId);
            case 13 -> dispatchService.changeStatusOfMotor(request, approverUserId);
            default -> throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Motor approval is not supported for subDepartmentId: "
                            + request.getSubDepartmentId());
        };

        return AcemTemplate.success(
                response,
                request.getActionType() == Status.APPROVED
                        ? "Motor approved successfully"
                        : "Motor rejected successfully"
        );
    }

    @PostMapping("/form/division/change-status")
    public ResponseEntity<AcemResponse<QcDivisionFormResponse>> changeStatusOfQcDivision(
            @Valid @RequestBody QcDivisionApprovalRequest request,
            Authentication authentication) {
        QcDivisionFormResponse response = qcDivisionService.changeStatusOfDivision(
                request, getUserId(authentication));
        return AcemTemplate.success(
                response,
                request.getActionType() == Status.APPROVED
                        ? "QC division approved successfully"
                        : "QC division rejected successfully");
    }
    @PostMapping("/form/change-status")
    public ResponseEntity<AcemResponse<ApproverStatusResponse>> changeStatus(
            @Valid @RequestBody ApproverStatusRequest request,
            Authentication authentication) {

        Long subDeptId = request.getSubDepartmentId();
        ApproverStatusResponse response;
        return switch (subDeptId.intValue()) {
            case 1 -> { // LOT
                response = rmpService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Raw material sourcing approved successfully"
                        : "Raw material sourcing rejected successfully");
            }
            case 2 -> { // RMC
                response = rmcService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Rocket motor casing approved successfully"
                        : "Rocket motor casing rejected successfully");
            }
            case 3 -> { // RMP
                response = rawMaterialPrepService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Raw material preparation form approved successfully"
                        : "Raw material preparation form rejected successfully");
            }
            case 4 -> { // Case Preparation
                response = casePreparationService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Case preparation form approved successfully"
                        : "Case preparation form rejected successfully");
            }
            case 5 -> { // Mixing Preparation
                response = mixingService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Mixing form approved successfully"
                        : "Mixing form rejected successfully");
            }
            case 6 -> { // Casting and Curing
                response = castingCuringService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Casting and Curing form approved successfully"
                        : "Casting and Curing form rejected successfully");
            }
            case 7 -> { // Post Cure Operations
                response = postCureOptService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Post Cure Operations form approved successfully"
                        : "Post Cure Operations form rejected successfully");
            }
            case 8 -> { // Subscale
                response = subscaleProcessingService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Subscale form approved successfully"
                        : "Subscale form rejected successfully");
            }
            case 9 -> { // Trimming
                response = trimmingService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Trimming form approved successfully"
                        : "Trimming form rejected successfully");
            }
            case 10 -> { // NDT
                response = ndtService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "NDT form approved successfully"
                        : "NDT form rejected successfully");
            }
            case 11 -> { // QC Division
                response = qcDivisionService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "QC Division form approved successfully"
                        : "QC Division form rejected successfully");
            }
            case 12 -> { // Static Test Facility
                response = staticTestingService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Static Test Facility form approved successfully"
                        : "Static Test Facility form rejected successfully");
            }
            case 13 -> { // Dispatch
                response = dispatchService.changeStatus(request, getUserId(authentication));
                yield AcemTemplate.success(response,request.getActionType() == Status.APPROVED
                        ? "Dispatch form approved successfully"
                        : "Dispatch form rejected successfully");
            }


            default -> throw new AcemException(ErrorCode.INVALID_REQUEST,  "Unsupported subDepartmentId: " + subDeptId);
        };
    }

    @PostMapping("/subdepartment/dashboard/stats")
    public ResponseEntity<AcemResponse<SubDeptStatsResponse>> getStats(
            @Valid @RequestBody SubDeptStatsRequest request) {
        return AcemTemplate.success(batchService.getApproverSubDeptStats(request),
                "Stats fetched successfully");
    }
    @PreAuthorize("hasAnyAuthority('Admin', 'User', 'Approver')")
    @PostMapping("/raw-material-sourcing/list")
    public ResponseEntity<AcemResponse<LotListResponse>> getLotList(
            @Valid @RequestBody LotListRequest request) {

        return AcemTemplate.success(
                rmpService.getLotList(request),
                "Raw material lot list fetched successfully"
        );
    }
    @PreAuthorize("hasAnyAuthority('Admin', 'User', 'Approver')")
    @PostMapping("/rocket-motor-casing/list")
    public ResponseEntity<AcemResponse<RocketMotorCasingListResponse>> getCasingList(
            @Valid @RequestBody RocketMotorCasingListRequest request) {
        return AcemTemplate.success(userOptService.getCasingList(request),
                "Rocket motor casing list fetched successfully"
        );
    }

    @PostMapping("/subdepartment/batch-list")
    public ResponseEntity<AcemResponse<UserBatchListResponse>> getBatchList(
            @Valid @RequestBody UserBatchListRequest request, Authentication authentication) {
        String userId = (String) authentication.getDetails();
        request.setUserId(userId);
        return AcemTemplate.success(
                userOptService.getBatchList(request),
                "Batch list fetched successfully");
    }

    @PostMapping("/bem-motor/list")
    public ResponseEntity<AcemResponse<BemMotorListResponseDto>>
    getBemMotorsWaitingForApproval(
      @Validated(ListGroup.class) @RequestBody BemMotorRequest request) {

        BemMotorListResponseDto data =
                bemMotorService.getBemMotorList(request);

        return AcemTemplate.success(
                data,
                "BEM motors waiting for approval retrieved successfully"
        );
    }

    @PostMapping("/bem-motor/change-status")
    public ResponseEntity<AcemResponse<ApproverStatusResponse>> changeBemMotorStatus(
            @RequestBody ApproverBemStatusRequest request,
            Authentication authentication) {

        String userId = (String) authentication.getDetails();

        ApproverStatusResponse response =
                bemMotorService.changeStatus(request, userId);

        return AcemTemplate.success(
                response,
                "BEM motor status updated successfully"
        );
    }
}
