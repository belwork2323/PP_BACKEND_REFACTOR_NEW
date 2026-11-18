package in.co.bel.acempp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import in.co.bel.acempp.dto.casepreparation.CasePreparationDetailsRequest;
import in.co.bel.acempp.dto.casepreparation.CasePreparationMotorRequest;
import in.co.bel.acempp.dto.casepreparation.CasePreparationResponse;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingCutRow;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingOperation;
import in.co.bel.acempp.dto.form.FormPdfRequest;
import in.co.bel.acempp.dto.mixing.MixingStageDto;
import in.co.bel.acempp.dto.mixing.MixDetailsDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingDetailsDto;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormRequest;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormResponse;
import in.co.bel.acempp.dto.mixing.mixingform.PremixDto;
import in.co.bel.acempp.dto.qcdivision.DivisionDetailDto;
import in.co.bel.acempp.dto.qcdivision.QcDivisionFormResponse;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwareDataResponse;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwareDispatch;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwareMotorResponse;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwarePreheating;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwarePreheatingRow;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.HardwareVisualObservation;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.config.JacksonDateTimeConfig;
import in.co.bel.acempp.service.manufacturing.CasePreparationService;
import in.co.bel.acempp.service.manufacturing.MixingService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FormPdfServiceQcHardwareTest {

    @Test
    void rendersFlatHardwareDetailsAndApprovedMotorStatus() throws Exception {
        FormPdfService service = new FormPdfService(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null,
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule()),
                null, null, null);
        HardwareMotorResponse motor = new HardwareMotorResponse();
        motor.setMotorId("RMC-2026-27-001");
        motor.setMotorSubmissionStatus(Status.APPROVED);
        motor.setPreheating(HardwarePreheating.builder()
                .preHeatingDateTime(LocalDateTime.of(2026, 10, 31, 1, 15))
                .rows(List.of(HardwarePreheatingRow.builder()
                        .date("31-10-2026")
                        .startDateTime(LocalDateTime.of(2026, 10, 31, 1, 15))
                        .endDateTime(LocalDateTime.of(2026, 10, 31, 5, 30))
                        .ovenNumber("2")
                        .temperature("76")
                        .build()))
                .build());
        motor.setDispatch(HardwareDispatch.builder()
                .hePunctures("12")
                .dispatchDateTime(LocalDateTime.of(2026, 10, 28, 5, 0))
                .visualObservations(List.of(HardwareVisualObservation.builder()
                        .srNo(1)
                        .parameter("Liner Coated Rubber Surface")
                        .observations("Pass")
                        .build()))
                .build());

        HardwareDataResponse hardware = new HardwareDataResponse();
        hardware.setMotorDetails(List.of(motor));
        DivisionDetailDto detail = DivisionDetailDto.builder()
                .division("HARDWARE")
                .status(Status.APPROVED)
                .data(hardware)
                .build();
        QcDivisionFormResponse response = QcDivisionFormResponse.builder()
                .batchId("ACEM/2610/0034")
                .subDepartmentId(11L)
                .divisionDetails(List.of(detail))
                .build();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, output);
        document.open();
        service.renderQcDivisionDetails(document, response);
        document.close();

        String text = new PdfTextExtractor(new PdfReader(output.toByteArray())).getTextFromPage(1);
        assertTrue(text.contains("RMC-2026-27-001 — Approved"));
        assertTrue(text.contains("Preheating"));
        assertTrue(text.contains("31-10-2026"), text);
        assertTrue(text.contains("01:15:00"), text);
        assertTrue(text.contains("01:15"));
        assertTrue(text.contains("05:30"));
        assertTrue(text.contains("28-10-2026 05:00:00"));
        assertTrue(text.contains("Liner Coated Rubber Surface"));
        assertTrue(text.contains("Pass"));
    }

    @Test
    void rendersCasePreparationCutDateTimesTogetherAndEmptyAttachmentsClearly() throws Exception {
        CasePreparationService casePreparationService = mock(CasePreparationService.class);
        FormPdfService service = new FormPdfService(
                null, null, null, casePreparationService, null, null, null, null, null,
                null, null, null, null, new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule()),
                null, null, null);
        CasePreparationMotorRequest motor = CasePreparationMotorRequest.builder()
                .motorId("RMC-2026-27-001")
                .abradingOperation(AbradingOperation.builder()
                        .firstCut(List.of(AbradingCutRow.builder()
                                .srNo(1)
                                .startDateTime(LocalDateTime.of(2026, 10, 31, 2, 10))
                                .endDateTime(LocalDateTime.of(2026, 10, 31, 3, 25))
                                .dustQty("5")
                                .observations("tghty")
                                .attachments(List.of())
                                .build()))
                        .build())
                .build();
        CasePreparationResponse details = CasePreparationResponse.builder()
                .batchId("ACEM/2610/0034")
                .casePreparationDetails(CasePreparationDetailsRequest.builder()
                        .motors(List.of(motor))
                        .build())
                .build();
        when(casePreparationService.getFormDetails("form-1")).thenReturn(details);
        FormPdfRequest request = new FormPdfRequest();
        request.setFormId("form-1");
        request.setSubDepartmentId(4L);

        String base64 = service.generatePdf(request, "approver").getBase64();
        String text = new PdfTextExtractor(new PdfReader(
                Base64.getDecoder().decode(base64))).getTextFromPage(1);
        String normalizedText = text.replaceAll("\\s+", " ");

        assertTrue(normalizedText.contains("Start Date & Time"));
        assertTrue(normalizedText.contains("31-10-2026 02:10"), text);
        assertTrue(normalizedText.contains("End Date & Time"));
        assertTrue(normalizedText.contains("31-10-2026 03:25"), text);
        assertTrue(normalizedText.contains("No photos/videos uploaded"), text);
        assertTrue(!normalizedText.contains("[]"), text);
    }

    @Test
    void rendersMixingCycleCodeWhenCycleNameIsMissing() throws Exception {
        MixingService mixingService = mock(MixingService.class);
        FormPdfService service = new FormPdfService(
                null, null, null, null, mixingService, null, null, null, null,
                null, null, null, null, new ObjectMapper(), null, null, null);
        PremixDto premix = PremixDto.builder()
                .premixNo(1)
                .mixDetails(mixDetails(LocalDate.of(2026, 11, 3)))
                .mixingCycle(MixingCycleDto.builder()
                        .mixingCycleCode("MC-18")
                        .build())
                .build();
        MixingFormResponse details = MixingFormResponse.builder()
                .batchId("ACEM/2610/0034")
                .mixingDetails(MixingDetailsDto.builder()
                        .stages(List.of(MixingStageDto.builder()
                                .stageType(StageType.PREMIX)
                                .premixes(List.of(premix))
                                .build()))
                        .build())
                .build();
        when(mixingService.getFormDetails(any(MixingFormRequest.class))).thenReturn(details);
        FormPdfRequest request = new FormPdfRequest();
        request.setFormId("form-2");
        request.setSubDepartmentId(5L);

        String base64 = service.generatePdf(request, "approver").getBase64();
        String text = new PdfTextExtractor(new PdfReader(
                Base64.getDecoder().decode(base64))).getTextFromPage(1);

        assertTrue(text.contains("MC-18"), text);
        assertTrue(text.contains("03-11-2026"), text);
        assertFalse(text.contains("2026-11-03"), text);
    }

    private MixDetailsDto mixDetails(LocalDate mixDate) {
        MixDetailsDto details = new MixDetailsDto();
        details.setMixDate(mixDate);
        details.setMixQuantity(BigDecimal.TEN);
        return details;
    }
}
