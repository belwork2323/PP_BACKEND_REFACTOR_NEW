package in.co.bel.acempp.dto.casepreparation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.IntegerValue;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.enums.CasingType;
import jakarta.validation.Valid;
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
        private CasingType typeOfCasing;
        private String typeOfInsulation;
        private String abradingWheelType;
        private List<@Valid AbradingCutRow> firstCut;
        private List<@Valid AbradingCutRow> secondCut;

        /** Legacy Mongo shape; deserialized on read only, never returned in API responses. */
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
        private String dustQty;
        private String observations;
        private List<DocumentDto> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbradingDetail {
        @JsonProperty("SR_NO")
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String operation;
        private String value;
        private String remarksObservations;
        private List<DocumentDto> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BellowBonding {
        private String adhesiveDetails;
        private String heBellowDimension;
        private LocalDateTime heMotorPastingDateTime;
        private String neBellowDimension;
        private LocalDateTime neMotorPastingDateTime;
        @JsonDeserialize(using = IntegerValue.class)
        private Integer numberOfSpacers;
        private String pastingDetails;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TceCleaning {
        private LocalDateTime tceCleaningDateTime;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal solventUsedQtyKg;
        private String observation;
        private DocumentDto testReport;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreHeating {
        private LocalDateTime preHeatingDateTime;
        private String vacuumBaggingApplied;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal vacuumApplied;
        private String preHeatingRecipe;
        private String otherDuration;
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
        private String parameter;
        private String value;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinerCoatingOperation {
        private LocalDateTime linerCoatingDateTime;
        private String linerType;
        private String batchNo;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal batchSize;
        private List<@Valid Ingredient> premixIngredients;
        private List<@Valid Ingredient> finalMixIngredients;
        private String qualifyingSubscaleBatchNo;
        private List<@Valid QualificationParameter> qualificationParameters;
        private List<@Valid ParameterValueRow> linerApplicationLog;
        private String rh;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ingredient {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String materialName;
        private String ingredient;
        private String mfgLot;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal partsByWeight;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal quantityTaken;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal totalQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualificationParameter {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String parameter;
        private String specification;
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
        private String parameter;
        private String observations;
        private String remarks;
    }
}
