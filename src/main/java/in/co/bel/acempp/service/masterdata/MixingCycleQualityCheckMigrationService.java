package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.entity.manufacturing.MixingCycleMaster;
import in.co.bel.acempp.entity.manufacturing.QualityCheckMaster;
import in.co.bel.acempp.repository.manufacturing.MixingCycleRepository;
import in.co.bel.acempp.repository.manufacturing.QualityCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One-time migration: copy legacy quality_checks params onto mixing_cycle_master
 * documents that share the same motorStage (PREMIX → premixQualityChecks, FINAL_MIX → finalMixQualityChecks).
 * Skips cycles that already have a non-empty list for that section.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MixingCycleQualityCheckMigrationService {

    private final QualityCheckRepository qualityCheckRepository;
    private final MixingCycleRepository mixingCycleRepository;

    public MigrationResult migrate() {
        int cyclesUpdated = 0;
        int paramsCopied = 0;
        List<String> orphans = new ArrayList<>();

        List<QualityCheckMaster> masters = qualityCheckRepository.findAll().stream()
                .filter(QualityCheckMasterDataService::isActive)
                .toList();

        for (QualityCheckMaster master : masters) {
            if (master.getMotorStage() == null || master.getMixType() == null) {
                continue;
            }
            String mixType = master.getMixType().trim().toUpperCase(Locale.ROOT);
            List<QualityCheckParams> source = master.getQualityChecks() == null
                    ? List.of()
                    : master.getQualityChecks().stream()
                    .filter(QualityCheckParameterMapper::isParameterActive)
                    .map(QualityCheckParameterMapper::toParameterDto)
                    .toList();
            if (source.isEmpty()) {
                continue;
            }

            List<MixingCycleMaster> cycles = mixingCycleRepository.findAllByMotorStage(master.getMotorStage());
            if (cycles.isEmpty()) {
                orphans.add(master.getQualityCheckCode() + " stage=" + master.getMotorStage()
                        + " mixType=" + mixType);
                continue;
            }

            for (MixingCycleMaster cycle : cycles) {
                MixingCycleMaster.CycleGroup group = cycle.getCycles();
                if (group == null) {
                    group = MixingCycleMaster.CycleGroup.builder()
                            .premixOperations(List.of())
                            .finalMixOperations(List.of())
                            .premixQualityChecks(List.of())
                            .finalMixQualityChecks(List.of())
                            .build();
                    cycle.setCycles(group);
                }
                boolean updated = false;
                if ("PREMIX".equals(mixType)) {
                    if (isEmpty(group.getPremixQualityChecks())) {
                        group.setPremixQualityChecks(copyParams(source));
                        updated = true;
                        paramsCopied += source.size();
                    }
                } else if ("FINAL_MIX".equals(mixType)) {
                    if (isEmpty(group.getFinalMixQualityChecks())) {
                        group.setFinalMixQualityChecks(copyParams(source));
                        updated = true;
                        paramsCopied += source.size();
                    }
                }
                if (updated) {
                    mixingCycleRepository.save(cycle);
                    cyclesUpdated++;
                }
            }
        }

        for (String orphan : orphans) {
            log.warn("QC master has no matching mixing cycle: {}", orphan);
        }
        log.info("MixingCycle QC migration complete: cyclesUpdated={}, paramsCopied={}, orphans={}",
                cyclesUpdated, paramsCopied, orphans.size());
        return new MigrationResult(cyclesUpdated, paramsCopied, orphans);
    }

    private static boolean isEmpty(List<QualityCheckParams> list) {
        return list == null || list.isEmpty();
    }

    private static List<QualityCheckParams> copyParams(List<QualityCheckParams> source) {
        List<QualityCheckParams> copy = new ArrayList<>(source.size());
        for (QualityCheckParams param : source) {
            copy.add(QualityCheckParameterMapper.toParameterDto(param));
        }
        return copy;
    }

    public record MigrationResult(int cyclesUpdated, int paramsCopied, List<String> orphans) {}
}
