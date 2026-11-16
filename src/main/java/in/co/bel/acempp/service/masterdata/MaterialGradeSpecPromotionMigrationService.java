package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One-time migration: copy unique grade-level specifications onto materials
 * that have grades but no top-level specifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialGradeSpecPromotionMigrationService {

    private final MaterialRepository materialRepository;

    public MigrationResult migrate() {
        List<String> updated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int specsCopied = 0;

        for (MaterialMaster material : materialRepository.findAll()) {
            String materialCode = material.getMaterialCode();
            if (hasTopLevelSpecs(material.getSpecifications())) {
                skipped.add(materialCode);
                continue;
            }
            List<MaterialGrade> grades = material.getGrades();
            if (grades == null || grades.isEmpty()) {
                skipped.add(materialCode);
                continue;
            }

            List<MaterialSpecification> promoted = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (MaterialGrade grade : grades) {
                if (grade == null || grade.getSpecifications() == null) {
                    continue;
                }
                for (MaterialSpecification spec : grade.getSpecifications()) {
                    if (spec == null) {
                        continue;
                    }
                    String key = uniquenessKey(spec);
                    if (key == null || !seen.add(key)) {
                        continue;
                    }
                    promoted.add(copySpec(spec));
                }
            }

            if (promoted.isEmpty()) {
                skipped.add(materialCode);
                continue;
            }

            material.setSpecifications(promoted);
            materialRepository.save(material);
            updated.add(materialCode);
            specsCopied += promoted.size();
        }

        log.info("Material grade-spec promotion complete: updated={}, skipped={}, specsCopied={}",
                updated.size(), skipped.size(), specsCopied);
        if (!updated.isEmpty()) {
            log.info("Materials updated: {}", updated);
        }
        if (!skipped.isEmpty()) {
            log.info("Materials skipped: {}", skipped);
        }
        return new MigrationResult(updated, skipped, specsCopied);
    }

    private static boolean hasTopLevelSpecs(List<MaterialSpecification> specs) {
        return specs != null && !specs.isEmpty();
    }

    private static String uniquenessKey(MaterialSpecification spec) {
        if (spec.getSpecificationCode() != null && !spec.getSpecificationCode().isBlank()) {
            return spec.getSpecificationCode().trim().toLowerCase(Locale.ROOT);
        }
        if (spec.getSpecificationName() != null && !spec.getSpecificationName().isBlank()) {
            return spec.getSpecificationName().trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static MaterialSpecification copySpec(MaterialSpecification source) {
        return MaterialSpecification.builder()
                .specificationCode(source.getSpecificationCode())
                .specificationName(source.getSpecificationName())
                .referenceRange(copyRange(source.getReferenceRange()))
                .isActive(source.getIsActive())
                .build();
    }

    private static ReferenceRange copyRange(ReferenceRange source) {
        if (source == null) {
            return null;
        }
        return ReferenceRange.builder()
                .minValue(source.getMinValue())
                .maxValue(source.getMaxValue())
                .unitId(source.getUnitId())
                .unit(source.getUnit())
                .build();
    }

    public record MigrationResult(List<String> updated, List<String> skipped, int specsCopied) {}
}
