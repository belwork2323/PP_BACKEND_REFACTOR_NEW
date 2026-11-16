package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.subscale.*;
import in.co.bel.acempp.dto.subscale.subscalecreation.HardwareArticleSelectionDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.HardwarePreparationDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.SubscaleDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.TrimmingTableDTO;
import in.co.bel.acempp.entity.manufacturing.SubscaleProcessing;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.SubscaleProcessingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SubscaleMetadataResolver {

    private final SubscaleProcessingRepository subscaleProcessingRepository;

    public SubScaleMetadata resolveApprovedMetadata(String batchId) {
        return subscaleProcessingRepository.findByBatchIdAndIsDeletedFalse(batchId)
                .filter(subscale -> subscale.getStatus() == Status.COMPLETELY_APPROVED)
                .map(this::toMetadata)
                .orElse(null);
    }

    /**
     * Subscale form metadata for any non-deleted form (not gated on COMPLETELY_APPROVED).
     * Used so QC Mixing can resolve mixingCycle as soon as it is chosen on the form.
     */
    public SubScaleMetadata resolveMetadataForBatch(String batchId) {
        return subscaleProcessingRepository.findByBatchIdAndIsDeletedFalse(batchId)
                .map(this::toMetadata)
                .orElse(null);
    }

    public Map<String, List<String>> resolveApprovedBemMotorIds(Collection<String> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> bemMotorIdsByBatch = new LinkedHashMap<>();
        subscaleProcessingRepository.findByBatchIdInAndStatusAndIsDeletedFalse(
                        batchIds,
                        Status.COMPLETELY_APPROVED)
                .forEach(subscale -> bemMotorIdsByBatch.put(
                        subscale.getBatchId(),
                        toBemMotorIds(toMetadata(subscale))));
        return bemMotorIdsByBatch;
    }

    public SubScaleMetadata buildMetadata(SubscaleProcessing subscale) {
        return toMetadata(subscale);
    }

    SubScaleMetadata toMetadata(SubscaleProcessing subscale) {
        SubScaleMetadata metadata = new SubScaleMetadata();
        if (subscale == null) {
            return metadata;
        }
        HardwarePreparationDetailsDTO hw = subscale.getHardwarePreparationDetails();

        if (hw != null) {
            metadata.setHardwarePreparationDetails(HardwarePreparationDetailsDto.builder()
                    .numberOf40KgBems(resolveBemCount(hw, hw.getNumberOf40KgBems(), "40"))
                    .numberOf10KgBems(resolveBemCount(hw, hw.getNumberOf10KgBems(), "10"))
                    .numberOf2KgBems(resolveBemCount(hw, hw.getNumberOf2KgBems(), "2"))
                    .build());
        }

        if (subscale.getTrimmingDetails() != null
                && subscale.getTrimmingDetails().getTrimmingTable() != null) {
            metadata.setBemMotors(subscale.getTrimmingDetails().getTrimmingTable().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> isBemArticleType(hw, item.getArticleType()))
                    .map(item -> BemMotorDto.builder()
                            .bemNo(resolveTrimmingBemIdentity(item))
                            .articleType(item.getArticleType())
                            .build())
                    .filter(motor -> motor.getBemNo() != null && !motor.getBemNo().isBlank())
                    .toList());
        }

        metadata.setMixingCycle(copyMixingCycle(subscale.getSubscaleDetails()));

        return metadata;
    }

    private static MixingCycleDto copyMixingCycle(SubscaleDetailsDTO details) {
        if (details == null || details.getMixingCycle() == null) {
            return null;
        }
        SubscaleDetailsDTO.MixingCycle cycle = details.getMixingCycle();
        String code = cycle.getMixingCycleCode() == null ? null : cycle.getMixingCycleCode().trim();
        String name = cycle.getMixingCycleName() == null ? null : cycle.getMixingCycleName().trim();
        if ((code == null || code.isBlank()) && (name == null || name.isBlank())
                && cycle.getMixingCycleId() == null) {
            return null;
        }
        Long cycleId = cycle.getMixingCycleId() == null
                ? null
                : cycle.getMixingCycleId().longValue();
        return MixingCycleDto.builder()
                .mixingCycleId(cycleId)
                .mixingCycleCode(code)
                .mixingCycleName(name)
                .build();
    }

    private static String resolveTrimmingBemIdentity(TrimmingTableDTO item) {
        if (item == null || item.getBemNo() == null || item.getBemNo().isBlank()) {
            return null;
        }
        return item.getBemNo().trim();
    }


    private Integer resolveBemCount(HardwarePreparationDetailsDTO hw, Integer legacyCount, String sizeHintKg) {
        if (legacyCount != null) {
            return legacyCount;
        }
        if (hw.getArticles() == null || hw.getArticles().isEmpty()) {
            return null;
        }
        int total = 0;
        boolean matched = false;
        for (HardwareArticleSelectionDTO selection : hw.getArticles()) {
            if (selection == null
                    || selection.getNoOfArticles() == null
                    || !Boolean.TRUE.equals(selection.getIsBem())) {
                continue;
            }
            String label = String.join(" ",
                    Objects.toString(selection.getSubscaleArticleName(), ""),
                    Objects.toString(selection.getSubscaleArticleCode(), ""));
            String normalized = label.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            if (sizeHintKg != null && !sizeHintKg.isBlank()
                    && !normalized.contains(sizeHintKg + "KG")
                    && !normalized.contains(sizeHintKg + "KGBEM")) {
                continue;
            }
            total += selection.getNoOfArticles();
            matched = true;
        }
        if (matched) {
            return total;
        }
        // Fallback: put total BEM article counts into 40kg slot when sizes are not in the name
        if ("40".equals(sizeHintKg)) {
            int bemTotal = hw.getArticles().stream()
                    .filter(Objects::nonNull)
                    .filter(s -> Boolean.TRUE.equals(s.getIsBem()))
                    .mapToInt(s -> s.getNoOfArticles() == null ? 0 : s.getNoOfArticles())
                    .sum();
            return bemTotal > 0 ? bemTotal : null;
        }
        return null;
    }


    public static boolean isBemArticleType(HardwarePreparationDetailsDTO hw, String articleType) {
        if (hw == null || hw.getArticles() == null || articleType == null || articleType.isBlank()) {
            return false;
        }
        String normalizedType = normalizeArticleLabel(articleType);
        if (normalizedType.isEmpty()) {
            return false;
        }
        for (HardwareArticleSelectionDTO selection : hw.getArticles()) {
            if (selection == null || !Boolean.TRUE.equals(selection.getIsBem())) {
                continue;
            }
            if (normalizedType.equalsIgnoreCase(normalizeArticleLabel(selection.getSubscaleArticleName()))
                    || normalizedType.equalsIgnoreCase(normalizeArticleLabel(selection.getSubscaleArticleCode()))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeArticleLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public List<String> toBemMotorIds(SubScaleMetadata metadata) {
        if (metadata == null || metadata.getBemMotors() == null) {
            return Collections.emptyList();
        }

        return metadata.getBemMotors().stream()
                .filter(Objects::nonNull)
                .map(BemMotorDto::getBemNo)
                .filter(Objects::nonNull)
                .filter(bemMotorId -> !bemMotorId.isBlank())
                .distinct()
                .toList();
    }
}
