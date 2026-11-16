package in.co.bel.acempp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.co.bel.acempp.dto.trimming.TrimmingDetailsResponse;
import in.co.bel.acempp.dto.trimming.trimmingcreation.MotorDTO;
import in.co.bel.acempp.dto.trimming.trimmingcreation.TrimmingDetailsSectionDTO;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonDateTimeConfigTest {

    @Data
    static class Details {
        private String startDatetime = "2026-10-29T01:00:00";
        private String observation = "Normal";
        private String date = "2026-10-29";
        private LocalDateTime approvedAt = LocalDateTime.of(2026, 10, 29, 1, 0);
        private LocalDate localDate = LocalDate.of(2026, 10, 29);
    }

    @Test
    void serializesDateTypesInUiFormatAndStillAcceptsExistingInputFormats() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule());
        LocalDateTime value = LocalDateTime.of(2026, 10, 29, 1, 0);

        assertEquals("\"29-10-2026 01:00:00\"", mapper.writeValueAsString(value));
        assertEquals(value, mapper.readValue("\"2026-10-29T01:00:00\"", LocalDateTime.class));
        assertEquals(
                LocalDate.of(2026, 10, 29),
                mapper.readValue("\"29-10-2026\"", LocalDate.class));
        assertEquals(
                value,
                mapper.readValue("\"29-10-2026 01:00:00\"", LocalDateTime.class));
        assertEquals(
                "29-10-2026 01:00:00",
                in.co.bel.acempp.util.DateTimeConversionUtil.formatLocalDateTimeString(
                        "2026-10-29T01:00:00"));
        assertEquals("\"29-10-2026\"", mapper.writeValueAsString(LocalDate.of(2026, 10, 29)));
    }

    @Test
    void serializesIsoDatetimeStringsInDetailsWithoutChangingDateOrText() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule());

        assertEquals(
                "{\"startDatetime\":\"29-10-2026 01:00:00\","
                        + "\"observation\":\"Normal\","
                        + "\"date\":\"2026-10-29\","
                        + "\"approvedAt\":\"29-10-2026 01:00:00\","
                        + "\"localDate\":\"29-10-2026\"}",
                mapper.writeValueAsString(new Details()));
    }

    @Test
    void trimmingDateFieldsUseLocalDateTimeAndRenamedResponseProperty() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule());

        MotorDTO motor = mapper.readValue(
                "{\"motorReceivedAt\":\"13-11-2026 10:08\","
                        + "\"sections\":[{\"sectionId\":\"TRIMMING_DETAILS\","
                        + "\"sectionData\":[{\"startDatetime\":\"13-11-2026 09:00\","
                        + "\"completionDatetime\":\"13-11-2026 10:08\"}]}]}",
                MotorDTO.class);
        TrimmingDetailsSectionDTO.TrimmingDetailData detail =
                ((TrimmingDetailsSectionDTO) motor.getSections().get(0)).getSectionData().get(0);
        LocalDateTime receivedAt = LocalDateTime.of(2026, 11, 13, 10, 8);

        assertEquals(receivedAt, motor.getMotorReceivedDateTime());
        assertEquals(LocalDateTime.of(2026, 11, 13, 9, 0), detail.getStartDatetime());
        assertEquals(receivedAt, detail.getCompletionDatetime());
        var motorJson = mapper.readTree(mapper.writeValueAsString(motor));
        assertEquals("13-11-2026 10:08:00", motorJson.path("motorReceivedDateTime").asText());
        org.junit.jupiter.api.Assertions.assertFalse(motorJson.has("motorReceivedAt"));
        var detailJson = motorJson.path("sections").get(0).path("sectionData").get(0);
        assertEquals("13-11-2026 09:00:00", detailJson.path("startDatetime").asText());
        assertEquals("13-11-2026 10:08:00", detailJson.path("completionDatetime").asText());

        TrimmingDetailsSectionDTO.TrimmingDetailData legacyDetail = mapper.readValue(
                "{\"startDate\":\"13-11-2026 09:00\","
                        + "\"completionDate\":\"13-11-2026 10:08\"}",
                TrimmingDetailsSectionDTO.TrimmingDetailData.class);
        assertEquals(LocalDateTime.of(2026, 11, 13, 9, 0), legacyDetail.getStartDatetime());
        assertEquals(receivedAt, legacyDetail.getCompletionDatetime());

        TrimmingDetailsSectionDTO.TrimmingDetailData uiDetail = mapper.readValue(
                "{\"startdatetime\":\"13-11-2026 09:00\","
                        + "\"completionDatetime\":\"13-11-2026 10:08\"}",
                TrimmingDetailsSectionDTO.TrimmingDetailData.class);
        assertEquals(LocalDateTime.of(2026, 11, 13, 9, 0), uiDetail.getStartDatetime());
        assertEquals(receivedAt, uiDetail.getCompletionDatetime());

        TrimmingDetailsResponse response = mapper.readValue(
                "{\"motorReceivedDateTime\":\"13-11-2026 10:08\"}",
                TrimmingDetailsResponse.class);
        assertEquals(receivedAt, response.getMotorReceivedDateTime());
        String responseJson = mapper.writeValueAsString(response);
        assertEquals("13-11-2026 10:08:00",
                mapper.readTree(responseJson).path("motorReceivedDateTime").asText());
    }
}
