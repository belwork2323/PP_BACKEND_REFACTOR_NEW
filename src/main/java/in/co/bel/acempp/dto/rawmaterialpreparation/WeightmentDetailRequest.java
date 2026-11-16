package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WeightmentDetailRequest {
    private String materialCode;
    private String materialName;
    private String gradeCode;
    private String gradeName;
    /** Premix this weighment row belongs to (per material × premix). */
    private Integer premixNo;
    /** Nullable — blank weighment cells must not coerce to 0. */
    private Double percentage;
    /** Nullable — blank weighment cells must not coerce to 0. */
    private Double weightTransferred;
    private String containerType;
    private String containerNumber;
    private String weighScaleNumber;
    private LocalDateTime weighingDateTime;
}
