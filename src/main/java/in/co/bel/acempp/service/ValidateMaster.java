package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads;
import in.co.bel.acempp.dto.mixing.MixerConfigurationDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.WeightmentPremixSheetRequest;
import in.co.bel.acempp.dto.rawmaterialpreparation.WeightmentSheetRequest;
import in.co.bel.acempp.entity.CastingStation;
import in.co.bel.acempp.entity.batch.BuildingMaster;
import in.co.bel.acempp.entity.batch.MixerMaster;
import in.co.bel.acempp.entity.batch.OvenMaster;
import in.co.bel.acempp.entity.manufacturing.CuringCycleMaster;
import in.co.bel.acempp.entity.manufacturing.MixingCycleMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BuildingMasterRepository;
import in.co.bel.acempp.repository.CastingStationRepository;
import in.co.bel.acempp.repository.MixerMasterRepository;
import in.co.bel.acempp.repository.OvenMasterRepository;
import in.co.bel.acempp.repository.manufacturing.CuringCycleMasterRepository;
import in.co.bel.acempp.repository.manufacturing.MixingCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateMaster {

    private final BuildingMasterRepository buildingMasterRepository;
    private final MixerMasterRepository mixerMasterRepository;
    private final MixingCycleRepository mixingCycleRepository;
    private final CastingStationRepository castingStationRepository;
    private final CuringCycleMasterRepository curingCycleMasterRepository;
    private final OvenMasterRepository ovenMasterRepository;

    public void validateAndResolveBuildingNumber(MixerConfigurationDto mixerConfig) {
        if (mixerConfig == null || mixerConfig.getBldgNo() == null || mixerConfig.getBldgNo().isBlank()) {
            return;
        }
        BuildingMaster building = buildingMasterRepository.findByCodeAndIsActiveTrue(mixerConfig.getBldgNo())
                .or(() -> buildingMasterRepository.findByNameAndIsActiveTrue(mixerConfig.getBldgNo()))
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                        "Invalid or inactive building number: " + mixerConfig.getBldgNo()));
        mixerConfig.setBldgNo(building.getName());
    }

    public void validateAndResolveMixer(MixerConfigurationDto mixerConfig) {
        if (mixerConfig == null || mixerConfig.getMixerId() == null || mixerConfig.getMixerId().isBlank()) {
            return;
        }
        MixerMaster mixer = mixerMasterRepository.findByCodeAndIsActiveTrue(mixerConfig.getMixerId())
                .or(() -> mixerMasterRepository.findByNameAndIsActiveTrue(mixerConfig.getMixerId()))
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                        "Invalid or inactive mixer ID/code: " + mixerConfig.getMixerId()));

        mixerConfig.setMixerId(mixer.getName());
    }

    public void validateAndResolveMixingCycle(MixingCycleDto mixingCycle) {
        if (mixingCycle == null || mixingCycle.getMixingCycleCode() == null || mixingCycle.getMixingCycleCode().isBlank()) {
            return;
        }

        MixingCycleMaster master = mixingCycleRepository.findByMixingCycleCodeAndIsActiveTrue(mixingCycle.getMixingCycleCode())
                .or(() -> mixingCycleRepository.findByMixingCycleNameAndIsActiveTrue(mixingCycle.getMixingCycleCode()))
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                        "Invalid or inactive mixing cycle code or name: " + mixingCycle.getMixingCycleCode()));

        mixingCycle.setMixingCycleId(master.getId()); // Assuming Long id in master
        mixingCycle.setMixingCycleName(master.getMixingCycleName());
    }

    public void validateAndResolveWeightmentBuilding(WeightmentSheetRequest weightmentSheet) {
        if (weightmentSheet == null) {
            return;
        }
        if (weightmentSheet.getMixerBuildingNumber() != null && !weightmentSheet.getMixerBuildingNumber().isBlank()) {
            BuildingMaster building = buildingMasterRepository.findByCodeAndIsActiveTrue(weightmentSheet.getMixerBuildingNumber())
                    .or(() -> buildingMasterRepository.findByNameAndIsActiveTrue(weightmentSheet.getMixerBuildingNumber()))
                    .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                            "Invalid or inactive building number: " + weightmentSheet.getMixerBuildingNumber()));

            weightmentSheet.setMixerBuildingNumber(building.getName());
        }

        if (weightmentSheet.getPremixSheets() != null) {
            for (WeightmentPremixSheetRequest premixSheet : weightmentSheet.getPremixSheets()) {
                if (premixSheet.getMixerBuildingNumber() != null && !premixSheet.getMixerBuildingNumber().isBlank()) {
                    BuildingMaster building = buildingMasterRepository.findByCodeAndIsActiveTrue(premixSheet.getMixerBuildingNumber())
                            .or(() -> buildingMasterRepository.findByNameAndIsActiveTrue(premixSheet.getMixerBuildingNumber()))
                            .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                                    "Invalid or inactive building number in premix sheet: " + premixSheet.getMixerBuildingNumber()));

                    premixSheet.setMixerBuildingNumber(building.getName());
                }
            }
        }
    }

    public void validateCastingStation(CastingCuringSectionPayloads.CastingSetup castingSetup) {
        if (castingSetup == null || castingSetup.getCastingStation() == null) {
            return;
        }

        CastingStation master = castingStationRepository.findByStationCodeAndStatus(castingSetup.getCastingStation(), "ACTIVE")
                .or(() -> castingStationRepository.findByStationNameAndStatus(castingSetup.getCastingStation(), "ACTIVE"))
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                        "Invalid or inactive mixing cycle code or name: " + castingSetup.getCastingStation()));

        castingSetup.setCastingStation(master.getStationName());
        castingSetup.setCastingType(master.getStationType());
    }

    public void validateCuringAndOven(
            CastingCuringSectionPayloads.CuringSetup curingSetup) {

        if (curingSetup == null
                || curingSetup.getCuringType() == null
                || curingSetup.getOven() == null
                || curingSetup.getOvenNo() == null) {
            return;
        }

        CuringCycleMaster curingMaster = curingCycleMasterRepository
                .findFirstByCuringTypeAndIsActiveTrue(curingSetup.getCuringType())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Invalid or inactive curing type: " + curingSetup.getCuringType()
                ));

        // Resolve oven from OvenMaster using code or name
        OvenMaster ovenMaster = ovenMasterRepository
                .findByCodeAndIsActiveTrue(curingSetup.getOven())
                .or(() -> ovenMasterRepository.findByNameAndIsActiveTrue(
                        curingSetup.getOven()))
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Invalid or inactive oven: " + curingSetup.getOven()
                ));

        curingSetup.setCuringType(curingMaster.getCuringType());
        curingSetup.setOven(ovenMaster.getName());
    }
}
