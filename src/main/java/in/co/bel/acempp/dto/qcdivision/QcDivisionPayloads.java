package in.co.bel.acempp.dto.qcdivision;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.BigDecimalValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.IntegerValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.StringValue;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.DecoringDetails;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.PropellantPressure;
import in.co.bel.acempp.dto.ndt.motor.AdditionalExposureDetailsDto;
import in.co.bel.acempp.dto.ndt.motor.RadiographyDetailsDto;
import in.co.bel.acempp.dto.ndt.motor.SignedNdtReportDto;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.dto.statictestingfacility.StaticMotorData;
import in.co.bel.acempp.dto.statictestingfacility.StaticMotorDataListDeserializer;
import in.co.bel.acempp.dto.subscale.subscalecreation.CuringTableDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.HardwareArticleSelectionDTO;
import in.co.bel.acempp.dto.rawmaterialpreparation.AluminumProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.ApCoarseProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.ApFineProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.ApUltraFineProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.DoaProcessDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.DryingTrayOvenDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.LotDetailDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.SievingDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.WeightmentSheetRequest;
import in.co.bel.acempp.enums.PostCureInhibitorType;
import in.co.bel.acempp.enums.PostCureLocation;
import in.co.bel.acempp.enums.PostCureOperationType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fixed wire models for the division-specific QC payloads.
 */
public final class QcDivisionPayloads {

    private QcDivisionPayloads() {
    }

    private static LocalDateTime combineLegacyDateTime(String date, String time) {
        if (time == null || time.isBlank()) return null;
        try {
            return DateTimeConversionUtil.parseLocalDateTime(time);
        } catch (DateTimeParseException exception) {
            if (date == null || date.isBlank()) throw exception;
            LocalDate parsedDate = DateTimeConversionUtil.parseLocalDate(date);
            return DateTimeConversionUtil.parseLocalDateTime(parsedDate + "T" + time);
        }
    }

    public interface DivisionDataInput {
    }

    public interface DivisionDataResponse {
    }

    public interface RawMaterialSection {
    }

    public interface PropellantProcess {
    }

    @Data
    public static class DivisionDetailInput {
        private String division;
        private String subType;
        private Status divisionSubmissionType;
        private DivisionDataInput data;
    }

    @Data
    public static class RawMaterialRevalidationData implements DivisionDataInput, DivisionDataResponse {
        private List<RevalidationMaterial> materials;
    }

    @Data
    public static class RevalidationMaterial {
        private String ingredient;
        private List<RevalidationQcDetail> qcDetails;
    }

    @Data
    public static class RevalidationQcDetail {
        private String lotBatchNumber;
        private String parameter;
        private String specification;
        private String result;
        /** Numeric or alphanumeric (when specs are N/A); stored/returned as text. */
        @JsonAlias("acemResult")
        private String acemQcResult;
        private String validity;
        private String remarks;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> qcCertificate;
    }

    @Data
    public static class RawMaterialProcessingDataInput implements DivisionDataInput {
        private List<RawProcessingPremixInput> premixes;
        /** QC-owned weighment sheet (editable when material schema is unavailable). */
        private WeightmentSheetRequest weightmentSheet;
    }

    @Data
    public static class RawMaterialProcessingDataResponse implements DivisionDataResponse {
        private List<RawProcessingPremixResponse> premixes;
        /** RMP weighment sheet — used when a processing material has no QC schema. */
        private WeightmentSheetRequest weightmentSheet;
    }

    @Data
    public static class RawProcessingPremixBase {
        private Integer premixNo;
        private LocalDate premixDate;
        private String materialType;
        private List<RawMaterialProcess> solidProcess;
        private List<RawMaterialProcess> liquidProcess;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class RawProcessingPremixInput extends RawProcessingPremixBase {
        private Status premixSubmissionType;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class RawProcessingPremixResponse extends RawProcessingPremixBase {
        private Status premixSubmissionType;
        private Status premixSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    public static class RawMaterialProcess {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Long gradeId;
        private String gradeCode;
        private String schemaVersion;
        private String schemaType;
        @JsonDeserialize(contentUsing = RawMaterialSectionDeserializer.class)
        private List<RawMaterialSection> sections;

        /** RMP-parity typed process — retained on draft even when section rows are empty. */
        private String processType;
        private List<LotDetailDto> lotDetails = new ArrayList<>();
        /** QA Raw Material Processing only — section-level remarks under Lot Details. */
        private String lotDetailsQaRemarks;
        /** QA Raw Material Processing only — process-root remarks (e.g. DEFAULT liquid). */
        private String qaRemarks;
        private DryingTrayOvenDto drying;
        private SievingDto sieving;
        private ApCoarseProcessDto apCoarse;
        private ApFineProcessDto apFine;
        private ApUltraFineProcessDto apUltraFine;
        private AluminumProcessDto aluminum;
        private DoaProcessDto doa;
    }

    @Data
    public static class FeedMaterialSection implements RawMaterialSection {
        private String sectionId;
        private List<FeedMaterialRow> sectionData;
    }

    @Data
    public static class FeedMaterialRow {
        private String lotNumber;
        private String quantity;
    }

    /** RMP-parity lot rows ({@code sectionId: lotDetails}). */
    @Data
    public static class LotDetailsSection implements RawMaterialSection {
        private String sectionId;
        private List<LotDetailsRow> sectionData;
    }

    @Data
    public static class LotDetailsRow {
        private String lotId;
        private Double quantity;
    }

    @Data
    public static class BlendingDryingSection implements RawMaterialSection {
        private String sectionId;
        private List<OperationParameterRow> sectionData;
    }

    @Data
    public static class OperationParameterRow {
        private String operation;
        private String setParameter;
        private String actualParameter;
    }

    @Data
    public static class DryingOperationSection implements RawMaterialSection {
        private String sectionId;
        private List<TimedOperationParameterRow> sectionData;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TimedOperationParameterRow extends OperationParameterRow {
        @JsonAlias("startTime")
        private LocalDateTime startDateTime;
        @JsonAlias("endTime")
        private LocalDateTime endDateTime;
    }

    @Data
    public static class UnloadingDispatchSection implements RawMaterialSection {
        private String sectionId;
        private List<UnloadingDispatchRow> sectionData;
    }

    @Data
    public static class UnloadingDispatchRow {
        private String binNumber;
        private String binCapacity;
        private String filledQuantity;
    }

    @Data
    public static class ParticleSizeDistributionSection implements RawMaterialSection {
        private String sectionId;
        private List<ParticleSizeRow> sectionData;
    }

    @Data
    public static class ParticleSizeRow {
        private String psdRequirement;
        private String specification;
        private String result;
    }

    @Data
    public static class DryingTrayOvenSection implements RawMaterialSection {
        private String sectionId;
        private List<DryingTrayOvenRow> sectionData;
    }

    @Data
    public static class DryingTrayOvenRow {
        private String ovenType;
        private String ovenNumber;
        private String ovenSetTemperature;
        private LocalDateTime startDatetime;
        private LocalDateTime endDatetime;
        private String moisture;
        private String observation;
    }

    @Data
    public static class SievingSection implements RawMaterialSection {
        private String sectionId;
        private List<SievingRow> sectionData;
    }

    @Data
    public static class SievingRow {
        private LocalDateTime sievingDispatchDatetime;
        private String sievedQuantity;
        private String sieveMeshSize;
        private String observation;
    }

    @Data
    public static class ShiftingToMixingSection implements RawMaterialSection {
        private String sectionId;
        private List<ShiftingToMixingRow> sectionData;
    }

    @Data
    public static class ShiftingToMixingRow {
        private String numberOfDrums;
        private String amountOfMaterial;
    }

    @Data
    public static class LiquidBlendingSection implements RawMaterialSection {
        private String sectionId;
        private List<LiquidBlendingRow> sectionData;
    }

    @Data
    public static class LiquidBlendingRow {
        private String mfgBatchLotNumber;
        private String totalQuantity;
        private String equipmentId;
        private String agitatorRpm;
        private String processTemperature;
        private String jacketTemperature;
        private LocalDateTime startDatetime;
        private LocalDateTime endDatetime;
        private String observation;
        private List<QcCheckRow> qcChecks;
    }

    @Data
    public static class QcCheckRow {
        private String parameter;
        private String specification;
        private String result;
    }

    @Data
    public static class TdiProcessingSection implements RawMaterialSection {
        private String sectionId;
        private List<TdiProcessingRow> sectionData;
    }

    @Data
    public static class TdiProcessingRow {
        private String mfgBatchLotNumber;
        private String quantity;
        private LocalDateTime dispatchDatetime;
        private String observation;
        private String totalQuantitySentForPremix;
    }

    /**
     * RMP-parity catch-all for sectionIds the typed QC models do not yet enumerate
     * (e.g. {@code apEquipment}, {@code apFineGrinding}, {@code trayOvenStorage}).
     * Same flexible shape as manufacturing {@code SectionDataRequest}.
     */
    @Data
    public static class FlexibleRawMaterialSection implements RawMaterialSection {
        private String sectionId;
        private List<Map<String, Object>> sectionData;
    }

    @Data
    public static class MixingDataInput implements DivisionDataInput {
        private List<MixingPremixInput> premixes;
    }

    @Data
    public static class MixingDataResponse implements DivisionDataResponse {
        private List<MixingPremixResponse> premixes;
    }

    @Data
    public static class MixingPremixInput {
        private Integer premixNo;
        private MixingStageInput premixDetails;
        private MixingStageInput finalMixDetails;
    }

    @Data
    public static class MixingPremixResponse {
        private Integer premixNo;
        private MixingStageResponse premixDetails;
        private MixingStageResponse finalMixDetails;
    }

    @Data
    public static class MixingStageBase {
        private String bowlNo;
        private String dateOfPremix;
        private String dateOfFinalMix;
        private String mixerBuildingNo;
        private String premixQty;
        private List<MixingParameter> parameters;
        private List<ViscosityBuildUpRow> viscosityBuildUp;
        /** QA Remarks for Final Mix viscosity build-up section (not approval remarks). */
        private String viscosityQaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MixingStageInput extends MixingStageBase {
        private Status premixSubmissionType;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MixingStageResponse extends MixingStageBase {
        private Status premixSubmissionType;
        private Status premixSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    public static class MixingParameter {
        private String parameterId;
        private String parameter;
        private String parameterName;
        private String specification;
        private String value;
        private List<String> values;
        /** Mixing Cycle Master sample count — required for QC VALUE_1..VALUE_N round-trip. */
        private Integer noOfSamples;
        private String remarks;
    }

    @Data
    public static class ViscosityBuildUpRow {
        private Integer srNo;
        private String time;
        private String finalMix1;
    }

    @Data
    public static class HardwareDataInput implements DivisionDataInput {
        private List<HardwareMotorInput> motorDetails;
    }

    @Data
    public static class HardwareDataResponse implements DivisionDataResponse {
        private List<HardwareMotorResponse> motorDetails;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwareMotorInput {
        private String motorId;
        private Status motorSubmissionType;
        private HardwareAbrading abrading;
        private HardwarePreheating preheating;
        private HardwareLinerCoating linerCoating;
        private HardwareDispatch dispatch;
        private HardwareAttachments attachments;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class HardwareMotorResponse extends HardwareMotorInput {
        private Status motorSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwareAbrading {
        private List<HardwareCutRow> firstCut;
        private List<HardwareCutRow> secondCut;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwareCutRow {
        private Integer srNo;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private String dustQty;
        private String observations;

        @JsonIgnore
        private String legacyDate;
        @JsonIgnore
        private String legacyStartTime;
        @JsonIgnore
        private String legacyEndTime;

        @JsonProperty(value = "date", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyDate(String value) {
            legacyDate = value;
            applyLegacyDateTimes();
        }

        @JsonProperty(value = "startTime", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyStartTime(String value) {
            legacyStartTime = value;
            applyLegacyDateTimes();
        }

        @JsonProperty(value = "endTime", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyEndTime(String value) {
            legacyEndTime = value;
            applyLegacyDateTimes();
        }

        private void applyLegacyDateTimes() {
            if (startDateTime == null) {
                startDateTime = combineLegacyDateTime(legacyDate, legacyStartTime);
            }
            if (endDateTime == null) {
                endDateTime = combineLegacyDateTime(legacyDate, legacyEndTime);
            }
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwarePreheating {
        private LocalDateTime preHeatingDateTime;
        private List<HardwarePreheatingRow> rows;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwarePreheatingRow {
        private Integer srNo;
        private String date;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private String ovenNumber;
        private String buildingNo;
        private String temperature;
        private String vacuumLevel;
        private String observations;

        @JsonIgnore
        private String legacyStartTime;
        @JsonIgnore
        private String legacyEndTime;

        public void setDate(String value) {
            date = value;
            applyLegacyDateTimes();
        }

        @JsonProperty(value = "startTime", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyStartTime(String value) {
            legacyStartTime = value;
            applyLegacyDateTimes();
        }

        @JsonProperty(value = "endTime", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyEndTime(String value) {
            legacyEndTime = value;
            applyLegacyDateTimes();
        }

        private void applyLegacyDateTimes() {
            if (startDateTime == null) {
                startDateTime = combineLegacyDateTime(date, legacyStartTime);
            }
            if (endDateTime == null) {
                endDateTime = combineLegacyDateTime(date, legacyEndTime);
            }
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwareLinerCoating {
        private LocalDateTime linerCoatingDateTime;
        private List<HardwareLinerRow> rows;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwareLinerRow {
        private Integer srNo;
        private String date;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private String linerQty;
        private String insulationTemp;
        private String rh;
        private String observations;

        @JsonIgnore
        private String legacyStartTime;
        @JsonIgnore
        private String legacyEndTime;

        public void setDate(String value) {
            date = value;
            applyLegacyDateTimes();
        }

        @JsonProperty(value = "startTime", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyStartTime(String value) {
            legacyStartTime = value;
            applyLegacyDateTimes();
        }

        @JsonProperty(value = "endTime", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyEndTime(String value) {
            legacyEndTime = value;
            applyLegacyDateTimes();
        }

        private void applyLegacyDateTimes() {
            if (startDateTime == null) {
                startDateTime = combineLegacyDateTime(date, legacyStartTime);
            }
            if (endDateTime == null) {
                endDateTime = combineLegacyDateTime(date, legacyEndTime);
            }
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwareDispatch {
        private String hePunctures;
        private String nePunctures;
        private String lfPunctures;
        private LocalDateTime dispatchDateTime;
        private List<HardwareVisualObservation> visualObservations;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwareVisualObservation {
        private Integer srNo;
        private String parameter;
        private String observations;
        private String remarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HardwareAttachments {
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadReport;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadGraph;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadPhoto;
    }

    @Data
    public static class CastingDataInput implements DivisionDataInput {
        private List<CastingMotorInput> motorDetails;
    }

    @Data
    public static class CastingDataResponse implements DivisionDataResponse {
        private List<CastingMotorResponse> motorDetails;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CastingMotorInput {
        private String motorId;
        private Status motorSubmissionType;
        private LocalDateTime motorReceivedAt;
        private QcCastingSetup setup;
        private QcCastingSections castingSections;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CastingMotorResponse extends CastingMotorInput {
        private Status motorSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcCastingSetup {
        private String castingType;
        private String castingStation;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcCastingSections {
        private QcFinalAssemblyDetails finalAssemblyDetails;
        private QcCastingProcess castingProcess;
        private QcSlurryCastDetails slurryCastDetails;
        private QcWeightmentDetails weightmentDetails;
        private QcPostCastOperations postCastOperations;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcFinalAssemblyDetails {
        private List<QcMotorCasing> motorCasing;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcMotorCasing {
        private List<QcMandrelMeasurement> mandrelMeasurements;
        private List<QcFeedPipeDistance> feedPipeDistance;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal emptyMotorWeight;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcMandrelMeasurement {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal aMock;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal bMock;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal bFinal;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal cMock;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal cFinal;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal bellowsThicknessD;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal eMock;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal eFinal;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcFeedPipeDistance {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal reading1;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal reading2;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcCastingProcess {
        private LocalDate dateOfCasting;
        private List<QcFinalMixBowlDetail> finalMixBowlDetails;
        private List<QcCastingFromBowlDetail> castingFromBowlDetails;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal initialVacuum;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal vacuumPressureCasting;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal vacuumPressureSoaking;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcFinalMixBowlDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String bowlId;
        @JsonAlias("bowlReceiptTime")
        private LocalDateTime bowlReceiptDateTime;
        @JsonAlias("dcOpenTime")
        private LocalDateTime dcOpenDateTime;
        @JsonAlias("dcCloseTime")
        private LocalDateTime dcCloseDateTime;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal initialWeight;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal finalWeight;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal initialSlurryDepth;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal slurryDepthAfterDc;
        @JsonAlias("ballValveOpenTime")
        private LocalDateTime ballValveOpenDateTime;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcCastingFromBowlDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String bowlId;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal timeInterval;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal rh;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal viscosity;
        private String motorId;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal slurryDepth;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal slurryCast;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal flowRate;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal valveOpening;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal vacuumLevel;
        private String remarks;
        /** QA row remarks (QC-entered); prefer over legacy {@code remarks}. */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcSlurryCastDetails {
        private List<QcSlurryCastFromBowl> slurryCastFromBowls;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcSlurryCastFromBowl {
        @JsonDeserialize(using = StringValue.class)
        private String rowKey;
        private String fmMotorLabel;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal slurryCast;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcWeightmentDetails {
        private QcLoadCellReading loadCellReading;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal totalWeight;
        /** QA remarks for the single weighment row (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcLoadCellReading {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal initial;
        @JsonDeserialize(using = BigDecimalValue.class)
        @JsonProperty("final")
        private BigDecimal finalReading;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCastOperations {
        private List<QcPostCastTableRow> postCastTable;
        private String pressurePlateAssemblyRequired;
        private List<QcPressurePlateDetail> pressurePlateDetails;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCastTableRow {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String activity;
        private String details;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPressurePlateDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @JsonAlias("startTime")
        private LocalDateTime startDateTime;
        @JsonAlias("endTime")
        private LocalDateTime endDateTime;
        private String pressureSensorUsed;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal initialPressureReading;
        private String observations;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    public static class CuringDataInput implements DivisionDataInput {
        private List<CuringMotorInput> curingDetails;
    }

    @Data
    public static class CuringDataResponse implements DivisionDataResponse {
        private List<CuringMotorResponse> curingDetails;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CuringMotorInput {
        private String motorId;
        private Status motorSubmissionType;
        private String motorStage;
        private String curingType;
        private String oven;
        private String ovenNumber;
        private LocalDateTime motorReceivedAt;
        /** QA remarks for curing configuration / setup (QC-entered). */
        private String qaRemarks;
        private QcCuringSections curingSections;
        private QcCuringSubscaleDetails subscaleDetails;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CuringMotorResponse extends CuringMotorInput {
        private Status motorSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcCuringSections {
        private QcCuringCycles curingCycles;
        private QcPostCuringDetails postCuringDetails;
        private DecoringDetails decoringDetails;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcCuringCycles {
        private List<QcCuringCycleRow> curingTable;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcCuringCycleRow {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal temperature;
        @JsonDeserialize(using = IntegerValue.class)
        private Integer time;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        @JsonDeserialize(using = IntegerValue.class)
        private Integer actualDurationMinutes;
        private PropellantPressure propellantPressure;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal peakPressureAchieved;
        private String hotWaterStatus;
        private String remarks;
        /** QA row remarks (QC-entered); prefer over legacy {@code remarks}. */
        private String qaRemarks;

        @JsonIgnore
        private String legacyStartDate;
        @JsonIgnore
        private String legacyStartTime;
        @JsonIgnore
        private String legacyEndDate;
        @JsonIgnore
        private String legacyEndTime;

        @JsonProperty(value = "startDate", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyStartDate(String value) {
            legacyStartDate = value;
            applyLegacyStartDateTime();
        }

        @JsonProperty(value = "startTime", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyStartTime(String value) {
            legacyStartTime = value;
            applyLegacyStartDateTime();
        }

        @JsonProperty(value = "endDate", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyEndDate(String value) {
            legacyEndDate = value;
            applyLegacyEndDateTime();
        }

        @JsonProperty(value = "endTime", access = JsonProperty.Access.WRITE_ONLY)
        public void setLegacyEndTime(String value) {
            legacyEndTime = value;
            applyLegacyEndDateTime();
        }

        private void applyLegacyStartDateTime() {
            if (startDateTime == null) {
                startDateTime = combineLegacyDateTime(legacyStartDate, legacyStartTime);
            }
        }

        private void applyLegacyEndDateTime() {
            if (endDateTime == null) {
                endDateTime = combineLegacyDateTime(legacyEndDate, legacyEndTime);
            }
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCuringDetails {
        private String otherObservations;
        private String visualObservation;
        private LocalDateTime pressurePlateRemovalDateTime;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal shoreAHardness;
        private LocalDateTime decoringDispatchDateTime;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcCuringSubscaleDetails {
        private List<CuringTableDTO> curingTable;
        private List<HardwareArticleSelectionDTO> hardwareArticles;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    public static class DeCoringDataInput implements DivisionDataInput {
        private List<DeCoringMotorInput> deCoringDetails;
    }

    @Data
    public static class DeCoringDataResponse implements DivisionDataResponse {
        private List<DeCoringMotorResponse> deCoringDetails;
    }

    @Data
    public static class DeCoringMotorBase {
        private String motorId;
        private QcDecoringDetails decoringDetails;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcDecoringDetails {
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal decoringLoad;
        @JsonAlias("decoringDate")
        private LocalDateTime decoringDateTime;
        private String decoringRemarks;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DeCoringMotorInput extends DeCoringMotorBase {
        private Status motorSubmissionType;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DeCoringMotorResponse extends DeCoringMotorBase {
        private Status motorSubmissionType;
        private Status motorSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    public static class TrimmingDataInput implements DivisionDataInput {
        private Integer numberOfMotors;
        private List<TrimmingMotorInput> trimmingDetails;
    }

    @Data
    public static class TrimmingDataResponse implements DivisionDataResponse {
        private Integer numberOfMotors;
        private List<TrimmingMotorResponse> trimmingDetails;
    }

    @Data
    public static class TrimmingMotorBase {
        private String motorIdNo;
        private Integer motorStage;
        @JsonAlias({"motorReceivedDate", "motorReceivedAt"})
        private LocalDateTime motorReceivedDateTime;
        private List<TrimmingOperationRow> trimmingOperationDetails;
        private List<TrimmingMeasurementRow> trimmingMeasurementDetails;
        /** QA section remarks for Dimensions After Trimming (QC-entered). */
        private String measurementQaRemarks;
        private TrimmingRemarks trimmingRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TrimmingMotorInput extends TrimmingMotorBase {
        private Status motorSubmissionType;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TrimmingMotorResponse extends TrimmingMotorBase {
        private Status motorSubmissionType;
        private Status motorSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    public static class TrimmingOperationRow {
        private String machineDetails;
        @JsonAlias({"startDate", "startDateTime"})
        private LocalDateTime startDatetime;
        @JsonAlias({"completionDate", "completionDateTime", "endDate"})
        private LocalDateTime completionDatetime;
        private String arborSize;
        private String cutterSize;
        private String ingestSize;
        private String motorTruing;
        private String arborTuring;
        private String remarks;
        /** QA row remarks (QC-entered); prefer over legacy {@code remarks}. */
        private String qaRemarks;
    }

    @Data
    public static class TrimmingMeasurementRow {
        @JsonProperty("SR_NO")
        private Integer srNo;
        @JsonProperty("DIMENSION")
        private String dimension;
        @JsonProperty("SPECIFIED")
        private String specified;
        @JsonProperty("MEASUREMENT_STAGE")
        private String measurementStage;
        @JsonProperty("R2T")
        private String r2t;
        @JsonProperty("R2B")
        private String r2b;
        @JsonProperty("R1L")
        private String r1l;
        @JsonProperty("R1R")
        private String r1r;
    }

    @Data
    public static class TrimmingRemarks {
        private String remarks;
        /** QA section remarks (QC-entered); prefer over legacy {@code remarks}. */
        private String qaRemarks;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> reportFile;
    }

    @Data
    public static class PostCureDataInput implements DivisionDataInput {
        private List<PostCureMotorInput> postCureMotorDetails;
    }

    @Data
    public static class PostCureDataResponse implements DivisionDataResponse {
        private List<PostCureMotorResponse> postCureMotorDetails;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PostCureMotorInput {
        private String motorId;
        private Status motorSubmissionType;
        private PostCureOperationType operationType;
        private PostCureInhibitorType inhibitorType;
        private QcLooseFlapFillingDetails looseFlapFillingDetails;
        private QcInhibitionDetails inhibitionDetails;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PostCureMotorResponse extends PostCureMotorInput {
        private Status motorSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcLooseFlapFillingDetails {
        private List<QcPostCureBellowRemovalDetail> bellowRemovalDetails;
        private QcPostCureEpoxyPreparation epoxyPreparationIngredients;
        private QcPostCureQualificationDetails qualificationDetails;
        private List<QcPostCureFillingDetail> fillingDetails;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureBellowRemovalDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private PostCureLocation location;
        @JsonAlias("fromDate")
        private LocalDateTime fromDateTime;
        @JsonAlias("toDate")
        private LocalDateTime toDateTime;
        private String observations;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureEpoxyPreparation {
        private String batchNo;
        @JsonAlias("preparationDate")
        private LocalDateTime preparationDateTime;
        private List<QcPostCureMixIngredient> parameters;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureMixIngredient {
        @JsonDeserialize(using = StringValue.class)
        private String rowKey;
        private String ingredient;
        private String mfgLot;
        private String partsByWeight;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal quantityTaken;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureQualificationDetails {
        private String batchNo;
        @JsonAlias("preparationDate")
        private LocalDateTime preparationDateTime;
        private List<QcPostCureQualificationParameter> parameters;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> qcReport;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureQualificationParameter {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String parameter;
        private String specification;
        private String result;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureFillingDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private PostCureLocation location;
        @JsonAlias("fromDate")
        private LocalDateTime fromDateTime;
        @JsonAlias("toDate")
        private LocalDateTime toDateTime;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal quantityFilled;
        private String observations;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
    }

    /**
     * QC Post Cure inhibition block — manufacturing {@code InhibitionDetails} parity
     * (premix / final mix / batch / nested dispatch / notApplicableRemarks).
     * Flat {@code dispatchDate}/{@code dispatchStation}/{@code remarks} remain for
     * legacy payloads that were persisted before the dual-model wire shape.
     */
    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcInhibitionDetails {
        private PostCureInhibitorType inhibitorType;
        private QcPostCureMixDetails premixDetails;
        private QcPostCureMixDetails finalMixDetails;
        private QcPostCureQualificationDetails qualificationDetails;
        private QcPostCureInhibitorBatchDetails inhibitorBatchDetails;
        private List<QcPostCureApplicationDetail> applicationDetails;
        private QcPostCureDispatchDetails dispatchDetails;
        /** Legacy flat dispatch fields (prefer {@link #dispatchDetails}). */
        @JsonAlias("dispatchDate")
        private LocalDateTime dispatchDateTime;
        private String dispatchStation;
        /** Manufacturing field; also accepts legacy wire key {@code remarks}. */
        @JsonAlias("remarks")
        private String notApplicableRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureMixDetails {
        private String batchNo;
        @JsonAlias("preparationDate")
        private LocalDateTime preparationDateTime;
        private List<QcPostCureMixIngredient> ingredients;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureInhibitorBatchDetails {
        private String batchNo;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal batchSize;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureDispatchDetails {
        @JsonAlias("dispatchDate")
        private LocalDateTime dispatchDateTime;
        private String dispatchStation;
        /** QA section remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QcPostCureApplicationDetail {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private PostCureLocation location;
        @JsonAlias("fromDate")
        private LocalDateTime fromDateTime;
        @JsonAlias("toDate")
        private LocalDateTime toDateTime;
        @JsonDeserialize(using = BigDecimalValue.class)
        private BigDecimal quantityApplied;
        private String observations;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
    }

    @Data
    public static class NdtDataInput implements DivisionDataInput {
        private List<NdtMotorInput> motorDetails;
    }

    @Data
    public static class NdtDataResponse implements DivisionDataResponse {
        private List<NdtMotorResponse> motorDetails;
    }

    /**
     * QC NDT motor wire (create/update). Uses {@code location} instead of manufacturing {@code orientation}.
     */
    @Data
    public static class NdtMotorInput {
        private String motorId;
        private Status motorSubmissionType;
        private Status motorSubmissionStatus;
        private RadiographyDetailsDto radiographyDetails;
        private List<AdditionalExposureDetailsDto> additionalExposureDetails;
        private List<QcNdtRadiographyObservation> radiographyObservations;
        private List<QcNdtVisualInspectionDetails> visualInspectionDetails;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadedVideos;
        private SignedNdtReportDto signedNdtReport;
    }

    @Data
    public static class NdtMotorResponse {
        private String motorId;
        private Status motorSubmissionType;
        private Status motorSubmissionStatus;
        private RadiographyDetailsDto radiographyDetails;
        private List<AdditionalExposureDetailsDto> additionalExposureDetails;
        private List<QcNdtRadiographyObservation> radiographyObservations;
        private List<QcNdtVisualInspectionDetails> visualInspectionDetails;
        private List<DocumentDto> uploadedVideos;
        private SignedNdtReportDto signedNdtReport;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    public static class QcNdtVisualInspectionDetails {
        private String observationType;
        private Integer sectionNumber;
        private String location;
        private String observation;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadedImages;
    }

    @Data
    public static class QcNdtRadiographyObservation {
        private Integer sectionNumber;
        private String location;
        private String observation;
        private String typeOfDefect;
        /** QA row remarks (QC-entered). */
        private String qaRemarks;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadedImages;
    }

    @Data
    public static class PropellantDataInput implements DivisionDataInput {
        private List<PropellantMotorInput> motors;
    }

    @Data
    public static class PropellantDataResponse implements DivisionDataResponse {
        private List<PropellantMotorResponse> motors;
    }

    @Data
    public static class PropellantMotorBase {
        private String motorId;
        @JsonDeserialize(contentUsing = PropellantProcessDeserializer.class)
        private List<PropellantProcess> processes;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PropellantMotorInput extends PropellantMotorBase {
        private Status motorSubmissionType;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PropellantMotorResponse extends PropellantMotorBase {
        private Status motorSubmissionType;
        private Status motorSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    public static class MechanicalPropertiesProcess implements PropellantProcess {
        private String processType;
        private MechanicalProcessData data;
    }

    @Data
    public static class MechanicalProcessData {
        private MechanicalProperties mechanicalProperties;
    }

    @Data
    public static class MechanicalProperties {
        private List<MechanicalSample> samples;
        private Map<String, Object> statistics;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadGraph;
        /** QC Division section-level QA Remarks. */
        private String qaRemarks;
    }

    /**
     * Mechanical / interface samples accept any number of final-mix columns
     * ({@code FM_1}, {@code FM_2}, {@code FM_3}, …).
     */
    @Data
    public static class MechanicalSample {
        @JsonProperty("PROPERTY")
        private String property;
        @JsonProperty("SAMPLE_NO")
        private Integer sampleNo;
        @JsonProperty("SPECIFICATION")
        private String specification;
        @JsonProperty("REMARKS")
        private String remarks;
        @JsonProperty("QA_REMARKS")
        private String qaRemarks;
        @JsonProperty("AVG")
        private BigDecimal avg;
        @JsonProperty("STD_DEV")
        private BigDecimal stdDev;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Map<String, Object> fmValues = new LinkedHashMap<>();

        @JsonAnySetter
        public void putFmValue(String key, Object value) {
            putDynamicFmValue(this::setKnownMechanicalField, () -> {
                if (fmValues == null) {
                    fmValues = new LinkedHashMap<>();
                }
                return fmValues;
            }, key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getFmValues() {
            return fmValues == null ? Collections.emptyMap() : fmValues;
        }

        private boolean setKnownMechanicalField(String key, Object value) {
            if (key == null || key.isBlank()) {
                return true;
            }
            String normalized = key.trim();
            if ("PROPERTY".equalsIgnoreCase(normalized) || "property".equals(normalized)) {
                this.property = value == null ? null : String.valueOf(value);
                return true;
            }
            if ("SAMPLE_NO".equalsIgnoreCase(normalized) || "sampleNo".equals(normalized)) {
                this.sampleNo = coerceInteger(value);
                return true;
            }
            if ("SPECIFICATION".equalsIgnoreCase(normalized) || "specification".equals(normalized)) {
                this.specification = value == null ? null : String.valueOf(value);
                return true;
            }
            if ("REMARKS".equalsIgnoreCase(normalized) || "remarks".equals(normalized)) {
                this.remarks = value == null ? null : String.valueOf(value);
                return true;
            }
            if ("QA_REMARKS".equalsIgnoreCase(normalized) || "qaRemarks".equals(normalized)) {
                this.qaRemarks = value == null ? null : String.valueOf(value);
                return true;
            }
            if ("AVG".equalsIgnoreCase(normalized) || "avg".equals(normalized)) {
                this.avg = coerceBigDecimal(value);
                return true;
            }
            if ("STD_DEV".equalsIgnoreCase(normalized) || "stdDev".equals(normalized)) {
                this.stdDev = coerceBigDecimal(value);
                return true;
            }
            return false;
        }
    }

    @Data
    public static class InterfacePropertiesProcess implements PropellantProcess {
        private String processType;
        private InterfaceProcessData data;
    }

    @Data
    public static class InterfaceProcessData {
        private InterfaceProperties interfaceProperties;
    }

    @Data
    public static class InterfaceProperties {
        private List<InterfaceSample> samples;
        private Map<String, Object> statistics;
        /** QC Division section-level QA Remarks. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InterfaceSample extends MechanicalSample {
        @JsonProperty("UPLOAD_GRAPH")
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadGraph;
    }

    @Data
    public static class BurnRateProcess implements PropellantProcess {
        private String processType;
        private BurnRateProcessData data;
    }

    @Data
    public static class BurnRateProcessData {
        private List<BurnRateRow> burnRateDetails;
        /** QC Division section-level QA Remarks. */
        private String qaRemarks;
    }

    /**
     * Burn-rate rows accept any number of final-mix columns
     * ({@code FM_1}, {@code FM_2}, {@code FM_3}, …).
     */
    @Data
    public static class BurnRateRow {
        @JsonProperty("PROPERTY")
        private String property;
        @JsonProperty("SPECIFICATION")
        private String specification;
        @JsonProperty("AVG")
        private BigDecimal avg;
        @JsonProperty("STD_DEV")
        private BigDecimal stdDev;
        @JsonProperty("UPLOAD_GRAPH")
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadGraph;
        @JsonProperty("QA_REMARKS")
        private String qaRemarks;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Map<String, Object> fmValues = new LinkedHashMap<>();

        @JsonAnySetter
        public void putFmValue(String key, Object value) {
            putDynamicFmValue(this::setKnownBurnRateField, () -> {
                if (fmValues == null) {
                    fmValues = new LinkedHashMap<>();
                }
                return fmValues;
            }, key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getFmValues() {
            return fmValues == null ? Collections.emptyMap() : fmValues;
        }

        private boolean setKnownBurnRateField(String key, Object value) {
            if (key == null || key.isBlank()) {
                return true;
            }
            String normalized = key.trim();
            if ("PROPERTY".equalsIgnoreCase(normalized) || "property".equals(normalized)) {
                this.property = value == null ? null : String.valueOf(value);
                return true;
            }
            if ("SPECIFICATION".equalsIgnoreCase(normalized) || "specification".equals(normalized)) {
                this.specification = value == null ? null : String.valueOf(value);
                return true;
            }
            if ("AVG".equalsIgnoreCase(normalized) || "avg".equals(normalized)) {
                this.avg = coerceBigDecimal(value);
                return true;
            }
            if ("STD_DEV".equalsIgnoreCase(normalized) || "stdDev".equals(normalized)) {
                this.stdDev = coerceBigDecimal(value);
                return true;
            }
            if ("QA_REMARKS".equalsIgnoreCase(normalized) || "qaRemarks".equals(normalized)) {
                this.qaRemarks = value == null ? null : String.valueOf(value);
                return true;
            }
            // UPLOAD_GRAPH is handled by the annotated field / deserializer when present.
            return "UPLOAD_GRAPH".equalsIgnoreCase(normalized) || "uploadGraph".equals(normalized);
        }
    }

    @Data
    public static class BallisticEvaluationProcess implements PropellantProcess {
        private String processType;
        private BallisticProcessData data;
    }

    @Data
    public static class BallisticProcessData {
        private List<BallisticRow> ballisticDetails;
        /** Ordered BEM column ids (e.g. FM_1_BEM_NO, FM_1_BEM_NO_2) so empty columns survive round-trip. */
        private List<String> bemColumns;
        /** QC Division section-level QA Remarks. */
        private String qaRemarks;
    }

    /**
     * Ballistic rows accept any number of BEM columns per FM
     * ({@code FM_1_BEM_NO}, {@code FM_1_BEM_NO_2}, {@code FM_2_BEM_NO}, …).
     */
    @Data
    public static class BallisticRow {
        @JsonProperty("DETAILS")
        private String details;
        @JsonProperty("SPECIFICATION")
        private String specification;
        @JsonProperty("QA_REMARKS")
        private String qaRemarks;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Map<String, Object> bemValues = new LinkedHashMap<>();

        @JsonAnySetter
        public void putBemValue(String key, Object value) {
            if (key == null || key.isBlank()) {
                return;
            }
            // Known row fields must not land in the BEM catch-all map.
            if ("QA_REMARKS".equalsIgnoreCase(key) || "qaRemarks".equals(key)) {
                this.qaRemarks = value == null ? null : String.valueOf(value);
                return;
            }
            if (bemValues == null) {
                bemValues = new LinkedHashMap<>();
            }
            bemValues.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getBemValues() {
            return bemValues == null ? Collections.emptyMap() : bemValues;
        }
    }

    @Data
    public static class StaticTestFacilityDataInput implements DivisionDataInput {
        /** Resolves polymorphic staticTestingDetails from motor subType (MAIN_MOTOR vs BEM). */
        @JsonDeserialize(using = StaticMotorDataListDeserializer.class)
        private List<StaticMotorData> motors;
    }

    @Data
    public static class StaticTestFacilityDataResponse implements DivisionDataResponse {
        /** Same polymorphic resolver as input — needed for Map → response conversion. */
        @JsonDeserialize(using = StaticMotorDataListDeserializer.class)
        private List<StaticMotorData> motors;
    }

    @Data
    public static class WeightmentDataInput implements DivisionDataInput {
        private List<MotorWeightInput> motorWeightDetails;
        private WeighscaleDetails weighscaleDetails;
    }

    @Data
    public static class WeightmentDataResponse implements DivisionDataResponse {
        private List<MotorWeightResponse> motorWeightDetails;
        private WeighscaleDetails weighscaleDetails;
    }

    @Data
    public static class MotorWeightBase {
        private String motorId;
        private List<WeightRow> weights;
        @JsonDeserialize(using = QcCertificateListDeserializer.class)
        private List<DocumentDto> uploadedFiles;
        /** QC Weighment Attachments section QA Remarks. */
        private String qaRemarks;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MotorWeightInput extends MotorWeightBase {
        private Status motorSubmissionType;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MotorWeightResponse extends MotorWeightBase {
        private Status motorSubmissionType;
        private Status motorSubmissionStatus;
        private String remarks;
        private String rejectionReason;
        private PersonInfo submittedBy;
        private LocalDateTime submittedAt;
        private PersonInfo approvedBy;
        private LocalDateTime approvedAt;
    }

    @Data
    public static class WeightRow {
        private String srNo;
        private String weightParameter;
        private BigDecimal weightKg;
        private WeightCalculation calculation;
        /** QC Weighment Details row QA Remarks. */
        private String qaRemarks;
    }

    @Data
    public static class WeightCalculation {
        private String formula;
        private Boolean autoCalculate;
    }

    @Data
    public static class WeighscaleDetails {
        private String weighscaleNo;
        private LocalDate calibrationDueDate;
    }

    private static final Pattern FM_COLUMN_KEY = Pattern.compile("(?i)^FM_(\\d+)$");

    /** True when {@code key} is {@code FM_<digits>} (e.g. FM_1, FM_12). */
    public static boolean isFmColumnKey(String key) {
        return key != null && FM_COLUMN_KEY.matcher(key.trim()).matches();
    }

    /** Parses {@code FM_N} → N, or null if not an FM column key. */
    public static Integer parseFmColumnIndex(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Matcher matcher = FM_COLUMN_KEY.matcher(key.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static void putDynamicFmValue(
            BiPredicate<String, Object> knownFieldHandler,
            Supplier<Map<String, Object>> fmMapSupplier,
            String key,
            Object value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (knownFieldHandler.test(key, value)) {
            return;
        }
        if (!isFmColumnKey(key)) {
            return;
        }
        Map<String, Object> fmValues = fmMapSupplier.get();
        String normalizedKey = "FM_" + parseFmColumnIndex(key);
        BigDecimal numeric = coerceBigDecimal(value);
        fmValues.put(normalizedKey, numeric != null ? numeric : value);
    }

    private static BigDecimal coerceBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer coerceInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private abstract static class StrictDeserializer<T> extends JsonDeserializer<T> {
        protected JsonNode readTree(JsonParser parser) throws IOException {
            return parser.getCodec().readTree(parser);
        }

        protected <V> V strictConvert(JsonParser parser, JsonNode node, Class<V> type) throws IOException {
            ObjectCodec codec = parser.getCodec();
            if (codec instanceof ObjectMapper mapper) {
                return mapper.readerFor(type)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .readValue(node);
            }
            return codec.treeToValue(node, type);
        }

        protected <V> V lenientConvert(JsonParser parser, JsonNode node, Class<V> type)
                throws IOException {
            ObjectCodec codec = parser.getCodec();
            if (codec instanceof ObjectMapper mapper) {
                return mapper.readerFor(type)
                        .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .readValue(node);
            }
            return codec.treeToValue(node, type);
        }

        protected String text(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asText();
        }

        protected JsonMappingException unsupported(JsonParser parser, String field, String value) {
            return JsonMappingException.from(parser, "Unsupported " + field + ": " + value);
        }
    }

    public static class DivisionDetailInputDeserializer extends StrictDeserializer<DivisionDetailInput> {
        private static final Set<String> ALLOWED = Set.of(
                "division", "subType", "divisionSubmissionType", "data");

        @Override
        public DivisionDetailInput deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = readTree(parser);
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!ALLOWED.contains(name)) {
                    throw JsonMappingException.from(parser,
                            "Backend-managed or unknown QC division field is not allowed: " + name);
                }
            }
            String division = normalize(text(node, "division"));
            String subType = normalize(text(node, "subType"));
            Class<? extends DivisionDataInput> dataType = switch (effectiveDivision(division, subType)) {
                case "RAW_MATERIAL_REVALIDATION" -> RawMaterialRevalidationData.class;
                case "RAW_MATERIAL_PROCESSING" -> RawMaterialProcessingDataInput.class;
                case "MIXING" -> MixingDataInput.class;
                case "HARDWARE" -> HardwareDataInput.class;
                case "CASTING" -> CastingDataInput.class;
                case "CURING" -> CuringDataInput.class;
                case "DE_CORING" -> DeCoringDataInput.class;
                case "TRIMMING" -> TrimmingDataInput.class;
                case "POST_CURE" -> PostCureDataInput.class;
                case "NDT" -> NdtDataInput.class;
                case "QC", "PROPELLANT_PROPERTIES" -> PropellantDataInput.class;
                case "WEIGHTMENT" -> WeightmentDataInput.class;
                case "STATIC_TEST_FACILITY" -> StaticTestFacilityDataInput.class;
                default -> throw unsupported(parser, "QC division", division);
            };
            DivisionDetailInput result = new DivisionDetailInput();
            result.setDivision(text(node, "division"));
            result.setSubType(text(node, "subType"));
            if (node.hasNonNull("divisionSubmissionType")) {
                result.setDivisionSubmissionType(Status.valueOf(node.get("divisionSubmissionType").asText()));
            }
            if (node.hasNonNull("data")) {
                result.setData(lenientConvert(parser, node.get("data"), dataType));
            }
            return result;
        }
    }

    public static class RawMaterialSectionDeserializer extends StrictDeserializer<RawMaterialSection> {
        @Override
        public RawMaterialSection deserialize(JsonParser p, DeserializationContext c) throws IOException {
            JsonNode n = readTree(p);
            String id = text(n, "sectionId");
            Class<? extends RawMaterialSection> type = switch (id == null ? "" : id) {
                case "feedMaterialDetails" -> FeedMaterialSection.class;
                case "lotDetails" -> LotDetailsSection.class;
                case "blendingDryingParameters" -> BlendingDryingSection.class;
                case "dryingOperationRvd" -> DryingOperationSection.class;
                case "unloadingDispatch" -> UnloadingDispatchSection.class;
                case "particleSizeDistribution" -> ParticleSizeDistributionSection.class;
                case "dryingTrayOven" -> DryingTrayOvenSection.class;
                case "sieving" -> SievingSection.class;
                case "shiftingToMixing" -> ShiftingToMixingSection.class;
                case "blending" -> LiquidBlendingSection.class;
                case "tdiProcessing" -> TdiProcessingSection.class;
                // RMP typed-process display sections (and any future sectionId).
                default -> FlexibleRawMaterialSection.class;
            };
            if (type == FlexibleRawMaterialSection.class) {
                if (id == null || id.isBlank()) {
                    throw unsupported(p, "raw-material sectionId", id);
                }
                return lenientConvert(p, n, type);
            }
            return strictConvert(p, n, type);
        }
    }

    public static class PropellantProcessDeserializer extends StrictDeserializer<PropellantProcess> {
        @Override
        public PropellantProcess deserialize(JsonParser p, DeserializationContext c) throws IOException {
            JsonNode n = readTree(p);
            String process = text(n, "processType");
            Class<? extends PropellantProcess> type = switch (process == null ? "" : process) {
                case "MECHANICAL_PROPERTIES" -> MechanicalPropertiesProcess.class;
                case "INTERFACE_PROPERTIES" -> InterfacePropertiesProcess.class;
                case "SSBR_UBR_BURN_RATE" -> BurnRateProcess.class;
                case "BALLISTIC_EVALUATION" -> BallisticEvaluationProcess.class;
                default -> throw unsupported(p, "propellant processType", process);
            };
            return strictConvert(p, n, type);
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String effectiveDivision(String division, String subType) {
        if ("RAW_MATERIAL".equals(division)) return subType;
        if ("POST_CURE_OPERATION".equals(division)) return "POST_CURE";
        return division;
    }
}
