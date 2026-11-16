package in.co.bel.acempp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import in.co.bel.acempp.dto.dispatch.DispatchDetailsResponse;
import in.co.bel.acempp.dto.dispatch.DispatchMotor;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads.DispatchDetails;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads.SafetyClearance;
import in.co.bel.acempp.dto.form.FormPdfRequest;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.service.dispatch.DispatchService;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormPdfServiceDispatchTest {

    @Test
    void rendersSafetyClearanceCertificateFileNameInsteadOfDocumentObject() throws Exception {
        DispatchService dispatchService = mock(DispatchService.class);
        FormPdfService service = new FormPdfService(
                null, null, null, null, null, null, null, null, null,
                null, null, null, dispatchService, new ObjectMapper(), null, null, null);
        DocumentDto certificate = new DocumentDto(
                "FILE_fd9bb4dc-9760-4111-9e8f-8769ea83fa7f",
                "SUCCESS",
                "certificate_STU7006 (1).pdf",
                "/uploads/certificate_STU7006.pdf",
                "application/pdf",
                true);
        DispatchDetails details = new DispatchDetails();
        details.setSafetyClearance(new SafetyClearance("YES", certificate));
        DispatchDetailsResponse response = DispatchDetailsResponse.builder()
                .batchId("BATCH-1")
                .formStatus(Status.APPROVED)
                .motors(List.of(DispatchMotor.builder()
                        .motorId("MOTOR-1")
                        .motorSubmissionStatus(Status.APPROVED)
                        .dispatchDetails(details)
                        .build()))
                .build();
        when(dispatchService.getFormDetails("form-1")).thenReturn(response);
        FormPdfRequest request = new FormPdfRequest();
        request.setFormId("form-1");
        request.setSubDepartmentId(13L);

        String base64 = service.generatePdf(request, "approver").getBase64();
        String text = new PdfTextExtractor(new PdfReader(
                Base64.getDecoder().decode(base64))).getTextFromPage(1);

        assertTrue(text.contains("certificate_STU7006 (1).pdf"), text);
        assertFalse(text.contains("DocumentDto("), text);
        assertFalse(text.contains("FILE_fd9bb4dc-9760-4111-9e8f-8769ea83fa7f"), text);
    }
}
