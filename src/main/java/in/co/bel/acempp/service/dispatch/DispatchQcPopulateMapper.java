package in.co.bel.acempp.service.dispatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads;
import in.co.bel.acempp.entity.qualitycontrol.DivisionDetail;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps approved QC Division CASTING / NDT / QC (propellant) data into Dispatch seed fields.
 */
@Component
@RequiredArgsConstructor
public class DispatchQcPopulateMapper {

    private static final String DISPATCH_TENSILE = "a) Tensile Strength";
    private static final String DISPATCH_MODULUS = "b) Modulus";
    private static final String DISPATCH_ELONGATION = "c) Elongation";
    private static final String DISPATCH_DENSITY = "d) Density";
    private static final String DISPATCH_PEEL = "a) Peel Strength";
    private static final String DISPATCH_TBS = "b) TBS";
    private static final String DISPATCH_SBS = "c) SBS";
    private static final String DISPATCH_BURN_RATE = "a) Burn Rate";

    private final ObjectMapper objectMapper;

    public Optional<DispatchPayloads.DispatchDetails> buildSeed(
            List<DivisionDetail> divisionDetails,
            String motorId) {
        if (divisionDetails == null || motorId == null || motorId.isBlank()) {
            return Optional.empty();
        }
        String targetMotorId = motorId.trim();

        LocalDateTime castingDate = resolveCastingDate(divisionDetails, targetMotorId);
        boolean ndtApproved = isMotorApproved(divisionDetails, QcDivisionCatalog.NDT, targetMotorId);
        List<DispatchPayloads.PropellantProperty> propellant =
                resolvePropellantProperties(divisionDetails, targetMotorId);

        if (castingDate == null && !ndtApproved && (propellant == null || propellant.isEmpty())) {
            return Optional.empty();
        }

        DispatchPayloads.DispatchDetails details = new DispatchPayloads.DispatchDetails();
        details.setCastingDate(castingDate);
        if (ndtApproved) {
            DispatchPayloads.ClearanceDetails ndt = new DispatchPayloads.ClearanceDetails();
            ndt.setAccorded("YES");
            details.setNdtClearance(ndt);
        }
        if (propellant != null && !propellant.isEmpty()) {
            details.setPropellantProperties(propellant);
        }
        return Optional.of(details);
    }

    private LocalDateTime resolveCastingDate(List<DivisionDetail> divisionDetails, String motorId) {
        DivisionDetail casting = findDivision(divisionDetails, QcDivisionCatalog.CASTING);
        if (casting == null) return null;
        Map<String, Object> motor = findApprovedMotorUnit(casting, "motorDetails", motorId);
        if (motor == null) return null;

        try {
            QcDivisionPayloads.CastingMotorResponse typed =
                    objectMapper.convertValue(motor, QcDivisionPayloads.CastingMotorResponse.class);
            if (typed.getCastingSections() != null
                    && typed.getCastingSections().getCastingProcess() != null) {
                LocalDate dateOfCasting = typed.getCastingSections().getCastingProcess().getDateOfCasting();
                if (dateOfCasting != null) {
                    return dateOfCasting.atStartOfDay();
                }
            }
        } catch (IllegalArgumentException ignored) {
            // fall through to map walk
        }

        Object sections = motor.get("castingSections");
        if (sections instanceof Map<?, ?> sectionsMap) {
            Object process = sectionsMap.get("castingProcess");
            if (process instanceof Map<?, ?> processMap) {
                Object date = processMap.get("dateOfCasting");
                return parseDateTime(date);
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) return dateTime;
        if (value instanceof LocalDate date) return date.atStartOfDay();
        if (value == null) return null;

        String text = String.valueOf(value).trim();
        if (text.isBlank()) return null;
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(text).toLocalDateTime();
            } catch (DateTimeParseException ignoredOffset) {
                try {
                    return LocalDate.parse(text).atStartOfDay();
                } catch (DateTimeParseException ignoredDate) {
                    return null;
                }
            }
        }
    }

    private List<DispatchPayloads.PropellantProperty> resolvePropellantProperties(
            List<DivisionDetail> divisionDetails,
            String motorId) {
        DivisionDetail qc = findDivision(divisionDetails, QcDivisionCatalog.QC);
        if (qc == null) return List.of();
        Map<String, Object> motor = findApprovedMotorUnit(qc, "motors", motorId);
        if (motor == null) return List.of();

        List<QcDivisionPayloads.PropellantProcess> processes = extractProcesses(motor);
        if (processes.isEmpty()) return List.of();

        Map<String, DispatchPayloads.PropellantProperty> byProperty = new LinkedHashMap<>();

        for (QcDivisionPayloads.PropellantProcess process : processes) {
            if (process instanceof QcDivisionPayloads.MechanicalPropertiesProcess mechanical) {
                List<QcDivisionPayloads.MechanicalSample> samples =
                        mechanical.getData() != null
                                && mechanical.getData().getMechanicalProperties() != null
                                ? mechanical.getData().getMechanicalProperties().getSamples()
                                : null;
                if (samples == null) continue;
                for (QcDivisionPayloads.MechanicalSample sample : samples) {
                    String dispatchProperty = mapMechanicalProperty(sample.getProperty());
                    if (dispatchProperty == null) continue;
                    upsertProperty(byProperty, dispatchProperty, sample.getSpecification(),
                            sample.getFmValues());
                }
            } else if (process instanceof QcDivisionPayloads.InterfacePropertiesProcess iface) {
                List<QcDivisionPayloads.InterfaceSample> samples =
                        iface.getData() != null
                                && iface.getData().getInterfaceProperties() != null
                                ? iface.getData().getInterfaceProperties().getSamples()
                                : null;
                if (samples == null) continue;
                for (QcDivisionPayloads.InterfaceSample sample : samples) {
                    String dispatchProperty = mapInterfaceProperty(sample.getProperty());
                    if (dispatchProperty == null) continue;
                    upsertProperty(byProperty, dispatchProperty, sample.getSpecification(),
                            sample.getFmValues());
                }
            } else if (process instanceof QcDivisionPayloads.BurnRateProcess burn) {
                List<QcDivisionPayloads.BurnRateRow> rows =
                        burn.getData() != null ? burn.getData().getBurnRateDetails() : null;
                if (rows == null) continue;
                // Prefer SSBR when present; otherwise first burn-rate row.
                QcDivisionPayloads.BurnRateRow chosen = null;
                for (QcDivisionPayloads.BurnRateRow row : rows) {
                    if (row == null) continue;
                    String prop = normalize(row.getProperty());
                    if (prop.contains("SSBR") || prop.contains("BURN")) {
                        if (chosen == null || prop.contains("SSBR")) {
                            chosen = row;
                            if (prop.contains("SSBR")) break;
                        }
                    }
                }
                if (chosen != null) {
                    upsertProperty(byProperty, DISPATCH_BURN_RATE, chosen.getSpecification(),
                            chosen.getFmValues());
                }
            }
        }

        List<DispatchPayloads.PropellantProperty> result = new ArrayList<>(byProperty.values());
        int sr = 1;
        for (DispatchPayloads.PropellantProperty property : result) {
            property.setSrNo(sr++);
        }
        return result;
    }

    private List<QcDivisionPayloads.PropellantProcess> extractProcesses(Map<String, Object> motor) {
        Object processesNode = motor.get("processes");
        if (processesNode == null) return List.of();
        try {
            QcDivisionPayloads.PropellantMotorResponse typed =
                    objectMapper.convertValue(motor, QcDivisionPayloads.PropellantMotorResponse.class);
            return typed.getProcesses() != null ? typed.getProcesses() : List.of();
        } catch (IllegalArgumentException ex) {
            try {
                return objectMapper.convertValue(
                        processesNode,
                        new TypeReference<List<QcDivisionPayloads.PropellantProcess>>() {});
            } catch (IllegalArgumentException ignored) {
                return List.of();
            }
        }
    }

    private void upsertProperty(
            Map<String, DispatchPayloads.PropellantProperty> byProperty,
            String property,
            String specification,
            Map<String, Object> fmValues) {
        DispatchPayloads.PropellantProperty row = byProperty.computeIfAbsent(property, key -> {
            DispatchPayloads.PropellantProperty created = new DispatchPayloads.PropellantProperty();
            created.setProperty(key);
            created.setFinalMixResults(new ArrayList<>());
            return created;
        });
        if ((row.getSpecification() == null || row.getSpecification().isBlank())
                && specification != null
                && !specification.isBlank()) {
            row.setSpecification(specification);
        }
        List<DispatchPayloads.FinalMixResult> mixes =
                row.getFinalMixResults() != null ? row.getFinalMixResults() : new ArrayList<>();
        row.setFinalMixResults(mixes);
        if (fmValues == null || fmValues.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : fmValues.entrySet()) {
            Integer mixNo = QcDivisionPayloads.parseFmColumnIndex(entry.getKey());
            if (mixNo == null) {
                continue;
            }
            putMixValue(mixes, mixNo, toBigDecimal(entry.getValue()));
        }
    }

    private void putMixValue(
            List<DispatchPayloads.FinalMixResult> mixes,
            int finalMixNo,
            BigDecimal value) {
        if (value == null) return;
        String text = value.stripTrailingZeros().toPlainString();
        for (DispatchPayloads.FinalMixResult existing : mixes) {
            if (Objects.equals(existing.getFinalMixNo(), finalMixNo)) {
                if (existing.getValue() == null || existing.getValue().isBlank()) {
                    existing.setValue(text);
                }
                return;
            }
        }
        DispatchPayloads.FinalMixResult mix = new DispatchPayloads.FinalMixResult();
        mix.setFinalMixNo(finalMixNo);
        mix.setValue(text);
        mixes.add(mix);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String mapMechanicalProperty(String raw) {
        String n = normalize(raw);
        if (n.isBlank()) return null;
        if (n.contains("TENSILE")) return DISPATCH_TENSILE;
        if (n.contains("MODULUS") || n.contains("E_MODULUS") || n.contains("EMODULUS")) {
            return DISPATCH_MODULUS;
        }
        if (n.contains("ELONGATION")) return DISPATCH_ELONGATION;
        if (n.contains("DENSITY")) return DISPATCH_DENSITY;
        return null;
    }

    private String mapInterfaceProperty(String raw) {
        String n = normalize(raw);
        if (n.isBlank()) return null;
        if (n.contains("PEEL")) return DISPATCH_PEEL;
        if (n.contains("TBS")) return DISPATCH_TBS;
        if (n.contains("SBS")) return DISPATCH_SBS;
        return null;
    }

    private boolean isMotorApproved(
            List<DivisionDetail> divisionDetails,
            String divisionName,
            String motorId) {
        DivisionDetail detail = findDivision(divisionDetails, divisionName);
        if (detail == null) return false;
        String unitsKey = QcDivisionCatalog.QC.equals(divisionName) ? "motors" : "motorDetails";
        return findApprovedMotorUnit(detail, unitsKey, motorId) != null;
    }

    private Map<String, Object> findApprovedMotorUnit(
            DivisionDetail detail,
            String unitsKey,
            String motorId) {
        if (detail == null || detail.getData() == null) return null;
        Object unitsNode = detail.getData().get(unitsKey);
        if (!(unitsNode instanceof List<?> units)) return null;
        for (Object unit : units) {
            if (!(unit instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> motor = (Map<String, Object>) rawMap;
            String id = String.valueOf(motor.getOrDefault("motorId", "")).trim();
            if (!motorId.equals(id)) continue;
            Status status = readStatus(motor.get("motorSubmissionStatus"));
            if (status == Status.APPROVED) {
                return motor;
            }
        }
        return null;
    }

    private DivisionDetail findDivision(List<DivisionDetail> details, String divisionName) {
        String target = QcDivisionCatalog.normalizeDivisionName(divisionName);
        for (DivisionDetail detail : details) {
            if (detail == null) continue;
            String name = QcDivisionCatalog.normalizeDivisionName(detail.getDivision());
            if (target != null && target.equals(name)) {
                return detail;
            }
            if (detail.getDivisionId() != null) {
                String byId = QcDivisionCatalog.canonicalName(detail.getDivisionId());
                if (target != null && target.equals(byId)) {
                    return detail;
                }
            }
        }
        return null;
    }

    private Status readStatus(Object raw) {
        if (raw instanceof Status status) return status;
        if (raw == null) return null;
        try {
            return Status.valueOf(String.valueOf(raw).trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace("(", "")
                .replace(")", "")
                .replace("%", "")
                .replace("/", "_");
    }
}
