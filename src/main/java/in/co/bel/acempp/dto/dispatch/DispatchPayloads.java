package in.co.bel.acempp.dto.dispatch;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.IntegerValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.StringValue;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class DispatchPayloads {

    private DispatchPayloads() {
    }

    public abstract static class StrictPayload {
        @JsonAnySetter
        public void rejectUnknownField(String field, Object ignored) {
            throw new IllegalArgumentException("Unknown Dispatch details field: " + field);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchDetails extends StrictPayload {
        private String stage;
        private LocalDateTime castingDate;
        private LocalDateTime dispatchDate;
        private String dispatchLocation;
        @Valid
        private ClearanceDetails ndtClearance;
        @Valid
        private ClearanceDetails finalAcceptanceCommitteeClearance;
        private List<@Valid PropellantProperty> propellantProperties;
        @Valid
        private WaiverDetails waiverDetails;
        private List<@Valid InspectionRow> rocketMotorInspection;
        private List<@Valid VehicleRow> vehicleDetails;
        private List<@Valid PackingRow> rocketMotorPackingDetails;
        private List<DocumentDto> uploadDispatchPhotos;
        @Valid
        private SafetyClearance safetyClearance;
        @Valid
        private DispatchTeam dispatchTeam;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClearanceDetails extends StrictPayload {
        private String accorded;
        private String momNo;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropellantProperty extends StrictPayload {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String property;
        private String specification;
        private List<@Valid FinalMixResult> finalMixResults;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalMixResult extends StrictPayload {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer finalMixNo;
        @JsonDeserialize(using = StringValue.class)
        private String value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaiverDetails extends StrictPayload {
        private Boolean available;
        private String details;
        private List<String> uploadedDocuments;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectionRow extends StrictPayload {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String parameter;
        private String observation;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleRow extends StrictPayload {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String checkPoint;
        private String observation;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackingRow extends StrictPayload {
        @JsonDeserialize(using = IntegerValue.class)
        private Integer srNo;
        private String nomenclature;
        private String observation;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SafetyClearance extends StrictPayload {
        private String accorded;
        private DocumentDto clearanceCertificate;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchTeam extends StrictPayload {
        private String qaRepresentative;
        private String safetyRepresentative;
        private String projectRepresentative;
    }
}
