package in.co.bel.acempp.dto.statictestingfacility;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class StfPayloads {

    private StfPayloads() {
    }

    public interface StaticTestingDetails {
    }

    public abstract static class StrictPayload {
        @JsonAnySetter
        public void rejectUnknownField(String field, Object ignored) {
            throw new IllegalArgumentException("Unknown STF details field: " + field);
        }
    }

    /**
     * MAIN-only nested {@code staticTestingDetails}.
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainMotorStaticTestingDetails extends StrictPayload implements StaticTestingDetails {
        @Valid
        private MainTestHeader testHeader;
        @Valid
        private MainMotorNozzleDetails nozzleDetails;
        @Valid
        private MainMotorData mainMotorData;
        private List<@Valid MainBallisticParameter> ballisticParameters;
        @Valid
        private MainBurnRate burnRate;
    }

    /**
     * BEM-only nested {@code staticTestingDetails}.
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemStaticTestingDetails extends StrictPayload implements StaticTestingDetails {
        @Valid
        private BemTestHeader testHeader;
        @Valid
        private BemIgniterDetails igniterDetails;
        @Valid
        private BemNozzleDetails nozzleDetails;
        @Valid
        private BemData bemData;
        private List<@Valid BemBallisticParameter> ballisticParameters;
        @Valid
        private BemBurnRate burnRate;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainTestHeader extends StrictPayload {
        /** Local ISO datetime string (e.g. {@code 2026-10-08T14:30:00}). */
        private LocalDateTime firingDateTime;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal ambientTemperature;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainMotorNozzleDetails extends StrictPayload {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal beforeDt;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal beforeDe;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal afterDt;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal afterDe;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nozzleErosionRateMmS;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainMotorData extends StrictPayload {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal propellantWeightKg;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal avgBemBurnRateMmS;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nValue;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal propellantDensity;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal gamma;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainBallisticParameter extends StrictPayload {
        private String parameter;
        private String channel1;
        /** QA row remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainBurnRate extends StrictPayload {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal burnRateMmS;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal pressureAbsKsc;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nValue;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemTestHeader extends StrictPayload {
        private String loadingNo;
        private String firingNo;
        /** Local ISO datetime string (e.g. {@code 2026-10-08T14:30:00}). */
        private LocalDateTime firingDateTime;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal temperature;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal rh;
        private String wtOfGrain;
        private String grainConfiguration;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemIgniterDetails extends StrictPayload {
        private String type;
        private String srNoAndLotNo;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal chargeMassGm;
        private String composition;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemNozzleDetails extends StrictPayload {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal beforeDt;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal beforeDe;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal afterDt;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal afterDe;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nozzleClosureThicknessMm;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemData extends StrictPayload {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal propellantWeightKg;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal conditioningTemperature;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nValue;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal webThicknessAverageMin;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal webThicknessStdDev;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemBallisticParameter extends StrictPayload {
        private String parameter;
        private String channel1;
        private String channel2;
        /** QA row remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemBurnRate extends StrictPayload {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal burnRateMmS;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal pressureAbsKsc;
        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        private String qaRemarks;
    }
}
