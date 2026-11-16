package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.mixing.ApprovedMixMetadata;
import in.co.bel.acempp.dto.mixing.MixDetailsDto;
import in.co.bel.acempp.dto.mixing.MixerConfigurationDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.mixing.MixingMetadata;
import in.co.bel.acempp.dto.mixing.MixingStageMetadata;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormRequest;
import in.co.bel.acempp.dto.mixing.mixingform.PremixDto;
import in.co.bel.acempp.dto.mixing.mixingform.TrialDetailsDto;
import in.co.bel.acempp.entity.manufacturing.Mixing;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.MixingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MixingMetadataResolver {

    private final MixingRepository mixingRepository;

    public MixingMetadata resolveApprovedMetadata(String batchId) {
        return mixingRepository.findByBatchIdAndIsDeletedFalse(batchId)
                .map(this::toMetadata)
                .filter(metadata -> !metadata.getStages().isEmpty())
                .orElse(null);
    }

    private MixingMetadata toMetadata(Mixing mixing) {
        List<MixingStageMetadata> stages = new ArrayList<>();

        if (mixing.getMixingDetails() == null
                || mixing.getMixingDetails().getStages() == null) {
            return MixingMetadata.builder().stages(stages).build();
        }

        mixing.getMixingDetails().getStages().stream()
                .filter(Objects::nonNull)
                .filter(stage -> stage.getStageType() != null)
                .forEach(stage -> {
                    List<ApprovedMixMetadata> approvedEntries =
                            stage.getPremixes() == null
                                    ? new ArrayList<>()
                                    : stage.getPremixes().stream()
                                    .filter(Objects::nonNull)
                                    .filter(entry -> entry.getPremixSubmissionStatus()
                                            == Status.APPROVED)
                                    .map(this::toApprovedMetadata)
                                    .sorted(Comparator.comparing(
                                            ApprovedMixMetadata::getPremixNo,
                                            Comparator.nullsLast(
                                                    Comparator.naturalOrder())))
                                    .toList();

                    if (!approvedEntries.isEmpty()) {
                        stages.add(MixingStageMetadata.builder()
                                .stageType(stage.getStageType())
                                .premixes(new ArrayList<>(approvedEntries))
                                .build());
                    }
                });

        return MixingMetadata.builder().stages(stages).build();
    }

    private ApprovedMixMetadata toApprovedMetadata(PremixDto entry) {
        MixerConfigurationDto mixer = entry.getMixerConfiguration();
        TrialDetailsDto trial = entry.getTrialDetails();
        MixDetailsDto mix = entry.getMixDetails();
        MixingCycleDto cycle = entry.getMixingCycle();

        return ApprovedMixMetadata.builder()
                .premixNo(entry.getPremixNo())
                .mixerId(mixer != null ? mixer.getMixerId() : null)
                .buildingNo(mixer != null ? mixer.getBldgNo() : null)
                .bowlId(mixer != null ? mixer.getBowlId() : null)
                .trialDateTime(trial != null ? trial.getTrialDateTime() : null)
                .observations(trial != null ? trial.getObservations() : null)
                .mixDate(mix != null ? mix.getMixDate() : null)
                .mixQuantity(mix != null ? mix.getMixQuantity() : null)
                .mixingCycle(copyCycle(cycle))
                .build();
    }

    private MixingCycleDto copyCycle(MixingCycleDto cycle) {
        if (cycle == null) {
            return null;
        }
        return MixingCycleDto.builder()
                .mixingCycleId(cycle.getMixingCycleId())
                .mixingCycleCode(cycle.getMixingCycleCode())
                .mixingCycleName(cycle.getMixingCycleName())
                .build();
    }
}
