package in.co.bel.acempp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.co.bel.acempp.config.JacksonDateTimeConfig;
import in.co.bel.acempp.dto.castingcuring.CastingCuringFormResponse;
import in.co.bel.acempp.dto.castingcuring.CastingCuringMotor;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.CuringCycle;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.CuringCycles;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.CuringSections;
import in.co.bel.acempp.dto.form.FormPdfRequest;
import in.co.bel.acempp.service.manufacturing.CastingCuringService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormPdfServiceCastingCuringTest {

    @Test
    void rendersCuringCycleDateTimesAsDayMonthYear() throws Exception {
        CastingCuringService castingCuringService = mock(CastingCuringService.class);
        FormPdfService service = new FormPdfService(
                null, null, null, null, null, castingCuringService, null, null, null,
                null, null, null, null, new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule()),
                null, null, null, null);
        CuringCycle cycle = CuringCycle.builder()
                .srNo(1)
                .temperature(BigDecimal.valueOf(80))
                .time(90)
                .startDateTime(LocalDateTime.of(2026, 11, 3, 9, 15))
                .endDateTime(LocalDateTime.of(2026, 11, 4, 10, 30))
                .hotWaterStatus("ON")
                .build();
        CastingCuringFormResponse details = CastingCuringFormResponse.builder()
                .batchId("BATCH-1")
                .motors(List.of(CastingCuringMotor.builder()
                        .motorId("MOTOR-1")
                        .curingSections(CuringSections.builder()
                                .curingCycles(CuringCycles.builder()
                                        .curingTable(List.of(cycle))
                                        .build())
                                .build())
                        .build()))
                .build();
        when(castingCuringService.getFormDetails("form-1")).thenReturn(details);
        FormPdfRequest request = new FormPdfRequest();
        request.setFormId("form-1");
        request.setSubDepartmentId(6L);

        String base64 = service.generatePdf(request, "approver").getBase64();
        String text = new PdfTextExtractor(new PdfReader(
                Base64.getDecoder().decode(base64))).getTextFromPage(1);

        assertTrue(text.contains("03-11-2026 09:15"), text);
        assertTrue(text.contains("04-11-2026 10:30"), text);
        assertFalse(text.contains("2026-11-03"), text);
        assertFalse(text.contains("2026-11-04"), text);
    }
}
