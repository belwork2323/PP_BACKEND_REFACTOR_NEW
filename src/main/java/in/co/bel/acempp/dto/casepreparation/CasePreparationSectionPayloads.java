package in.co.bel.acempp.dto.casepreparation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.IntegerValue;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.enums.CasingType;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import in.co.bel.acempp.validation.groups.DraftGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CasePreparationSectionPayloads {

    private CasePreparationSectionPayloads() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbradingOperation {
        @NotNull(message = "Type of Casing is required", groups = {SubmitGroup.class})
        private CasingType typeOfCasing;

        @NotBlank(message = "Type of Insulation is required", groups = {SubmitGroup.class})
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String typeOfInsulation;

        @NotBlank(message = "Abrading Wheel Type is required", groups = {SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String abradingWheelType;

        private List<@Valid AbradingCutRow> firstCut;
        private List<@Valid AbradingCutRow> secondCut;
        @JsonProperty(value = "abradingDetails", access = Access.WRITE_ONLY)
        private List<AbradingDetail> abradingDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbradingCutRow {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @JsonAlias({"startTime", "startDatetime", "fromTime"})
        @Field("startTime")
        private LocalDateTime startDateTime;
        @JsonAlias({"endTime", "endDatetime", "toTime"})
        @Field("endTime")
        private LocalDateTime endDateTime;
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String dustQty;

        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX)
        private String observations;
        private List<@Valid DocumentDto> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbradingDetail {
        @JsonProperty("SR_NO")
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String operation;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String value;

        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX)
        private String remarksObservations;
        @NotNull(message = "Attachments list is required", groups = {SubmitGroup.class, DraftGroup.class})
        @NotEmpty(message = "At least one attachment must be provided", groups = {SubmitGroup.class, DraftGroup.class})
        @Valid
        private List<DocumentDto> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BellowBonding {
        @Size(max = ValidationConstants.MAX_LONG_TEXT)
        private String adhesiveDetails;

        @NotBlank(message = "HE Bellow Dimension is required", groups = {SubmitGroup.class})
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String heBellowDimension;

        @NotNull(message = "HE Motor Pasting Date & Time is required", groups = {SubmitGroup.class})
        private LocalDateTime heMotorPastingDateTime;

        @NotBlank(message = "NE Bellow Dimension is required", groups = {SubmitGroup.class})
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String neBellowDimension;

        @NotNull(message = "NE Motor Pasting Date & Time is required", groups = {SubmitGroup.class})
        private LocalDateTime neMotorPastingDateTime;

        @NotNull(message = "Number of Spacers is required", groups = {SubmitGroup.class})
        @DecimalMin(value = "0")
        private Integer numberOfSpacers;

        @Size(max = ValidationConstants.MAX_LONG_TEXT)
        private String pastingDetails;

        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX)
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TceCleaning {
        @NotNull(message = "TCE Cleaning Date & Time is required", groups = {SubmitGroup.class})
        private LocalDateTime tceCleaningDateTime;

        @NotNull(message = "Solvent Used Qty is required", groups = {SubmitGroup.class})
        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 3)
        private BigDecimal solventUsedQtyKg;

        @NotBlank(message = "Observation is required", groups = {SubmitGroup.class})
        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX)
        private String observation;

        @Valid
        private DocumentDto testReport;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreHeating {
        private LocalDateTime preHeatingDateTime;

        @NotBlank(message = "Vacuum Bagging Applied is required", groups = {SubmitGroup.class})
        private String vacuumBaggingApplied;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "760.0")
        @Digits(integer = 3, fraction = 3)
        private BigDecimal vacuumApplied;

        @NotBlank(message = "Pre-heating Recipe is required", groups = {SubmitGroup.class})
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String preHeatingRecipe;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String otherDuration;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String otherTemperature;

        private List<@Valid ParameterValueRow> temperatureDuration;
        private List<@Valid ParameterValueRow> preHeatingMonitoring;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterValueRow {
        @JsonProperty("SR_NO")
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        @NotBlank(message = "parameter is required", groups = {SubmitGroup.class})
        private String parameter;

        @Size(max = ValidationConstants.MAX_LONG_TEXT)
        @NotBlank(message = "value is required", groups = {SubmitGroup.class})
        private String value;

        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX)
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinerCoatingOperation {
        private LocalDateTime linerCoatingDateTime;

        @NotBlank(message = "Liner Type is required", groups = {SubmitGroup.class})
        private String linerType;

        @NotBlank(message = "Batch No is required", groups = {SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String batchNo;

        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 3)
        private BigDecimal batchSize;

        private List<@Valid Ingredient> premixIngredients;
        private List<@Valid Ingredient> finalMixIngredients;

        @NotBlank(message = "Qualifying Subscale Batch No is required", groups = {SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String qualifyingSubscaleBatchNo;

        private List<@Valid QualificationParameter> qualificationParameters;
        private List<@Valid ParameterValueRow> linerApplicationLog;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String rh;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ingredient {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String materialName;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String ingredient;

        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String mfgLot;

        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 3)
        private BigDecimal partsByWeight;

        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 3)
        private BigDecimal quantityTaken;

        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 3)
        private BigDecimal totalQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualificationParameter {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String parameter;

        @Size(max = ValidationConstants.MAX_LONG_TEXT)
        private String specification;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        @NotBlank(message = "result is required", groups = {SubmitGroup.class})
        private String result;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchToCasting {
        private List<@Valid DispatchVisualObservation> dispatchVisualObservations;
        private List<@Valid ParameterValueRow> dispatchToCastingDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchVisualObservation {
        @JsonProperty("SR_NO")
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX)
        private String parameter;

        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX)
        @NotBlank(message = "observations is required", groups = {SubmitGroup.class})
        private String observations;

        @Pattern(regexp = "^$|" + ValidationConstants.ALPHABET_WITH_SPECIAL)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX)
        private String remarks;
    }
}