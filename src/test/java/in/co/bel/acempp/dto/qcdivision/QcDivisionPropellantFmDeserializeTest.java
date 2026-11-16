package in.co.bel.acempp.dto.qcdivision;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.co.bel.acempp.config.JacksonDateTimeConfig;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads;
import in.co.bel.acempp.entity.qualitycontrol.DivisionDetail;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.service.dispatch.DispatchQcPopulateMapper;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionCatalog;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QcDivisionPropellantFmDeserializeTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JacksonDateTimeConfig().flexibleDateInputModule())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void mechanicalSampleKeepsFm1ThroughFm3() throws Exception {
        String json = """
                {
                  "PROPERTY": "Tensile Strength (kgf/cm²)",
                  "SAMPLE_NO": 1,
                  "SPECIFICATION": ">= 10",
                  "FM_1": 11.5,
                  "FM_2": 12.0,
                  "FM_3": 13.25,
                  "AVG": 12.25,
                  "STD_DEV": 0.9
                }
                """;

        QcDivisionPayloads.MechanicalSample sample =
                objectMapper.readerFor(QcDivisionPayloads.MechanicalSample.class)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .readValue(json);

        assertEquals("Tensile Strength (kgf/cm²)", sample.getProperty());
        assertEquals(1, sample.getSampleNo());
        assertEquals(new BigDecimal("11.5"), sample.getFmValues().get("FM_1"));
        assertEquals(new BigDecimal("12.0"), sample.getFmValues().get("FM_2"));
        assertEquals(new BigDecimal("13.25"), sample.getFmValues().get("FM_3"));

        Map<?, ?> roundTrip = objectMapper.convertValue(sample, Map.class);
        assertEquals(11.5, ((Number) roundTrip.get("FM_1")).doubleValue(), 0.0001);
        assertEquals(12.0, ((Number) roundTrip.get("FM_2")).doubleValue(), 0.0001);
        assertEquals(13.25, ((Number) roundTrip.get("FM_3")).doubleValue(), 0.0001);
    }

    @Test
    void burnRateRowKeepsFm1ThroughFm3() throws Exception {
        String json = """
                {
                  "PROPERTY": "SSBR",
                  "SPECIFICATION": "0.1-0.2",
                  "FM_1": 0.11,
                  "FM_2": 0.12,
                  "FM_3": 0.13,
                  "AVG": 0.12,
                  "STD_DEV": 0.01
                }
                """;

        QcDivisionPayloads.BurnRateRow row =
                objectMapper.readerFor(QcDivisionPayloads.BurnRateRow.class)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .readValue(json);

        assertEquals("SSBR", row.getProperty());
        assertEquals(new BigDecimal("0.11"), row.getFmValues().get("FM_1"));
        assertEquals(new BigDecimal("0.12"), row.getFmValues().get("FM_2"));
        assertEquals(new BigDecimal("0.13"), row.getFmValues().get("FM_3"));
    }

    @Test
    void dispatchPopulateMapsAllFmColumns() {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("PROPERTY", "Tensile Strength (kgf/cm²)");
        sample.put("SPECIFICATION", ">= 10");
        sample.put("FM_1", 10);
        sample.put("FM_2", 11);
        sample.put("FM_3", 12);

        Map<String, Object> mechanicalProperties = Map.of("samples", List.of(sample));
        Map<String, Object> processData = Map.of("mechanicalProperties", mechanicalProperties);
        Map<String, Object> process = Map.of(
                "processType", "MECHANICAL_PROPERTIES",
                "data", processData);
        Map<String, Object> motor = new LinkedHashMap<>();
        motor.put("motorId", "M-1");
        motor.put("motorSubmissionStatus", Status.APPROVED.name());
        motor.put("processes", List.of(process));

        DivisionDetail qc = new DivisionDetail();
        qc.setDivision(QcDivisionCatalog.QC);
        qc.setDivisionSubmissionStatus(Status.APPROVED);
        qc.setData(Map.of("motors", List.of(motor)));

        DispatchQcPopulateMapper mapper = new DispatchQcPopulateMapper(objectMapper);
        DispatchPayloads.DispatchDetails seed = mapper.buildSeed(List.of(qc), "M-1").orElse(null);
        assertNotNull(seed);
        assertNotNull(seed.getPropellantProperties());
        assertTrue(seed.getPropellantProperties().stream()
                .anyMatch(row -> "a) Tensile Strength".equals(row.getProperty())));

        DispatchPayloads.PropellantProperty tensile = seed.getPropellantProperties().stream()
                .filter(row -> "a) Tensile Strength".equals(row.getProperty()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, tensile.getFinalMixResults().size());
        assertEquals("10", tensile.getFinalMixResults().get(0).getValue());
        assertEquals(1, tensile.getFinalMixResults().get(0).getFinalMixNo());
        assertEquals("11", tensile.getFinalMixResults().get(1).getValue());
        assertEquals(2, tensile.getFinalMixResults().get(1).getFinalMixNo());
        assertEquals("12", tensile.getFinalMixResults().get(2).getValue());
        assertEquals(3, tensile.getFinalMixResults().get(2).getFinalMixNo());
    }
}
