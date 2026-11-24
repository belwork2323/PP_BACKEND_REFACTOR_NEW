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
import in.co.bel.acempp.entity.batch.BuildingMaster;
import in.co.bel.acempp.repository.BuildingMasterRepository;
import in.co.bel.acempp.service.manufacturing.CastingCuringService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormPdfServiceCastingCuringTest {

    @Test
    void rendersCastingDateTimeLabelsWithDatePrefix() throws Exception {
        FormPdfService service = new FormPdfService(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, new ObjectMapper(), null, null, null, null, null);
        Method labelMethod = FormPdfService.class.getDeclaredMethod("ccCastingActivityLabel", String.class);
        labelMethod.setAccessible(true);

        assertTrue(labelMethod.invoke(service, "Soaking Time").equals("Soaking Date & Time"));
        assertTrue(labelMethod.invoke(service, "Time of removal from pit").equals("Date & Time of Removal from Pit"));
        assertTrue(labelMethod.invoke(service, "timeOfDispatchToCuringStation")
                .equals("Date & Time of Dispatch to Curing Station"));
    }

    @Test
    void resolvesDecoringBuildingCodeToBuildingName() throws Exception {
        AtomicReference<String> queriedCode = new AtomicReference<>();
        BuildingMasterRepository buildingRepository = (BuildingMasterRepository)
                java.lang.reflect.Proxy.newProxyInstance(
                        BuildingMasterRepository.class.getClassLoader(),
                        new Class<?>[]{BuildingMasterRepository.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("findByCode")) {
                                queriedCode.set((String) args[0]);
                                return Optional.of(BuildingMaster.builder()
                                        .code("BLD-4").name("Assembly Building").build());
                            }
                            throw new UnsupportedOperationException(method.getName());
                        });
        FormPdfService service = new FormPdfService(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, new ObjectMapper(), null, null, null, null,
                buildingRepository);
        Method resolveMethod = FormPdfService.class.getDeclaredMethod("ccBuildingName", String.class);
        resolveMethod.setAccessible(true);

        assertTrue(resolveMethod.invoke(service, "BLD-4").equals("Assembly Building"));
        assertTrue("BLD-4".equals(queriedCode.get()));
    }

    @Test
    void rendersCuringCycleDateTimesAsDayMonthYear() throws Exception {
        CastingCuringService castingCuringService = mock(CastingCuringService.class);
        FormPdfService service = new FormPdfService(
                null, null, null, null, null, castingCuringService, null, null, null,
                null, null, null, null, new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule()),
                null, null, null, null, null);
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
