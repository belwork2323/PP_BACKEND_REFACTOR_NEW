package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.auth.ChangePasswordRequest;
import in.co.bel.acempp.dto.auth.ChangePasswordResponse;
import in.co.bel.acempp.dto.batch.MasterDataDto;
import in.co.bel.acempp.dto.batch.OvenMasterDto;
import in.co.bel.acempp.dto.general.*;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleListRequest;
import in.co.bel.acempp.dto.user.UserDetailsResponse;
import in.co.bel.acempp.dto.user.UserUuidRequest;
import in.co.bel.acempp.entity.batch.EquipmentTypeMaster;
import in.co.bel.acempp.entity.batch.UnitMaster;
import in.co.bel.acempp.entity.batch.SubscaleArticleMaster;
import in.co.bel.acempp.entity.BeamEnergy;
import in.co.bel.acempp.entity.EquipmentMaster;
import in.co.bel.acempp.entity.manufacturing.MixingCycleMaster;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.SystemService;
import in.co.bel.acempp.service.UserService;
import in.co.bel.acempp.service.masterdata.MixingCycleMasterDataService;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.ListGroup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system")
public class SystemController {
    private final SystemService systemService;
    private final UserService userService;
    private final MixingCycleMasterDataService mixingCycleMasterDataService;

    @GetMapping("/roles")
    public ResponseEntity<AcemResponse<List<RolesDto>>> getAllRoles() {
        log.info("Fetching all system roles");
        List<RolesDto> roles = systemService.getAllRoles();
        return AcemTemplate.success(roles, "Roles list fetched successfully");
    }

    @GetMapping("/departments")
    public ResponseEntity<AcemResponse<List<DeptsDto>>> getAllDepartments() {
        log.info("Fetching all departments");
        List<DeptsDto> departments = systemService.getAllDepartments();
        return AcemTemplate.success(departments, "Departments fetched successfully");
    }

    @GetMapping("/sub-departments")
    public ResponseEntity<AcemResponse<List<SubdeptFlat>>> getAllSubDepartments() {
        log.info("Fetching all sub-departments");
        List<SubdeptFlat> subDepartments = systemService.getAllSubDepartments();
        return AcemTemplate.success(subDepartments, "Sub-departments fetched successfully");
    }

    @PostMapping("/change-password")
    public ResponseEntity<AcemResponse<ChangePasswordResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        String userId = (String) authentication.getDetails();
        return AcemTemplate.success(
                systemService.changePassword(request, userId),
                "Password changed successfully");
    }

    @PostMapping("/user/profile-details")
    public ResponseEntity<AcemResponse<UserDetailsResponse>> getUserDetails(@Valid @RequestBody UserUuidRequest request) {
        UserDetailsResponse response = userService.getUserDetails(request);
        return AcemTemplate.success(response, "User details fetched successfully");
    }

    @GetMapping("/mixers")
    public ResponseEntity<AcemResponse<List<MasterDataDto>>> getMixerMaster() {

        return AcemTemplate.success(
                systemService.getMixerMaster(),
                "Mixer master data fetched successfully"
        );
    }

    @GetMapping("/buildings")
    public ResponseEntity<AcemResponse<List<MasterDataDto>>> getBuildingMaster() {

        return AcemTemplate.success(
                systemService.getBuildingMaster(),
                "Building master data fetched successfully"
        );
    }

    @GetMapping("/ovens")
    public ResponseEntity<AcemResponse<List<OvenMasterDto>>> getOvenMaster() {

        return AcemTemplate.success(
                systemService.getOvenMaster(),
                "Oven master data fetched successfully"
        );
    }

    @GetMapping("/subscale-articles")
    public ResponseEntity<AcemResponse<List<SubscaleArticleMaster>>> getSubscaleArticles(
            @RequestParam(defaultValue = "true") Boolean isActive) {

        return AcemTemplate.success(
                systemService.getSubscaleArticles(isActive),
                "Subscale article master fetched successfully"
        );
    }

    @PostMapping("/mixing-cycles")
    public ResponseEntity<AcemResponse<MixingCycleListRequest.MixingCycleDetailsResponse>> getMixingCycle(
            @Validated(DetailGroup.class) @RequestBody MixingCycleListRequest request) {

        return AcemTemplate.success(
                systemService.getMixingCycleDetails(request.getMixingCycleCode() , request.getMixerType()),
                "Mixing cycle details fetched successfully"
        );
    }

    @GetMapping("/mixing-cycle-list")
    public ResponseEntity<AcemResponse<List<MixingCycleDto>>> getMasterSummary() {
        List<MixingCycleDto> data = systemService.getMixingCycleList();
        return AcemTemplate.success(data, "Mixing cycle master fetched successfully");
    }


    @PostMapping("/mixing-cycles-project")
    public ResponseEntity<AcemResponse<List<MixingCycleDto>>> getMixingCycleByProjectId(@Valid @RequestBody in.co.bel.acempp.dto.masterdata.mixingcycles.MixingCycleListRequest request) {
        List<MixingCycleDto> data = mixingCycleMasterDataService.findActiveByProjectIdAndMotorStage(request);
        return AcemTemplate.success(data, "Mixing cycle master fetched successfully");
    }

    @GetMapping("/energy-beam-list")
    public ResponseEntity<AcemResponse<List<BeamEnergy>>> getEnergyBeamMaster() {
        List<BeamEnergy> data = systemService.getBeamEnergyList();
        return AcemTemplate.success(data, "Beam energy master fetched successfully");
    }

    @GetMapping("/unit-list")
    public ResponseEntity<AcemResponse<List<UnitMaster>>> getUnitMaster() {
        List<UnitMaster> data = systemService.getUnitList();
        return AcemTemplate.success(data, "Unit master fetched successfully");
    }

    @GetMapping("/equipment-type-list")
    public ResponseEntity<AcemResponse<List<EquipmentTypeMaster>>> getEquipmentTypeMaster() {
        List<EquipmentTypeMaster> data = systemService.getEquipmentTypeList();
        return AcemTemplate.success(data, "Equipment type master fetched successfully");
    }

    @GetMapping("/equipment-list")
    public ResponseEntity<AcemResponse<List<EquipmentMaster>>> getEquipmentMaster() {
        List<EquipmentMaster> data = systemService.getEquipmentMaster();
        return AcemTemplate.success(data, "Equipment master fetched successfully");
    }


}
