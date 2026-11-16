package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.batch.batchdetails.*;
import in.co.bel.acempp.dto.dashboard.*;
import in.co.bel.acempp.dto.dashboard.systemmanagerdashboard.*;
import in.co.bel.acempp.dto.dashboard.systemmanagerdashboard.ManagerActiveBatchesRequest;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.SysMgrDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system-manager/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('System Manager', 'Centre Head')")
public class SystemManagerController {

    private final SysMgrDashboardService dashboardService;

    @PostMapping("/stats")
    public ResponseEntity<AcemResponse<ManagerDashboardStatsResponse>> getStats(
            @Valid @RequestBody ManagerDashboardStatsRequest request) {

        return AcemTemplate.success(
                dashboardService.getStats(request),
                "Manager dashboard statistics fetched successfully"
        );
    }

    @PostMapping("/chartData")
    public ResponseEntity<AcemResponse<ManagerDashboardChartResponse>>
    getChartData(
            @Valid @RequestBody ManagerDashboardChartRequest request) {

        return AcemTemplate.success(
                dashboardService.getChartData(request),
                "Manager dashboard charts data fetched successfully"
        );
    }

    @PostMapping("/active-batches")
    public ResponseEntity<
            AcemResponse<SystemManagerActiveBatchesResponse>> getActiveBatches(
            @Valid @RequestBody ManagerActiveBatchesRequest request,
            Authentication authentication) {

        return AcemTemplate.success(
                dashboardService.getManagerActiveBatches(
                        request,
                        authentication.getName()
                ),"Active batches fetched successfully"
        );
    }
    @PostMapping("/batch-subdept-stages")
    @PreAuthorize("hasAnyAuthority('Admin', 'System Manager', 'Centre Head')")
    public ResponseEntity<AcemResponse<BatchStatusListResponse>> getBatchSubDepartmentStages(
            @Valid @RequestBody BatchStatusListRequest request) {

        return AcemTemplate.success(
                dashboardService.getBatchStatusList(request),
                "Batch subdepartment status list fetched successfully"
        );
    }

    @PostMapping("/blockchain-events")
    public ResponseEntity<AcemResponse<BlockchainEventsResponse>> getBlockchainEvents(
            @Valid @RequestBody ManagerBlockchainEventsRequest request) {

        return AcemTemplate.success(
                dashboardService.getManagerBlockchainEvents(request),
                "Blockchain events fetched successfully");
    }

    @PostMapping("/alerts")
    public ResponseEntity<AcemResponse<AlertsResponse>> getAlerts(
            @Valid @RequestBody AlertsRequest request) {

        return AcemTemplate.success(
                dashboardService.getAlerts(request),
                "Alerts fetched successfully");
    }

    @PreAuthorize("hasAnyAuthority('Admin', 'System Manager', 'Centre Head')")
    @PostMapping("/batch/details")
    public ResponseEntity<AcemResponse<SystemManagerBatchDetailsResponse>> getBatchDetails(
            @Valid @RequestBody SystemManagerBatchDetailsRequest request) {
        return AcemTemplate.success(
                dashboardService.getBatchDetails(request),
                "System manager batch details fetched successfully");
    }

    @PostMapping("/batch-dept-stages")
    @PreAuthorize("hasAnyAuthority('Admin', 'System Manager', 'Centre Head')")
    public ResponseEntity<AcemResponse<BatchStagesResponse>> getBatchStages(
            @Valid @RequestBody BatchStagesRequest request) {
        return AcemTemplate.success(
                dashboardService.getBatchStages(request.getBatchId()),
                "Batch stages fetched successfully");
    }
}
