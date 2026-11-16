package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.batch.SubDeptStatsRequest;
import in.co.bel.acempp.dto.batch.SubDeptStatsResponse;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckRequest;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckResponse;
import in.co.bel.acempp.dto.rocketmotorcasing.*;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.*;
import in.co.bel.acempp.dto.user.operations.*;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.BatchService;
import in.co.bel.acempp.service.manufacturing.QualityCheckService;
import in.co.bel.acempp.service.sourcing.RocketMotorCasingService;
import in.co.bel.acempp.service.UserOperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/v1/user/subdepartment")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class UserOperationController {

    private final UserOperationService userOptService;
    private final BatchService batchService;
    private final RocketMotorCasingService rmcService;
    private final QualityCheckService qualityCheckService;

    @PreAuthorize("hasAnyAuthority('User', 'Admin','Approver')")
    @PostMapping("/materials-list")
    public ResponseEntity<AcemResponse<List<MaterialDto>>> getMaterialsList(
            @RequestBody MaterialListRequest request) {
        return AcemTemplate.success(userOptService.getMaterialsList(request),
                "Materials fetched successfully");
    }


    @PostMapping("/materials/specification-list")
    public ResponseEntity<AcemResponse<SpecificationListResponse>> getSpecList(
            @Valid @RequestBody SpecificationListRequest request) {
        return AcemTemplate.success(
                userOptService.getSpecificationList(request),
                "Specifications fetched successfully");
    }


    @PostMapping("/dimensional/parameters-list")
    public ResponseEntity<AcemResponse<DimensionalParamListResponse>> getDimParams(
            @Valid @RequestBody DimensionalParamRequest request) {
        return AcemTemplate.success(
                userOptService.getDimensionalParams(request),
                "Dimensional inspection parameters fetched successfully");
    }

    @PreAuthorize("hasAnyAuthority('User', 'Admin','Approver', 'Centre Head')")
    @GetMapping("/motors-stage-list")
    public ResponseEntity<AcemResponse<List<MotorStageDto>>> getMotorStageList(
            @RequestParam(required = false) String projectId) {
        return AcemTemplate.success(
                userOptService.getMotorStageList(projectId),
                "Motor stage list fetched successfully");
    }

    @PreAuthorize("hasAnyAuthority('Admin', 'User', 'Approver')")
    @PostMapping("/rocket-motor-casing/list")
    public ResponseEntity<AcemResponse<RocketMotorCasingListResponse>> getCasingList(
            @Valid @RequestBody RocketMotorCasingListRequest request) {
        return AcemTemplate.success(userOptService.getCasingList(request),
        "Rocket motor casing list fetched successfully"
        );
    }
    @PreAuthorize("hasAnyAuthority('Admin', 'User', 'Approver')")
    @GetMapping("/insulation/type-list")
    public ResponseEntity<AcemResponse<List<InsulationTypeDto>>> getInsulationTypeList() {
        return AcemTemplate.success(
                userOptService.getInsulationTypeList(),
                "Insulation types fetched successfully");
    }

    @PreAuthorize("hasAnyAuthority('Admin', 'User', 'Approver')")
    @PostMapping("/insulation/specification-list")
    public ResponseEntity<AcemResponse<InsulationSpecificationResponse>>
    getInsulationSpecificationList(
            @Valid @RequestBody InsulationSpecificationRequest request) {

        return AcemTemplate.success(
                userOptService.getInsulationSpecifications(request),
                "Insulation specifications fetched successfully"
        );
    }
    @PreAuthorize("hasAnyAuthority('User','Admin')")
    @PostMapping("/approved-motors-list")
    public ResponseEntity<AcemResponse<AvailableMotorListResponse>> getAvailableMotors(@Valid @RequestBody
                                                                                       AvailableMotorRequest request) {
        return AcemTemplate.success(rmcService.getAvailableMotors(request),
                "Available motor casings fetched successfully"
        );
    }
    @PreAuthorize("hasAnyAuthority('User','Admin', 'Approver')")
    @GetMapping("/casting-station")
    public ResponseEntity<AcemResponse<List<CastingStationDto>>> getCastingStations() {
        List<CastingStationDto> data = userOptService.getCastingStations();
        return AcemTemplate.success(data, "Casting stations fetched successfully");
    }

    @PostMapping("/batch-list")
    public ResponseEntity<AcemResponse<UserBatchListResponse>> getBatchList(
            @Valid @RequestBody UserBatchListRequest request, Authentication authentication) {
        String userId = (String) authentication.getDetails();
        request.setUserId(userId);
        return AcemTemplate.success(
                userOptService.getBatchList(request),
                "Batch list fetched successfully");
    }

    @PostMapping("/dashboard/stats")
    public ResponseEntity<AcemResponse<SubDeptStatsResponse>> getStats(
            @Valid @RequestBody SubDeptStatsRequest request) {
        return AcemTemplate.success(batchService.getUserSubDeptStats(request),
                "Stats fetched successfully");
    }
    @PostMapping("/motors")
    public ResponseEntity<AcemResponse<MotorListResponse>> getMotors(
            @Valid @RequestBody MotorListRequest request) {
        MotorListResponse data = userOptService.getMotorsByBatch(request.getBatchId());
        return AcemTemplate.success(data, "Motors linked to batch fetched successfully");
    }
    @PreAuthorize("hasAnyAuthority('User', 'Approver')")
    @PostMapping("/material-lots")
    public ResponseEntity<AcemResponse<MaterialLotResponse>> getMaterialLots(
            @Valid @RequestBody MaterialLotRequest request) {
        return AcemTemplate.success(
                userOptService.getMaterialLotsByBatch(request.getBatchId()),
                "Material lots fetched successfully");
    }

    @PostMapping("/mixing/quality-checks")
    public ResponseEntity<AcemResponse<QualityCheckResponse>> getQCMixingParams(
            @Valid @RequestBody QualityCheckRequest request) {
        return AcemTemplate.success(
                qualityCheckService.getQualityChecks(request),
                "Quality Checks fetched successfully");
    }

}
