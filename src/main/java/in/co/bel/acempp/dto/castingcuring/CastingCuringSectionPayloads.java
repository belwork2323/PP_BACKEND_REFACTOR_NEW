package in.co.bel.acempp.dto.castingcuring;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.IntegerValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.StringValue;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CastingCuringSectionPayloads {

    private CastingCuringSectionPayloads() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastingSetup {
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "castingType cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN, message = "castingType format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String castingType;

        @NotNull(message = "castingStation is required", groups = {DraftGroup.class, SubmitGroup.class})
        @NotBlank(message = "castingStation cannot be blank", groups = {DraftGroup.class, SubmitGroup.class})
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "castingStation cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN, message = "castingStation format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String castingStation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuringSetup {
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "oven cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN, message = "oven format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String oven;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "ovenNo cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN, message = "ovenNo format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String ovenNo;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "curingType cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN, message = "curingType format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String curingType;

        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "configuration cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN, message = "configuration format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String configuration;

        private Integer motorsToCureCount;

        private Integer ovensUtilized;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastingSections {
        @Valid
        private FinalAssemblyDetails finalAssemblyDetails;
        @Valid
        private CastingProcess castingProcess;
        @Valid
        private SlurryCastDetails slurryCastDetails;
        @Valid
        private PostCastOperations postCastOperations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalAssemblyDetails {
        private List<@Valid MotorCasing> motorCasing;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MotorCasing {
        private List<@Valid MandrelMeasurement> mandrelMeasurements;
        private List<@Valid FeedPipeDistance> feedPipeDistance;

        @NotNull(message = "emptyMotorWeight is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal emptyMotorWeight;
        @NotBlank(message = "vaccum applied is required", groups = SubmitGroup.class)
        private String vacuumApplied;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MandrelMeasurement {
        private Integer srNo;

        @NotNull(message = "aMock is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal aMock;

        @NotNull(message = "bMock is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal bMock;

        @NotNull(message = "bFinal is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal bFinal;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal cMock;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal cFinal;

        @NotNull(message = "bellowsThicknessD is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal bellowsThicknessD;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal eMock;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal eFinal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedPipeDistance {
        private Integer srNo;

        @NotNull(message = "reading1 is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal reading1;

        @NotNull(message = "reading2 is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal reading2;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastingProcess {
        private List<@Valid FinalMixBowlDetail> finalMixBowlDetails;
        private List<@Valid CastingFromBowlDetail> castingFromBowlDetails;

        @NotBlank(message = "initialVacuum is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, groups = {DraftGroup.class, SubmitGroup.class})
        private String initialVacuum;

        @NotBlank(message = "vacuumPressureCasting is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, groups = {DraftGroup.class, SubmitGroup.class})
        private String vacuumPressureCasting;

        @NotBlank(message = "vacuumPressureSoaking is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, groups = {DraftGroup.class, SubmitGroup.class})
        private String vacuumPressureSoaking;

        @NotBlank(message = "TDI Clearance After Vacuum Trial is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX, groups = {DraftGroup.class, SubmitGroup.class})
        private String tdiClearanceAfterVacuum;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalMixBowlDetail {
        private Integer srNo;

        @NotBlank(message = "bowlId is required", groups = SubmitGroup.class)
        @Size(max = 50, groups = {DraftGroup.class, SubmitGroup.class})
        private String bowlId;

        @NotNull(message = "bowlReceiptDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("bowlReceiptTime")
        private LocalDateTime bowlReceiptDateTime;

        @NotNull(message = "initialWeight is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal initialWeight;

        @NotNull(message = "finalWeight is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal finalWeight;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal initialSlurryDepth;

        @NotNull(message = "dcOpenDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("dcOpenTime")
        private LocalDateTime dcOpenDateTime;

        @NotNull(message = "dcCloseDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("dcCloseTime")
        private LocalDateTime dcCloseDateTime;

        @NotNull(message = "slurryDepthAfterDc is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal slurryDepthAfterDc;

        @NotNull(message = "ballValveOpenDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("ballValveOpenTime")
        private LocalDateTime ballValveOpenDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastingFromBowlDetail {
        private Integer srNo;

        @NotBlank(message = "bowlId is required", groups = SubmitGroup.class)
        private String bowlId;

        @NotBlank(message = "timeInterval is required", groups = SubmitGroup.class)
        private String timeInterval;

        @NotNull(message = "rh is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal rh;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal viscosity;

        @NotBlank(message = "motorId is required", groups = SubmitGroup.class)
        private String motorId;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal slurryDepth;

        @NotNull(message = "slurryCast is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal slurryCast;

        @NotNull(message = "flowRate is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal flowRate;

        @NotNull(message = "valveOpening is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal valveOpening;

        @NotNull(message = "vacuumLevel is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal vacuumLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlurryCastDetails {
        @NotEmpty(message = "slurryCastFromBowls cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "slurry-cast row is required", groups = SubmitGroup.class) @Valid SlurryCastFromBowl> slurryCastFromBowls;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlurryCastFromBowl {
        @NotBlank(message = "rowKey is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = StringValue.class)
        private String rowKey;

        @NotBlank(message = "fmMotorLabel is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX, message = "fmMotorLabel cannot exceed 255 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String fmMotorLabel;

        @NotNull(message = "slurryCast is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal slurryCast;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCastOperations {
        @NotEmpty(message = "postCastTable cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "post-cast operation is required", groups = SubmitGroup.class) @Valid PostCastOperation> postCastTable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCastOperation {
        private Integer srNo;

        @NotBlank(message = "activity is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = StringValue.class)
        private String activity;

        @NotBlank(message = "details are required", groups = SubmitGroup.class)
        @JsonDeserialize(using = StringValue.class)
        private String details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuringSections {
        @Valid
        @NotNull(message = "curingCycles is required", groups = SubmitGroup.class)
        private CuringCycles curingCycles;

        @Valid
        @NotNull(message = "postCuringDetails is required", groups = SubmitGroup.class)
        private PostCuringDetails postCuringDetails;

        @Valid
        @NotNull(message = "decoringDetails is required", groups = SubmitGroup.class)
        private DecoringDetails decoringDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuringCycles {
        @NotEmpty(message = "curingTable cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "curing cycle row is required", groups = SubmitGroup.class) @Valid CuringCycle> curingTable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuringCycle {
        private Integer srNo;

        @NotNull(message = "temperature is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal temperature;

        @NotNull(message = "time is required", groups = SubmitGroup.class)
        private Integer time;

        @NotNull(message = "startDateTime is required", groups = SubmitGroup.class)
        private LocalDateTime startDateTime;

        @NotNull(message = "endDateTime is required", groups = SubmitGroup.class)
        private LocalDateTime endDateTime;

        @NotBlank(message = "hotWaterStatus is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = StringValue.class)
        private String hotWaterStatus;

        @Valid
        private PropellantPressure propellantPressure;

        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "remarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropellantPressure {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal reading1;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal reading2;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCuringDetails {
        @NotBlank(message = "otherObservations are required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX, message = "otherObservations cannot exceed 255 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String otherObservations;

        @NotBlank(message = "visualObservation is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX, message = "visualObservation cannot exceed 255 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String visualObservation;

        private LocalDateTime pressurePlateRemovalDateTime;

        @NotBlank(message = "shoreAHardness is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = StringValue.class)
        private String shoreAHardness;

        @NotNull(message = "decoringDispatchDateTime is required", groups = SubmitGroup.class)
        private LocalDateTime decoringDispatchDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecoringDetails {
        @NotNull(message = "decoringDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("decoringDate")
        private LocalDateTime decoringDateTime;

        @NotBlank(message = "buildingNo is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = StringValue.class)
        private String buildingNo;

        @NotNull(message = "decoringLoad is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal decoringLoad;

        @NotNull(message = "rh is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal rh;

        @NotNull(message = "propellent temperature is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal propellantTemperature;

        @NotNull(message = "mandrel weight is required", groups = SubmitGroup.class)
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal mandrelWeight;


        @NotBlank(message = "decoringRemarks is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX, message = "decoringRemarks cannot exceed 255 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String decoringRemarks;

        @NotEmpty(message = "decoringVisualObservation cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "visual observation file is required", groups = SubmitGroup.class) @Valid DocumentDto> decoringVisualObservation;
    }
}