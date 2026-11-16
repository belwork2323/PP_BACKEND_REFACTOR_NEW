package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.auth.ChangePasswordRequest;
import in.co.bel.acempp.dto.auth.ChangePasswordResponse;
import in.co.bel.acempp.dto.batch.MasterDataDto;
import in.co.bel.acempp.dto.batch.OvenMasterDto;
import in.co.bel.acempp.dto.general.DeptsDto;
import in.co.bel.acempp.dto.general.RolesDto;
import in.co.bel.acempp.dto.general.SubDeptsDto;
import in.co.bel.acempp.dto.general.SubdeptFlat;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleListRequest;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.entity.*;
import in.co.bel.acempp.entity.batch.EquipmentTypeMaster;
import in.co.bel.acempp.entity.batch.UnitMaster;
import in.co.bel.acempp.entity.batch.SubscaleArticleMaster;
import in.co.bel.acempp.entity.manufacturing.MixingCycleMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.*;
import in.co.bel.acempp.repository.manufacturing.MixingCycleRepository;
import in.co.bel.acempp.service.masterdata.MixingCycleMasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemService {

    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MixerMasterRepository mixerRepository;
    private final BuildingMasterRepository buildingRepository;
    private final OvenMasterRepository ovenMasterRepository;
    private final MixingCycleRepository mixingCycleRepository;
    private final SubscaleArticleRepository subscaleArticleRepository;
    private final BeamEnergyRepository beamEnergyRepository;
    private final UnitMasterRepository unitMasterRepository;
    private final EquipmentTypeMasterRepository equipmentTypeMasterRepository;
    private final EquipmentRepository equipmentRepository;
    // ---------- ROLES ----------
    public List<RolesDto> getAllRoles() {
        List<RoleMaster> roles = roleRepository.findAllByOrderBySortAsc();
        if (roles.isEmpty()) {
            throw new AcemException(ErrorCode.ROLES_NOT_FOUND, "No roles found");

        }
        return
                roles.stream()
                        .map(r -> RolesDto.builder().roleId(r.getRoleId())
                                .roleName(r.getRoleName())
                                .build())
                        .collect(Collectors.toList());
    }


    // ---------- DEPARTMENTS (nested) ----------
    public List<DeptsDto> getAllDepartments() {
        List<DepartmentMaster> departments = departmentRepository.findAllByOrderByDepartmentIdAsc();
        if (departments.isEmpty()) {
            throw new AcemException(ErrorCode.DEPARTMENTS_NOT_FOUND, "No departments found");
        }
        return departments.stream()
                .map(dept -> DeptsDto.builder()
                        .departmentId(dept.getDepartmentId())     // business key
                        .departmentName(dept.getDepartmentName())
                        .subDepartments(
                                dept.getSubDepartments() == null
                                        ? List.of()
                                        : dept.getSubDepartments().stream()
                                        .map(sd -> SubDeptsDto.builder()
                                                .subDepartmentId(sd.getSubDepartmentId())
                                                .subDepartmentName(sd.getSubDepartmentName())
                                                .build())
                                        .collect(Collectors.toList())
                        )
                        .build())
                .collect(Collectors.toList());
    }

    // ---------- FLAT SUB-DEPARTMENTS ----------
    public List<SubdeptFlat> getAllSubDepartments() {
        List<DepartmentMaster> departments = departmentRepository.findAll();
        List<SubdeptFlat> result = new ArrayList<>();
        for (DepartmentMaster dept : departments) {
            if (dept.getSubDepartments() != null) {
                for (SubDepartmentMaster sd : dept.getSubDepartments()) {
                    result.add(SubdeptFlat.builder()
                            .subDepartmentId(sd.getSubDepartmentId())
                            .subDepartmentName(sd.getSubDepartmentName())
                            .departmentId(dept.getDepartmentId())   // from parent
                            .build());
                }
            }
        }
        if (result.isEmpty()) {
            throw new AcemException(ErrorCode.SUB_DEPARTMENTS_NOT_FOUND,
                    "No sub-departments found");
        }
        return result;
    }

    //    ------------------------------MIXING CYCLE DETAILS AND LIST-------------------------------------
//    public List<MixingCycleSummaryDto> getMixingCycleList() {
//        List<MixingCycleMaster> items = mixingCycleRepository.findAll();
//        if (items.isEmpty()) {
//            throw new RuntimeException("NO_DATA: No mixing cycles configured");
//        }
//        return items.stream()
//                .map(m -> MixingCycleSummaryDto.builder()
//                        .mixingCycleId(m.getId())
//                        .mixingCycleCode(m.getMixingCycleCode())
//                        .mixingCycleName(m.getMixingCycleName())
//                        .motorStage(m.getMotorStage())
//                        .build())
//                .toList();
//    }
    public List<MixingCycleDto> getMixingCycleList() {
        List<MixingCycleMaster> cycles = mixingCycleRepository.findAll();

        return cycles.stream()
                .filter(MixingCycleMasterDataService::isActive)
                .map(c -> MixingCycleDto.builder()
                        .mixingCycleId(c.getId())
                        .mixingCycleCode(c.getMixingCycleCode())
                        .mixingCycleName(c.getMixingCycleName())
                        .build())
                .toList();
    }
    public List<MixingCycleDto> getMixingCycleByProjectId(MixingCycleListRequest request) {
        List<MixingCycleMaster> cycles = mixingCycleRepository.findAll();

        return cycles.stream()
                .filter(MixingCycleMasterDataService::isActive)
                .map(c -> MixingCycleDto.builder()
                        .mixingCycleId(c.getId())
                        .mixingCycleCode(c.getMixingCycleCode())
                        .mixingCycleName(c.getMixingCycleName())
                        .build())
                .toList();
    }



    public MixingCycleListRequest.MixingCycleDetailsResponse getMixingCycleDetails(String mixingCycleCode, String mixerType) {

        if (mixingCycleCode == null || mixingCycleCode.isBlank()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Mixing cycle code is required"
            );
        }

        MixingCycleMaster cycle = mixingCycleRepository.findByMixingCycleCode(mixingCycleCode)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Mixing cycle not found: " + mixingCycleCode
                ));

        if (!MixingCycleMasterDataService.isActive(cycle)) {
            throw new AcemException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Mixing cycle not found: " + mixingCycleCode
            );
        }

        List<MixingCycleListRequest.MixingCycleDetailsResponse.OperationDto> premixOperations = Collections.emptyList();
        List<MixingCycleListRequest.MixingCycleDetailsResponse.OperationDto> finalMixOperations = Collections.emptyList();

        // Initialize quality check lists as empty by default
        List<QualityCheckParams> premixQualityChecks = Collections.emptyList();
        List<QualityCheckParams> finalMixQualityChecks = Collections.emptyList();

        if (cycle.getCycles() != null) {

            // Conditional filtering based on mixerType string
            boolean includePremix = mixerType == null || mixerType.isBlank() || mixerType.equalsIgnoreCase("PREMIX");
            boolean includeFinalMix = mixerType == null || mixerType.isBlank() || mixerType.equalsIgnoreCase("FINAL_MIX");

            if (includePremix && cycle.getCycles().getPremixOperations() != null) {
                premixOperations = cycle.getCycles().getPremixOperations()
                        .stream()
                        .filter(MixingCycleMasterDataService::isOperationActive)
                        .map(op -> MixingCycleListRequest.MixingCycleDetailsResponse.OperationDto.builder()
                                .operationId(op.getOperationId())
                                .sequenceNo(op.getSequenceNo())
                                .operationName(op.getOperationName())
                                .build())
                        .toList();

                premixQualityChecks = MixingCycleMasterDataService.activePremixQualityChecks(cycle);
            }

            if (includeFinalMix && cycle.getCycles().getFinalMixOperations() != null) {
                finalMixOperations = cycle.getCycles().getFinalMixOperations()
                        .stream()
                        .filter(MixingCycleMasterDataService::isOperationActive)
                        .map(op -> MixingCycleListRequest.MixingCycleDetailsResponse.OperationDto.builder()
                                .operationId(op.getOperationId())
                                .sequenceNo(op.getSequenceNo())
                                .operationName(op.getOperationName())
                                .build())
                        .toList();

                finalMixQualityChecks = MixingCycleMasterDataService.activeFinalMixQualityChecks(cycle);
            }
        }

        return MixingCycleListRequest.MixingCycleDetailsResponse.builder()
                .mixingCycleId(cycle.getId())
                .mixingCycleCode(cycle.getMixingCycleCode())
                .mixingCycleName(cycle.getMixingCycleName())
                .description(cycle.getDescription())
                .motorStage(cycle.getMotorStage())
                .cycles(
                        MixingCycleListRequest.MixingCycleDetailsResponse.CyclesDto.builder()
                                .premixOperations(premixOperations)
                                .finalMixOperations(finalMixOperations)
                                .premixQualityChecks(premixQualityChecks)
                                .finalMixQualityChecks(finalMixQualityChecks)
                                .build()
                )
                .build();
    }

    // CHANGE PASSWORD

    public ChangePasswordResponse changePassword(ChangePasswordRequest request, String userId) {

        // 1. Find user by UUID
        User user = userRepository
                .findByUserUUID(request.getUser_uuid())
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND,
                        "User not found"));

        // 2. Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AcemException(ErrorCode.INVALID_CREDENTIALS,
                    "Current password is incorrect");
        }

        // 3. Confirm password must match new password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "New password and confirm password do not match");
        }

        // 4. Validate password strength
        in.co.bel.acempp.util.PasswordPolicy.validateStrength(request.getNewPassword());

        // 5. New password must not match current or any prior password in history
        if (in.co.bel.acempp.util.PasswordPolicy.matchesAnyHistory(
                passwordEncoder,
                request.getNewPassword(),
                user.getPassword(),
                user.getPasswordHistory())) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "New password must be different from previously used passwords");
        }

        // 6. Update password + history; clear forced-change flag and temp password
        String previousHash = user.getPassword();
        user.setPasswordHistory(
                in.co.bel.acempp.util.PasswordPolicy.pushHistory(user.getPasswordHistory(), previousHash));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setTemporaryPassword(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getUsername());

        return ChangePasswordResponse.builder()
                .user_uuid(user.getUserUUID())
                .username(user.getUsername())
                .status(String.valueOf(user.getStatus() == Status.ACTIVE
                        ? Status.ACTIVE
                        : Status.INACTIVE))
                .passwordUpdatedOn(LocalDateTime.now())
                .build();
    }


    public List<MasterDataDto> getMixerMaster() {

        return mixerRepository.findAll()
                .stream()
                .filter(m -> m.getIsActive() == null || Boolean.TRUE.equals(m.getIsActive()))
                .map(m -> MasterDataDto.builder()
                        .id(m.getId())
                        .code(m.getCode())
                        .name(m.getName())
                        .build())
                .toList();
    }


    public List<MasterDataDto> getBuildingMaster() {

        return buildingRepository.findAll()
                .stream()
                .filter(building -> building.getIsActive() == null || Boolean.TRUE.equals(building.getIsActive()))
                .map(building -> MasterDataDto.builder()
                        .id(building.getId())
                        .code(building.getCode())
                        .name(building.getName())
                        .build())
                .toList();
    }

    public List<OvenMasterDto> getOvenMaster() {

        return ovenMasterRepository.findAll()
                .stream()
                .filter(oven -> oven.getIsActive() == null || Boolean.TRUE.equals(oven.getIsActive()))
                .map(oven -> OvenMasterDto.builder()
                        .id(oven.getId())
                        .code(oven.getCode())
                        .name(oven.getName())
                        .noOfOvenAvailable(oven.getNoOfOvenAvailable())
                        .build())
                .toList();
    }

    public List<SubscaleArticleMaster> getSubscaleArticles(Boolean isActive) {

        if (Boolean.TRUE.equals(isActive)) {
            return subscaleArticleRepository
                    .findByIsActiveTrueOrderByDisplayOrderAscSubscaleArticleNameAsc();
        }

        return subscaleArticleRepository
                .findAllByOrderByDisplayOrderAscSubscaleArticleNameAsc();
    }

    public List<BeamEnergy> getBeamEnergyList() {
        return beamEnergyRepository.findAll().stream()
                .filter(item -> item.getIsActive() == null || Boolean.TRUE.equals(item.getIsActive()))
                .toList();
    }

    public List<UnitMaster> getUnitList() {
        return unitMasterRepository.findAll().stream()
                .filter(item -> item.getIsActive() == null || Boolean.TRUE.equals(item.getIsActive()))
                .toList();
    }

    public List<EquipmentTypeMaster> getEquipmentTypeList() {
        return equipmentTypeMasterRepository.findAll().stream()
                .filter(item -> item.getIsActive() == null || Boolean.TRUE.equals(item.getIsActive()))
                .toList();
    }

    public List<EquipmentMaster> getEquipmentMaster() {
        return equipmentRepository.findAll().stream()
                .filter(item -> item.getIsActive() == null || Boolean.TRUE.equals(item.getIsActive()))
                .toList();
    }
}