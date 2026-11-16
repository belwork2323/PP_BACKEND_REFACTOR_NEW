package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckRequest;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckResponse;
import in.co.bel.acempp.entity.manufacturing.MixingCycleMaster;
import in.co.bel.acempp.entity.manufacturing.QualityCheckMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.manufacturing.MixingCycleRepository;
import in.co.bel.acempp.repository.manufacturing.QualityCheckRepository;
import in.co.bel.acempp.service.masterdata.MixingCycleMasterDataService;
import in.co.bel.acempp.service.masterdata.QualityCheckMasterDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Ops lookup for mixing quality checks.
 * Prefers QC nested on Mixing Cycle; falls back to legacy quality_checks collection.
 */
@Service
@RequiredArgsConstructor
public class QualityCheckService {
    private final QualityCheckRepository repository;
    private final MixingCycleRepository mixingCycleRepository;

    public QualityCheckResponse getQualityChecks(QualityCheckRequest request) {
        String mixType = request.getMixType() == null
                ? ""
                : request.getMixType().trim().toUpperCase(Locale.ROOT);
        Integer motorStage = request.getMotorStage();
        String mixingCycleCode = request.getMixingCycleCode() == null
                ? ""
                : request.getMixingCycleCode().trim();

        List<QualityCheckParams> fromCycle =
                resolveFromMixingCycles(mixType, motorStage, mixingCycleCode);
        if (!fromCycle.isEmpty()) {
            return QualityCheckResponse.builder()
                    .mixType(mixType)
                    .qualityChecks(fromCycle)
                    .build();
        }

        QualityCheckMaster master = repository.findByMixTypeAndMotorStage(mixType, motorStage)
                .orElseThrow(() ->
                        new AcemException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Quality Check Master not found for mixType : " + mixType));
        if (!QualityCheckMasterDataService.isActive(master)) {
            throw new AcemException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Quality Check Master not found for mixType : " + mixType);
        }

        return QualityCheckResponse.builder()
                .mixType(master.getMixType())
                .qualityChecks(QualityCheckMasterDataService.activeParameters(master))
                .build();
    }

    private List<QualityCheckParams> resolveFromMixingCycles(
            String mixType, Integer motorStage, String mixingCycleCode) {
        if (mixingCycleCode != null && !mixingCycleCode.isBlank()) {
            return mixingCycleRepository.findByMixingCycleCode(mixingCycleCode)
                    .filter(MixingCycleMasterDataService::isActive)
                    .map(cycle -> qualityChecksForMixType(mixType, cycle))
                    .orElse(List.of());
        }
        if (motorStage == null) {
            return List.of();
        }
        List<MixingCycleMaster> cycles = mixingCycleRepository.findAllByMotorStage(motorStage).stream()
                .filter(MixingCycleMasterDataService::isActive)
                .toList();
        // Ambiguous when N actives share a stage — do not pick arbitrarily.
        if (cycles.size() != 1) {
            return List.of();
        }
        return qualityChecksForMixType(mixType, cycles.get(0));
    }

    private static List<QualityCheckParams> qualityChecksForMixType(
            String mixType, MixingCycleMaster cycle) {
        List<QualityCheckParams> params = "FINAL_MIX".equals(mixType)
                ? MixingCycleMasterDataService.activeFinalMixQualityChecks(cycle)
                : MixingCycleMasterDataService.activePremixQualityChecks(cycle);
        return params == null ? List.of() : params;
    }
}
