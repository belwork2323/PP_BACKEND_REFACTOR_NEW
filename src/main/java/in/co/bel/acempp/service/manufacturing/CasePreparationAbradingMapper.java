package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingCutRow;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingDetail;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingOperation;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwareAbrading;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwareCutRow;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class CasePreparationAbradingMapper {

    public void normalize(AbradingOperation abrading) {
        if (abrading == null || !hasLegacyDetails(abrading)) {
            return;
        }
        List<AbradingDetail> details = abrading.getAbradingDetails();
        abrading.setFirstCut(mergeCutRows(
                abrading.getFirstCut(), buildCutRows(details, true)));
        abrading.setSecondCut(mergeCutRows(
                abrading.getSecondCut(), buildCutRows(details, false)));
        abrading.setAbradingDetails(null);
    }

    public HardwareAbrading toHardwareAbrading(AbradingOperation abrading) {
        if (abrading == null) {
            return HardwareAbrading.builder()
                    .firstCut(Collections.emptyList())
                    .secondCut(Collections.emptyList())
                    .build();
        }
        normalize(abrading);
        return HardwareAbrading.builder()
                .firstCut(toHardwareCutRows(abrading.getFirstCut()))
                .secondCut(toHardwareCutRows(abrading.getSecondCut()))
                .build();
    }

    private List<HardwareCutRow> toHardwareCutRows(List<AbradingCutRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<HardwareCutRow> mapped = new ArrayList<>();
        for (AbradingCutRow row : rows) {
            if (row == null) {
                continue;
            }
            mapped.add(HardwareCutRow.builder()
                    .srNo(row.getSrNo())
                    .startDateTime(row.getStartDateTime())
                    .endDateTime(row.getEndDateTime())
                    .dustQty(row.getDustQty())
                    .observations(row.getObservations())
                    .build());
        }
        return mapped;
    }

    private List<AbradingCutRow> buildCutRows(List<AbradingDetail> details, boolean firstCut) {
        String cutToken = firstCut ? "1st" : "2nd";
        String dustToken = firstCut ? "(A)" : "(B)";

        AbradingDetail start = findDetail(details, cutToken, "Start");
        AbradingDetail end = findDetail(details, cutToken, "End");
        AbradingDetail dust = findDetailByOperationContains(details, cutToken, "Dust Weight", dustToken);

        if (start == null && end == null && dust == null) {
            return Collections.emptyList();
        }

        AbradingCutRow row = AbradingCutRow.builder()
                .srNo(1)
                .startDateTime(parseLocalDateTime(start != null ? start.getValue() : null))
                .endDateTime(parseLocalDateTime(end != null ? end.getValue() : null))
                .dustQty(dust != null ? dust.getValue() : null)
                .observations(firstNonBlank(
                        start != null ? start.getRemarksObservations() : null,
                        end != null ? end.getRemarksObservations() : null,
                        dust != null ? dust.getRemarksObservations() : null))
                .attachments(collectAttachments(start, end, dust))
                .build();
        return List.of(row);
    }

    private List<in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto> collectAttachments(
            AbradingDetail start,
            AbradingDetail end,
            AbradingDetail dust) {
        List<in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto> attachments = new ArrayList<>();
        if (start != null && start.getAttachments() != null) {
            attachments.addAll(start.getAttachments());
        }
        if (end != null && end.getAttachments() != null) {
            attachments.addAll(end.getAttachments());
        }
        if (dust != null && dust.getAttachments() != null) {
            attachments.addAll(dust.getAttachments());
        }
        return attachments.isEmpty() ? null : attachments;
    }

    private boolean hasLegacyDetails(AbradingOperation abrading) {
        return abrading.getAbradingDetails() != null && !abrading.getAbradingDetails().isEmpty();
    }

    private List<AbradingCutRow> mergeCutRows(
            List<AbradingCutRow> currentRows,
            List<AbradingCutRow> legacyRows) {
        if (legacyRows == null || legacyRows.isEmpty()) {
            return currentRows;
        }
        if (currentRows == null || currentRows.isEmpty()) {
            return legacyRows;
        }

        AbradingCutRow current = currentRows.getFirst();
        AbradingCutRow legacy = legacyRows.getFirst();
        if (current == null) {
            List<AbradingCutRow> mergedRows = new ArrayList<>(currentRows);
            mergedRows.set(0, legacy);
            return mergedRows;
        }

        AbradingCutRow merged = AbradingCutRow.builder()
                .srNo(firstNonNull(current.getSrNo(), legacy.getSrNo()))
                .startDateTime(firstNonNull(current.getStartDateTime(), legacy.getStartDateTime()))
                .endDateTime(firstNonNull(current.getEndDateTime(), legacy.getEndDateTime()))
                .dustQty(firstNonNull(current.getDustQty(), legacy.getDustQty()))
                .observations(firstNonNull(current.getObservations(), legacy.getObservations()))
                .attachments(firstNonNull(current.getAttachments(), legacy.getAttachments()))
                .build();
        List<AbradingCutRow> mergedRows = new ArrayList<>(currentRows);
        mergedRows.set(0, merged);
        return mergedRows;
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private AbradingDetail findDetail(List<AbradingDetail> details, String cutToken, String phase) {
        return details.stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getOperation() != null
                        && d.getOperation().contains(cutToken)
                        && d.getOperation().contains(phase))
                .findFirst()
                .orElse(null);
    }

    private AbradingDetail findDetailByOperationContains(
            List<AbradingDetail> details,
            String cutToken,
            String... tokens) {
        return details.stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getOperation() != null && d.getOperation().contains(cutToken))
                .filter(d -> {
                    String op = d.getOperation().toLowerCase(Locale.ROOT);
                    for (String token : tokens) {
                        if (!op.contains(token.toLowerCase(Locale.ROOT))) {
                            return false;
                        }
                    }
                    return true;
                })
                .findFirst()
                .orElse(null);
    }

    private LocalDateTime parseLocalDateTime(String value) {
        return DateTimeConversionUtil.toLocalDateTime(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
