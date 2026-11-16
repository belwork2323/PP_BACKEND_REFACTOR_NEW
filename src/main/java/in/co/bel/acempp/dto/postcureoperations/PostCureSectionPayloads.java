package in.co.bel.acempp.dto.postcureoperations;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.IntegerValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.StringValue;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.enums.PostCureInhibitorType;
import in.co.bel.acempp.enums.PostCureLocation;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.custom.ValidMixIngredient;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PostCureSectionPayloads {

    private PostCureSectionPayloads() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LooseFlapFillingDetails {
        @JsonAlias("bellowBondingDetails")
        @NotEmpty(message = "bellowRemovalDetails cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "bellow-removal row is required", groups = SubmitGroup.class)
        @Valid BellowRemovalDetail> bellowRemovalDetails;

        @Valid
        @NotNull(message = "epoxyPreparationIngredients is required", groups = SubmitGroup.class)
        private EpoxyPreparationDetails epoxyPreparationIngredients;

        @Valid
        @NotNull(message = "qualificationDetails is required", groups = SubmitGroup.class)
        private QualificationDetails qualificationDetails;

        @NotEmpty(message = "fillingDetails cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "filling row is required", groups = SubmitGroup.class)
        @Valid FillingDetail> fillingDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BellowRemovalDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;

        @NotNull(message = "location is required", groups = SubmitGroup.class)
        private PostCureLocation location;

        @NotNull(message = "fromDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("fromDate")
        @Field("fromDate")
        private LocalDateTime fromDateTime;

        @NotNull(message = "toDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("toDate")
        @Field("toDate")
        private LocalDateTime toDateTime;

        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "observations cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "observations format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String observations;

        /** QA row remarks (QC-entered); ignored by manufacturing UI. */
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "qaRemarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "qaRemarks format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String qaRemarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EpoxyPreparationDetails {
        @NotBlank(message = "batchNo is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
                message = "batchNo cannot exceed 100 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
                message = "batchNo format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        private String batchNo;

        @NotNull(message = "preparationDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("preparationDate")
        @Field("preparationDate")
        private LocalDateTime preparationDateTime;

        @NotEmpty(message = "parameters cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "ingredient row is required", groups = SubmitGroup.class)
        @Valid MixIngredient> parameters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    // Required ingredient fields (mfgLot, quantityTaken, etc.) apply on SUBMIT only —
    // DRAFT may persist incomplete mix rows.
    @ValidMixIngredient(groups = SubmitGroup.class)
    public static class MixIngredient {
        private String rowKey;
        private String ingredient;
        /** Optional catalog code — used by flexible Others mixes for lot lookup after reload. */
        private String materialCode;
        private String mfgLot;
        private String partsByWeight;
        private BigDecimal quantityTaken;
        /** QA row remarks (QC-entered); ignored by manufacturing UI. */
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "qaRemarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "qaRemarks format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String qaRemarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualificationDetails {
        @NotBlank(message = "batchNo is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
                message = "batchNo cannot exceed 100 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
                message = "batchNo format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        private String batchNo;

        @NotNull(message = "preparationDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("preparationDate")
        @Field("preparationDate")
        private LocalDateTime preparationDateTime;

        @NotEmpty(message = "parameters cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "qualification row is required", groups = SubmitGroup.class)
        @Valid QualificationParameter> parameters;

        @NotNull(message = "qcReport is required", groups = SubmitGroup.class)
        private List<@NotNull(message = "QC report entry is required", groups = SubmitGroup.class)
        @Valid DocumentDto> qcReport;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Specification {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal min;

        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal max;

        @Size(max = 50, message = "unit cannot exceed 50 characters", groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualificationParameter {
        private Integer srNo;

        @NotBlank(message = "parameter is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
                message = "parameter cannot exceed 100 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        private String parameter;

        @Valid
        @NotNull(message = "specification is required", groups = SubmitGroup.class)
        private Specification specification;

        @Digits(integer = 10, fraction = 3,
                message = "result can have up to 10 digits and 3 decimal places",
                groups = {DraftGroup.class, SubmitGroup.class})
        private BigDecimal result;

        /** QA row remarks (QC-entered); ignored by manufacturing UI. */
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "qaRemarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "qaRemarks format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String qaRemarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FillingDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;

        @NotNull(message = "location is required", groups = SubmitGroup.class)
        private PostCureLocation location;

        @NotNull(message = "fromDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("fromDate")
        @Field("fromDate")
        private LocalDateTime fromDateTime;

        @NotNull(message = "toDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("toDate")
        @Field("toDate")
        private LocalDateTime toDateTime;

        @JsonAlias("qtyFilled")
        @NotNull(message = "quantityFilled is required", groups = SubmitGroup.class)
        @DecimalMin(value = "0.000001", message = "quantityFilled must be greater than 0",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Digits(integer = 10, fraction = 3,
                message = "quantityFilled can have up to 10 digits and 3 decimal places",
                groups = {DraftGroup.class, SubmitGroup.class})
        private BigDecimal quantityFilled;

        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "observations cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "observations format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String observations;

        /** QA row remarks (QC-entered); ignored by manufacturing UI. */
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "qaRemarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "qaRemarks format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String qaRemarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InhibitionDetails {
        private PostCureInhibitorType inhibitorType;

        @Valid
        private MixDetails premixDetails;

        @Valid
        private MixDetails finalMixDetails;

        @Valid
        private QualificationDetails qualificationDetails;

        @Valid
        private InhibitorBatchDetails inhibitorBatchDetails;

        private List<@NotNull(message = "application row is required", groups = SubmitGroup.class)
        @Valid ApplicationDetail> applicationDetails;

        @Valid
        private DispatchDetails dispatchDetails;

        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "notApplicableRemarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "notApplicableRemarks format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        private String notApplicableRemarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MixDetails {
        @NotBlank(message = "batchNo is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
                message = "batchNo cannot exceed 100 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
                message = "batchNo format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        private String batchNo;

        @NotNull(message = "preparationDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("preparationDate")
        @Field("preparationDate")
        private LocalDateTime preparationDateTime;

        @NotEmpty(message = "ingredients cannot be empty", groups = SubmitGroup.class)
        private List<@NotNull(message = "ingredient row is required", groups = SubmitGroup.class)
        @Valid MixIngredient> ingredients;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InhibitorBatchDetails {
        @NotBlank(message = "batchNo is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
                message = "batchNo cannot exceed 100 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
                message = "batchNo format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        private String batchNo;

        @NotNull(message = "batchSize is required", groups = SubmitGroup.class)
        @DecimalMin(value = "0.000001", message = "batchSize must be greater than 0",
                groups = {DraftGroup.class, SubmitGroup.class})
        private BigDecimal batchSize;

        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "qaRemarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "qaRemarks format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String qaRemarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;

        @NotNull(message = "location is required", groups = SubmitGroup.class)
        private PostCureLocation location;

        @NotNull(message = "fromDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("fromDate")
        @Field("fromDate")
        private LocalDateTime fromDateTime;

        @NotNull(message = "toDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("toDate")
        @Field("toDate")
        private LocalDateTime toDateTime;

        @NotNull(message = "quantityApplied is required", groups = SubmitGroup.class)
        @DecimalMin(value = "0.000001", message = "quantityApplied must be greater than 0",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Digits(integer = 10, fraction = 3,
                message = "quantityApplied can have up to 10 digits and 3 decimal places",
                groups = {DraftGroup.class, SubmitGroup.class})
        private BigDecimal quantityApplied;

        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "observations cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "observations format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String observations;

        /** QA row remarks (QC-entered); ignored by manufacturing UI. */
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "qaRemarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "qaRemarks format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String qaRemarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchDetails {
        @NotNull(message = "dispatchDateTime is required", groups = SubmitGroup.class)
        @JsonAlias("dispatchDate")
        @Field("dispatchDate")
        private LocalDateTime dispatchDateTime;

        @NotBlank(message = "dispatchStation is required", groups = SubmitGroup.class)
        @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
                message = "dispatchStation cannot exceed 100 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
                message = "dispatchStation format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String dispatchStation;

        /** QA section remarks (QC-entered); ignored by manufacturing UI. */
        @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
                message = "qaRemarks cannot exceed 255 characters",
                groups = {DraftGroup.class, SubmitGroup.class})
        @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
                message = "qaRemarks format is invalid",
                groups = {DraftGroup.class, SubmitGroup.class})
        @JsonDeserialize(using = StringValue.class)
        private String qaRemarks;
    }
}