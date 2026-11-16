package in.co.bel.acempp.service.qualitycontrol;

import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.*;
import in.co.bel.acempp.dto.casepreparation.Motor;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.*;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.service.manufacturing.CasePreparationAbradingMapper;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class QcHardwareMapper {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DateTimeConversionUtil.UI_DATE_PATTERN);

    private final CasePreparationAbradingMapper casePreparationAbradingMapper;

    public HardwareMotorInput toHardwareMotorInput(
            Motor motor,
            String buildingNo,
            String ovenNo,
            String rh) {
        if (motor == null) {
            return null;
        }
        HardwareMotorInput input = new HardwareMotorInput();
        input.setMotorId(motor.getMotorId());
        input.setMotorSubmissionType(motor.getMotorSubmissionType());
        input.setAbrading(mapAbrading(motor.getAbradingOperation()));
        input.setPreheating(mapPreheating(motor.getPreHeating(), buildingNo, ovenNo));
        input.setLinerCoating(mapLinerCoating(motor.getLinerCoatingOperation(), rh));
        input.setDispatch(mapDispatch(motor.getDispatchToCasting()));
        input.setAttachments(mapAttachments(motor));
        return input;
    }

    public HardwareMotorResponse toHardwareMotorResponse(
            Motor motor,
            Status motorSubmissionStatus,
            String buildingNo,
            String ovenNo,
            String rh) {
        HardwareMotorResponse response = new HardwareMotorResponse();
        HardwareMotorInput mapped = toHardwareMotorInput(motor, buildingNo, ovenNo, rh);
        if (mapped != null) {
            response.setMotorId(mapped.getMotorId());
            response.setMotorSubmissionType(mapped.getMotorSubmissionType());
            response.setAbrading(mapped.getAbrading());
            response.setPreheating(mapped.getPreheating());
            response.setLinerCoating(mapped.getLinerCoating());
            response.setDispatch(mapped.getDispatch());
            response.setAttachments(mapped.getAttachments());
        }
        response.setMotorSubmissionStatus(motorSubmissionStatus);
        return response;
    }

    private HardwareAbrading mapAbrading(AbradingOperation abrading) {
        return casePreparationAbradingMapper.toHardwareAbrading(abrading);
    }

    private HardwarePreheating mapPreheating(PreHeating preHeating, String buildingNo, String ovenNo) {
        if (preHeating == null) {
            return HardwarePreheating.builder().rows(Collections.emptyList()).build();
        }

        Map<String, String> monitoring = parameterMap(preHeating.getPreHeatingMonitoring());
        String date = firstNonBlank(
                parameterValue(monitoring, "Date"),
                formatLocalDate(preHeating.getPreHeatingDateTime()));
        String startTime = parameterValue(
                monitoring,
                "Oven Start Time",
                "Oven Start Date & Time",
                "Cycle Start Time",
                "Cycle Start Date & Time",
                "Start Date & Time",
                "Start Date and Time",
                "Start DateTime",
                "Start Time");
        String endTime = parameterValue(
                monitoring,
                "Cycle End Time",
                "Cycle End Date & Time",
                "End Date & Time",
                "End Date and Time",
                "End DateTime",
                "End Time");
        String temperature = lastParameterValue(preHeating.getTemperatureDuration());
        String observations = monitoring.get("Visual Observation After Pre-heating");

        HardwarePreheatingRow row = HardwarePreheatingRow.builder()
                .srNo(1)
                .date(date)
                .startDateTime(combineDateAndTime(date, startTime))
                .endDateTime(combineDateAndTime(date, endTime))
                .ovenNumber(ovenNo)
                .buildingNo(buildingNo)
                .temperature(temperature)
                .vacuumLevel(preHeating.getVacuumApplied() != null
                        ? preHeating.getVacuumApplied().stripTrailingZeros().toPlainString()
                        : null)
                .observations(observations)
                .build();

        return HardwarePreheating.builder()
                .preHeatingDateTime(preHeating.getPreHeatingDateTime())
                .rows(List.of(row))
                .build();
    }

    private HardwareLinerCoating mapLinerCoating(LinerCoatingOperation liner, String rh) {
        if (liner == null) {
            return HardwareLinerCoating.builder().rows(Collections.emptyList()).build();
        }

        Map<String, String> log = parameterMap(liner.getLinerApplicationLog());
        String effectiveRh = firstNonBlank(liner.getRh(), rh);
        String date = firstNonBlank(parameterValue(log, "Date"),
                formatLocalDate(liner.getLinerCoatingDateTime()));

        HardwareLinerRow row = HardwareLinerRow.builder()
                .srNo(1)
                .date(date)
                .startDateTime(combineDateAndTime(date, parameterValue(
                        log,
                        "Liner Start Date & Time",
                        "Start Date & Time",
                        "Start Date and Time",
                        "Start DateTime",
                        "Start Time")))
                .endDateTime(combineDateAndTime(date, parameterValue(
                        log,
                        "Liner End Date & Time",
                        "End Date & Time",
                        "End Date and Time",
                        "End DateTime",
                        "End Time")))
                .linerQty(firstNonBlank(
                        log.get("Liner Applied (w/o DCM)"),
                        log.get("Liner Applied")))
                .insulationTemp(log.get("Rocket Motor Insulation Temp"))
                .rh(effectiveRh)
                .observations(firstNonBlank(
                        log.get("Remarks"),
                        log.get("Observation")))
                .build();

        return HardwareLinerCoating.builder()
                .linerCoatingDateTime(liner.getLinerCoatingDateTime())
                .rows(List.of(row))
                .build();
    }

    private HardwareDispatch mapDispatch(DispatchToCasting dispatch) {
        if (dispatch == null) {
            return new HardwareDispatch();
        }

        Map<String, String> details = parameterMap(dispatch.getDispatchToCastingDetails());
        List<HardwareVisualObservation> visualObservations = Optional
                .ofNullable(dispatch.getDispatchVisualObservations())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(obs -> HardwareVisualObservation.builder()
                        .srNo(obs.getSrNo())
                        .parameter(obs.getParameter())
                        .observations(obs.getObservations())
                        .remarks(obs.getRemarks())
                        .build())
                .toList();

        String dispatchDateTimeValue = details.get("Dispatch Time");
        LocalDateTime dispatchDateTime = dispatchDateTimeValue == null
                || dispatchDateTimeValue.isBlank()
                ? null
                : DateTimeConversionUtil.parseLocalDateTime(dispatchDateTimeValue);

        return HardwareDispatch.builder()
                .hePunctures(details.get("Puncturing at HE (Nos)"))
                .nePunctures(details.get("Puncturing at NE (Nos)"))
                .lfPunctures(details.get("Puncturing at LF Extension (Nos)"))
                .dispatchDateTime(dispatchDateTime)
                .visualObservations(visualObservations)
                .build();
    }

    private HardwareAttachments mapAttachments(Motor motor) {
        List<DocumentDto> uploadPhoto = new ArrayList<>();
        if (motor.getAbradingOperation() != null) {
            casePreparationAbradingMapper.normalize(motor.getAbradingOperation());
            collectCutRowAttachments(motor.getAbradingOperation().getFirstCut(), uploadPhoto);
            collectCutRowAttachments(motor.getAbradingOperation().getSecondCut(), uploadPhoto);
        }

        List<DocumentDto> uploadReport = new ArrayList<>();
        if (motor.getTceCleaning() != null && motor.getTceCleaning().getTestReport() != null) {
            uploadReport.add(motor.getTceCleaning().getTestReport());
        }

        return HardwareAttachments.builder()
                .uploadReport(uploadReport)
                .uploadGraph(Collections.emptyList())
                .uploadPhoto(uploadPhoto)
                .build();
    }

    private void collectCutRowAttachments(List<AbradingCutRow> rows, List<DocumentDto> target) {
        if (rows == null) {
            return;
        }
        for (AbradingCutRow row : rows) {
            if (row != null && row.getAttachments() != null) {
                target.addAll(row.getAttachments());
            }
        }
    }

    private Map<String, String> parameterMap(List<ParameterValueRow> rows) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (ParameterValueRow row : rows) {
            if (row != null && row.getParameter() != null) {
                map.put(row.getParameter(), row.getValue());
            }
        }
        return map;
    }

    private String lastParameterValue(List<ParameterValueRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        ParameterValueRow last = rows.get(rows.size() - 1);
        return last != null ? last.getValue() : null;
    }

    private String parameterValue(Map<String, String> values, String... parameterNames) {
        for (String parameterName : parameterNames) {
            String normalizedName = normalizeParameterName(parameterName);
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (normalizeParameterName(entry.getKey()).equals(normalizedName)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String normalizeParameterName(String parameterName) {
        return parameterName == null
                ? ""
                : parameterName.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String formatLocalDate(LocalDateTime date) {
        return date != null
                ? date.toLocalDate().format(DATE_FORMATTER)
                : null;
    }

    private LocalDateTime combineDateAndTime(String date, String time) {
        if (time == null || time.isBlank()) return null;
        try {
            return DateTimeConversionUtil.parseLocalDateTime(time);
        } catch (DateTimeParseException exception) {
            if (date == null || date.isBlank()) throw exception;
            return DateTimeConversionUtil.parseLocalDateTime(
                    DateTimeConversionUtil.parseLocalDate(date) + "T" + time);
        }
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
