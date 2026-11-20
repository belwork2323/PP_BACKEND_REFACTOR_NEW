package in.co.bel.acempp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FormPdfServiceQcHardwareAttachmentsTest {

    @Test
    void rendersMotorLevelHardwareAttachmentsAfterDispatchObservations() throws Exception {
        FormPdfService service = new FormPdfService(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, new ObjectMapper(), null, null, null, null);
        Map<String, Object> dispatch = Map.of(
                "visualObservations", List.of(Map.of(
                        "srNo", 1,
                        "parameter", "Liner Coated Rubber Surface Visual Observation",
                        "observations", "Looks good",
                        "remarks", "OK")));
        Map<String, Object> attachments = Map.of(
                "uploadReport", List.of(Map.of("fileName", "report.png")),
                "uploadGraph", List.of(),
                "uploadPhoto", List.of(Map.of("fileName", "photo.png")));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, output);
        document.open();
        Method render = FormPdfService.class.getDeclaredMethod(
                "renderQcHardwareDispatchSection", Document.class, Object.class, Object.class);
        render.setAccessible(true);
        render.invoke(service, document, dispatch, attachments);
        document.close();

        String text = new PdfTextExtractor(new PdfReader(output.toByteArray())).getTextFromPage(1);
        assertTrue(text.contains("Visual Observations"), text);
        assertTrue(text.contains("Upload Report / Graph / Photo"), text);
        assertTrue(text.contains("report.png"), text);
        assertTrue(text.contains("photo.png"), text);
        assertTrue(text.indexOf("report.png") > text.indexOf("Visual Observations"), text);
    }
}
