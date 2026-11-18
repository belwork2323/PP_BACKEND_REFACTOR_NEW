package in.co.bel.acempp.service.qualitycontrol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.co.bel.acempp.config.JacksonDateTimeConfig;
import in.co.bel.acempp.dto.casepreparation.CasePreparationMotorRequest;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingCutRow;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingDetail;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingOperation;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.DispatchToCasting;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.ParameterValueRow;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.LinerCoatingOperation;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.PreHeating;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwareMotorResponse;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.service.manufacturing.CasePreparationAbradingMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QcHardwareMapperTest {

    private final QcHardwareMapper mapper = new QcHardwareMapper(new CasePreparationAbradingMapper());

    @Test
    void preservesSourceDateTimesAndFormatsCombinedDispatchDateTime() {
        CasePreparationMotorRequest motor = CasePreparationMotorRequest.builder()
                .motorId("RMC-2026-27-001")
                .preHeating(PreHeating.builder()
                        .preHeatingDateTime(LocalDateTime.of(2026, 10, 31, 1, 15))
                        .preHeatingMonitoring(List.of(
                                row("Date", "31-10-2026"),
                                row("Oven Start Time", "31-10-2026 01:15"),
                                row("Cycle End Time", "31-10-2026 05:30")))
                        .build())
                .dispatchToCasting(DispatchToCasting.builder()
                        .dispatchToCastingDetails(List.of(row("Dispatch Time", "2026-10-28T05:00:00")))
                        .build())
                .build();

        HardwareMotorResponse response = mapper.toHardwareMotorResponse(
                motor, Status.DRAFT, "BLD-1", "2", null);

        assertEquals("31-10-2026", response.getPreheating().getRows().getFirst().getDate());
        assertEquals(
                LocalDateTime.of(2026, 10, 31, 1, 15),
                response.getPreheating().getRows().getFirst().getStartDateTime());
        assertEquals(
                LocalDateTime.of(2026, 10, 31, 5, 30),
                response.getPreheating().getRows().getFirst().getEndDateTime());
        assertEquals(
                LocalDateTime.of(2026, 10, 28, 5, 0),
                response.getDispatch().getDispatchDateTime());

        ObjectMapper jsonMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonNode json = jsonMapper.valueToTree(response);
        assertEquals("31-10-2026 01:15:00",
                json.path("preheating").path("preHeatingDateTime").asText());
        assertEquals("28-10-2026 05:00:00",
                json.path("dispatch").path("dispatchDateTime").asText());
        JsonNode row = json.path("preheating").path("rows").get(0);
        assertEquals("31-10-2026", row.path("date").asText());
        assertEquals("31-10-2026 01:15:00", row.path("startDateTime").asText());
        assertEquals("31-10-2026 05:30:00", row.path("endDateTime").asText());
    }

    @Test
    void mapsLegacySourceDateTimeNamesAndDateTimeParameterLabels() throws Exception {
        ObjectMapper jsonMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        CasePreparationMotorRequest motor = jsonMapper.readValue("""
                {
                  "motorId": "RMC-2026-27-001",
                  "abradingOperation": {
                    "firstCut": [{
                      "startTime": "10-11-2026 03:15:00",
                      "endTime": "10-11-2026 03:45:00"
                    }]
                  },
                  "preHeating": {
                    "preHeatingMonitoring": [
                      {"parameter": "Date", "value": "10-11-2026"},
                      {"parameter": "Oven Start Date & Time", "value": "10-11-2026 04:00:00"},
                      {"parameter": "Cycle End Date & Time", "value": "10-11-2026 05:00:00"}
                    ]
                  },
                  "linerCoatingOperation": {
                    "linerApplicationLog": [
                      {"parameter": "Date", "value": "10-11-2026"},
                      {"parameter": "Start Date & Time", "value": "10-11-2026 06:00:00"},
                      {"parameter": "End Date & Time", "value": "10-11-2026 07:00:00"}
                    ]
                  }
                }
                """, CasePreparationMotorRequest.class);

        HardwareMotorResponse response = mapper.toHardwareMotorResponse(
                motor, Status.DRAFT, null, null, null);

        assertEquals(LocalDateTime.of(2026, 11, 10, 3, 15),
                response.getAbrading().getFirstCut().getFirst().getStartDateTime());
        assertEquals(LocalDateTime.of(2026, 11, 10, 3, 45),
                response.getAbrading().getFirstCut().getFirst().getEndDateTime());
        assertEquals(LocalDateTime.of(2026, 11, 10, 4, 0),
                response.getPreheating().getRows().getFirst().getStartDateTime());
        assertEquals(LocalDateTime.of(2026, 11, 10, 5, 0),
                response.getPreheating().getRows().getFirst().getEndDateTime());
        assertEquals(LocalDateTime.of(2026, 11, 10, 6, 0),
                response.getLinerCoating().getRows().getFirst().getStartDateTime());
        assertEquals(LocalDateTime.of(2026, 11, 10, 7, 0),
                response.getLinerCoating().getRows().getFirst().getEndDateTime());
    }

    @Test
    void fillsMissingAbradingDateTimesFromLegacyDetailsWithoutReplacingCutRowData() {
        CasePreparationMotorRequest motor = CasePreparationMotorRequest.builder()
                .abradingOperation(AbradingOperation.builder()
                        .firstCut(List.of(AbradingCutRow.builder()
                                .srNo(1)
                                .dustQty("4")
                                .observations("retained")
                                .build()))
                        .abradingDetails(List.of(
                                abradingDetail("1st Cut Start Date & Time", "10-11-2026 03:15:00"),
                                abradingDetail("1st Cut End Date & Time", "10-11-2026 03:45:00")))
                        .build())
                .build();

        HardwareMotorResponse response = mapper.toHardwareMotorResponse(
                motor, Status.DRAFT, null, null, null);
        var cut = response.getAbrading().getFirstCut().getFirst();

        assertEquals(LocalDateTime.of(2026, 11, 10, 3, 15), cut.getStartDateTime());
        assertEquals(LocalDateTime.of(2026, 11, 10, 3, 45), cut.getEndDateTime());
        assertEquals("4", cut.getDustQty());
        assertEquals("retained", cut.getObservations());
    }

    @Test
    void fallbackHardwareDateDoesNotCombineDateWithTime() {
        CasePreparationMotorRequest motor = CasePreparationMotorRequest.builder()
                .preHeating(PreHeating.builder()
                        .preHeatingDateTime(LocalDateTime.of(2026, 10, 31, 1, 15))
                        .build())
                .build();

        HardwareMotorResponse response = mapper.toHardwareMotorResponse(
                motor, Status.DRAFT, null, null, null);

        assertEquals("31-10-2026", response.getPreheating().getRows().getFirst().getDate());
    }

    @Test
    void serializesCasePreparationDateAndDateTimeFieldsWithExplicitNames() {
        CasePreparationMotorRequest motor = CasePreparationMotorRequest.builder()
                .prrcClearanceDate(LocalDate.of(2026, 10, 31))
                .preHeating(PreHeating.builder()
                        .preHeatingDateTime(LocalDateTime.of(2026, 10, 31, 1, 15))
                        .build())
                .linerCoatingOperation(LinerCoatingOperation.builder()
                        .linerCoatingDateTime(LocalDateTime.of(2026, 10, 31, 2, 30))
                        .build())
                .abradingOperation(AbradingOperation.builder()
                        .firstCut(List.of(AbradingCutRow.builder()
                                .startDateTime(LocalDateTime.of(2026, 10, 31, 3, 10))
                                .endDateTime(LocalDateTime.of(2026, 10, 31, 3, 45))
                                .build()))
                        .build())
                .build();
        ObjectMapper jsonMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JsonNode json = jsonMapper.valueToTree(motor);

        assertEquals("31-10-2026", json.path("prrcClearanceDate").asText());
        assertEquals("31-10-2026 01:15:00", json.path("preHeating")
                .path("preHeatingDateTime").asText());
        assertEquals("31-10-2026 02:30:00", json.path("linerCoatingOperation")
                .path("linerCoatingDateTime").asText());
        JsonNode abradingRow = json.path("abradingOperation").path("firstCut").get(0);
        assertEquals("31-10-2026 03:10:00", abradingRow.path("startDateTime").asText());
        assertEquals("31-10-2026 03:45:00", abradingRow.path("endDateTime").asText());
        assertTrue(abradingRow.path("date").isMissingNode());
        assertTrue(abradingRow.path("startTime").isMissingNode());
    }

    private ParameterValueRow row(String parameter, String value) {
        return ParameterValueRow.builder().parameter(parameter).value(value).build();
    }

    private AbradingDetail abradingDetail(String operation, String value) {
        return AbradingDetail.builder().operation(operation).value(value).build();
    }
}
