package in.co.bel.acempp.dto.statictestingfacility;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainMotorStaticTestingDetails extends StrictPayload implements StaticTestingDetails {

        @Valid
        @NotNull(message = "Test header is required", groups = SubmitGroup.class)
        private MainTestHeader testHeader;

        @Valid
        @NotNull(message = "Nozzle details are required", groups = SubmitGroup.class)
        private MainMotorNozzleDetails nozzleDetails;

        @Valid
        @NotNull(message = "Main motor data is required", groups = SubmitGroup.class)
        private MainMotorData mainMotorData;

        @Valid
        private List<@Valid MainBallisticParameter> ballisticParameters;

        @Valid
        @NotNull(message = "Burn rate details are required", groups = SubmitGroup.class)
        private MainBurnRate burnRate;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemStaticTestingDetails extends StrictPayload implements StaticTestingDetails {

        @Valid
        @NotNull(message = "Test header is required", groups = SubmitGroup.class)
        private BemTestHeader testHeader;

        @Valid
        @NotNull(message = "Igniter details are required", groups = SubmitGroup.class)
        private BemIgniterDetails igniterDetails;

        @Valid
        @NotNull(message = "Nozzle details are required", groups = SubmitGroup.class)
        private BemNozzleDetails nozzleDetails;

        @Valid
        @NotNull(message = "BEM data is required", groups = SubmitGroup.class)
        private BemData bemData;

        @Valid
        private List<@Valid BemBallisticParameter> ballisticParameters;

        @Valid
        @NotNull(message = "Burn rate details are required", groups = SubmitGroup.class)
        private BemBurnRate burnRate;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainTestHeader extends StrictPayload {

        @NotNull(message = "Firing date and time is required", groups = SubmitGroup.class)
        private LocalDateTime firingDateTime;

        @NotNull(message = "Ambient temperature is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal ambientTemperature;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainMotorNozzleDetails extends StrictPayload {

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal beforeDt;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal beforeDe;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal afterDt;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal afterDe;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nozzleErosionRateMmS;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainMotorData extends StrictPayload {

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal propellantWeightKg;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal avgBemBurnRateMmS;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nValue;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal propellantDensity;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal gamma;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainBallisticParameter extends StrictPayload {

        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL, groups = SubmitGroup.class)
        private String parameter;

        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL, groups = SubmitGroup.class)
        private String channel1;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainBurnRate extends StrictPayload {

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal burnRateMmS;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal pressureAbsKsc;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nValue;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemTestHeader extends StrictPayload {

        @NotBlank(groups = SubmitGroup.class)
        @Size(max = ValidationConstants.CODE_REF_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = ValidationConstants.CODE_REF_PATTERN, groups = SubmitGroup.class)
        private String loadingNo;

        @NotBlank(groups = SubmitGroup.class)
        @Size(max = ValidationConstants.CODE_REF_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = ValidationConstants.CODE_REF_PATTERN, groups = SubmitGroup.class)
        private String firingNo;

        @NotNull(groups = SubmitGroup.class)
        private LocalDateTime firingDateTime;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal temperature;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal rh;

        @NotBlank(groups = SubmitGroup.class)
        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        private String wtOfGrain;

        @NotBlank(groups = SubmitGroup.class)
        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        private String grainConfiguration;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemIgniterDetails extends StrictPayload {

        @NotBlank(groups = SubmitGroup.class)
        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        @Pattern(regexp = ValidationConstants.ALPHABET_WITH_SPECIAL, groups = SubmitGroup.class)
        private String type;

        @NotBlank(groups = SubmitGroup.class)
        @Size(max = ValidationConstants.CODE_REF_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = ValidationConstants.CODE_REF_PATTERN, groups = SubmitGroup.class)
        private String srNoAndLotNo;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal chargeMassGm;

        @NotBlank(groups = SubmitGroup.class)
        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        @Pattern(regexp = ValidationConstants.ALPHABET_WITH_SPECIAL, groups = SubmitGroup.class)
        private String composition;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemNozzleDetails extends StrictPayload {

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal beforeDt;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal beforeDe;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal afterDt;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal afterDe;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nozzleClosureThicknessMm;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemData extends StrictPayload {

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal propellantWeightKg;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal conditioningTemperature;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal nValue;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal webThicknessAverageMin;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal webThicknessStdDev;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemBallisticParameter extends StrictPayload {

        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL, groups = SubmitGroup.class)
        private String parameter;

        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL, groups = SubmitGroup.class)
        private String channel1;

        @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL, groups = SubmitGroup.class)
        private String channel2;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BemBurnRate extends StrictPayload {

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal burnRateMmS;

        @NotNull(groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal pressureAbsKsc;

        @Size(max = ValidationConstants.REMARKS_MAX, groups = SubmitGroup.class)
        @Pattern(regexp = "^$|" + ValidationConstants.REMARKS_PATTERN, groups = SubmitGroup.class)
        private String qaRemarks;
    }
}
