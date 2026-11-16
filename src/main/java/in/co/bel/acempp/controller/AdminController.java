package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.auth.resetpswd.*;
import in.co.bel.acempp.dto.admin.motor.*;
import in.co.bel.acempp.dto.batch.*;
import in.co.bel.acempp.dto.dashboard.*;
import in.co.bel.acempp.dto.project.*;
import in.co.bel.acempp.dto.user.*;
import in.co.bel.acempp.audit.Audit;
import in.co.bel.acempp.enums.AuditAction;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin', 'Centre Head')")
public class AdminController {

    private final UserService userService;
    private final ProjectService projectService;
    private final PasswordResetService passwordResetAdminService;
    private final BatchService batchService;
    private final AdminMotorService adminMotorService;
    private final AdminDashboardService adminDashboardService;
    private final BlockchainEventsService blockchainEventsService;

    @PostMapping("/dashboard/stats")
    public ResponseEntity<AcemResponse<DashboardStatsResponse>> getStats(
            @Valid @RequestBody DashboardStatsRequest request) {
        return AcemTemplate.success(
                adminDashboardService.getStats(request),
                "Dashboard statistics fetched successfully");
    }

    @PostMapping("/dashboard/chartData")
    public ResponseEntity<AcemResponse<ChartDataResponse>> getChartData(
            @Valid @RequestBody DashboardStatsRequest request) {
        return AcemTemplate.success(
                adminDashboardService.getChartData(request),
                "Dashboard charts data fetched successfully");
    }

    @PostMapping("/dashboard/active-batches")
    public ResponseEntity<AcemResponse<ActiveBatchesResponse>> getActiveBatches(
            @Valid @RequestBody ActiveBatchesRequest request) {
        return AcemTemplate.success(
                adminDashboardService.getActiveBatches(request),
                "Active batches fetched successfully");
    }

    @PostMapping("/dashboard/blockchain-events")
    public ResponseEntity<AcemResponse<ActiveBatchesResponse>> getBlockchainTransactionList(
            @Valid @RequestBody BlockchainEventsRequest request) {
        return AcemTemplate.success(
                blockchainEventsService.getBlockchainEvents(request),
                "Blockchain transactions fetched successfully");
    }

    @PostMapping("/dashboard/blockchain-events-by-batchId")
    public ResponseEntity<AcemResponse<BlockchainEventsResponse>> getBlockchainTransactionByBatchId(
            @Valid @RequestBody BatchDetailsRequest request) {
        return AcemTemplate.success(
                blockchainEventsService.getBlockchainEventsByBatchId(request),
                "Blockchain transactions fetched successfully");
    }

    @Audit(action = AuditAction.CREATE_USER)
    @PostMapping("/user/create")
    public ResponseEntity<AcemResponse<CreateUserResponse>> createUser(@Valid @RequestBody CreateUserRequest request,
                                                                       Authentication authentication) {
        String createdBy = authentication.getName();
        CreateUserResponse response = userService.createUser(request, createdBy);
        return AcemTemplate.created(response, "User created successfully");
    }

    @Audit(action = AuditAction.UPDATE_USER)
    @PutMapping("/user/update")
    public ResponseEntity<AcemResponse<UpdateUserResponse>> updateUser(@Valid @RequestBody UpdateUserRequest request,
                                                                       Authentication authentication) {

        String updatedBy = authentication.getName();
        UpdateUserResponse response = userService.updateUser(request, updatedBy);
        return AcemTemplate.success(response, "User updated successfully");
    }


    // User Details
    @PostMapping("/user/details")
    public ResponseEntity<AcemResponse<UserDetailsResponse>> getUserDetails(@Valid @RequestBody UserUuidRequest request) {
        UserDetailsResponse response = userService.getUserDetails(request);
        return AcemTemplate.success(response, "User details fetched successfully");
    }

    // Delete User
    @Audit(action = AuditAction.DELETE_USER)
    @DeleteMapping("/user")
    public ResponseEntity<AcemResponse<DeleteUserResponse>> deleteUser(@Valid @RequestBody UserUuidRequest request) {
        return AcemTemplate.success(userService.deleteUser(request), "User deleted successfully");
    }

    // Activate User (soft-reactivate after delete/INACTIVE)
    @Audit(action = AuditAction.ACTIVATE_USER)
    @PostMapping("/user/activate")
    public ResponseEntity<AcemResponse<ActivateUserResponse>> activateUser(
            @Valid @RequestBody UserUuidRequest request) {
        return AcemTemplate.success(
                userService.activateUser(request),
                "User activated successfully");
    }

    @PostMapping("/user/list")
    public ResponseEntity<AcemResponse<UserListResponse>> getUserList(@Valid @RequestBody UserListRequest request) {
        return AcemTemplate.success(userService.getUserList(request), "Users fetched successfully"
        );
    }

    @PostMapping("/user/stats")
    public ResponseEntity<AcemResponse<UserStatsResponse>> getUserStats(@Valid @RequestBody UserStatsRequest request) {
        return AcemTemplate.success(userService.getUserStats(request), "User statistics fetched successfully"
        );
    }

    @PostMapping("project/create")
    public ResponseEntity<AcemResponse<CreateProjectResponse>> createProject(@Valid @RequestBody CreateProjectRequest
                                                                                     request,
                                                                             Authentication authentication) {
        String createdBy = authentication.getName();
        CreateProjectResponse response =
                projectService.createProject(request, createdBy);
        return AcemTemplate.created(response, "Project created successfully"
        );
    }

    @PostMapping("project/update")
    public ResponseEntity<AcemResponse<UpdateProjectResponse>> updateProject(@Valid @RequestBody UpdateProjectRequest
                                                                                         request) {
        UpdateProjectResponse response = projectService.updateProject(request);
        return AcemTemplate.success(response, "Project updated successfully");
    }

    @PreAuthorize("hasAnyAuthority('Admin','User', 'Centre Head')")
    @PostMapping("/project/list")
    public ResponseEntity<AcemResponse<ProjectListResponse>> getProjectList(@Valid @RequestBody ProjectListRequest
                                                                                        request) {
        ProjectListResponse data = projectService.getProjectList(request);
        return AcemTemplate.success(data, "Project list fetched successfully");
    }

    @DeleteMapping("/project/delete")
    public ResponseEntity<AcemResponse<DeleteProjectResponse>> deleteProject(
            @Valid @RequestBody DeleteProjectRequest request) {
        DeleteProjectResponse response = projectService.deleteProject(request);
        return AcemTemplate.success(response, "Project deleted successfully"
        );
    }

    @PostMapping("/project/stats")
    public ResponseEntity<AcemResponse<ProjectStatsResponse>> getProjectStats(
            @Valid @RequestBody ProjectStatsRequest request) {

        ProjectStatsResponse response = projectService.getProjectStats(request);

        return AcemTemplate.success(
                response,
                "Project statistics fetched successfully"
        );
    }

    @PostMapping("/batch/create")
    public ResponseEntity<AcemResponse<BatchResponseData>> createBatch(
            @Valid @RequestBody CreateBatchRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        return AcemTemplate.created(
                batchService.createBatch(request, userId),
                "Batch created successfully");
    }

    @PutMapping("/batch/update")
    public ResponseEntity<AcemResponse<UpdateBatchResponse>> updateBatch(
            @Valid @RequestBody UpdateBatchRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        UpdateBatchResponse response = batchService.updateBatch(request, userId);
        return AcemTemplate.success(response, "Batch updated successfully");
    }

    @PreAuthorize("hasAnyAuthority('Admin','System Manager', 'User', 'Approver', 'Centre Head')")
    @PostMapping("/batch/details")
    public ResponseEntity<AcemResponse<BatchDetailsResponse>> getBatchDetails(
            @Valid @RequestBody BatchDetailsRequest request) {   // simple DTO with @NotBlank batchId
        BatchDetailsResponse data = batchService.getBatchDetails(request.getBatchId());
        return AcemTemplate.success(data, "Batch details fetched successfully");
    }

    @PostMapping("/batch/list")
    public ResponseEntity<AcemResponse<BatchListResponse>> getBatchList(
            @Valid @RequestBody BatchListRequest request) {
        return AcemTemplate.success(
                batchService.getBatchList(request),
                "Batch list fetched successfully");
    }

    @PostMapping("/batch/stats")
    public ResponseEntity<AcemResponse<BatchStatsResponse>> getBatchStats(
            @Valid @RequestBody BatchStatsRequest request) {
        return AcemTemplate.success(
                batchService.getBatchStats(request),
                "Batch statistics fetched successfully");
    }

    @DeleteMapping("/batch")
    public ResponseEntity<AcemResponse<DeleteBatchResponse>> deleteBatch(
            @Valid @RequestBody DeleteBatchRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        return AcemTemplate.success(
                batchService.softDeleteBatch(request, userId),
                "Batch deleted successfully");
    }

    @PostMapping("/motor/list")
    public ResponseEntity<AcemResponse<MotorListResponse>> getMotorList(
            @RequestBody MotorListRequest request,
            Authentication authentication) {
        return AcemTemplate.success(
                adminMotorService.getMotorList(
                        request != null ? request : new MotorListRequest(),
                        authentication),
                "Motor list fetched successfully");
    }

    @PreAuthorize("hasAnyAuthority('Admin', 'Centre Head', 'System Manager')")
    @PostMapping("/motor/stats")
    public ResponseEntity<AcemResponse<MotorStatsResponse>> getMotorStats(
            @RequestBody(required = false) MotorStatsRequest request,
            Authentication authentication) {
        return AcemTemplate.success(
                adminMotorService.getMotorStats(
                        request != null ? request : new MotorStatsRequest(),
                        authentication),
                "Motor statistics fetched successfully");
    }

    @PreAuthorize("hasAnyAuthority('Admin', 'Centre Head', 'System Manager')")
    @PostMapping("/motor/details")
    public ResponseEntity<AcemResponse<MotorDetailsResponse>> getMotorDetails(
            @Valid @RequestBody MotorDetailsRequest request,
            Authentication authentication) {
        return AcemTemplate.success(
                adminMotorService.getMotorDetails(request, authentication),
                "Motor details fetched successfully");
    }

    @PreAuthorize("hasAuthority('Admin')")
    @PostMapping("/motor/discard")
    public ResponseEntity<AcemResponse<MotorDiscardResponse>> discardMotor(
            @Valid @RequestBody MotorDiscardRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        return AcemTemplate.success(
                adminMotorService.discardMotor(request, userId),
                "Motor discarded successfully");
    }


    @PostMapping("/reset-password/list")
    public ResponseEntity<AcemResponse<PasswordResetListResponse>> getResetRequests(
            @RequestBody PasswordResetListRequest request) {
        return AcemTemplate.success(
                passwordResetAdminService.getResetRequests(request),
                "Password reset requests fetched successfully");
    }

    @PostMapping("/reset-password/changeStatus")
    public ResponseEntity<AcemResponse<ChangeResetStatusResponse>> changeStatus(
            @Valid @RequestBody ChangeResetStatusRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        String userId = (String) authentication.getDetails();

        String message = request.getAction().equalsIgnoreCase("APPROVE")
                ? "Password reset request approved successfully"
                : "Password reset request rejected";

        return AcemTemplate.success(
                passwordResetAdminService.changeStatus(request, userId, username),
                message);
    }

}
