package in.co.bel.acempp.entity.manufacturing;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class WeightmentDetail {
    private String materialCode;
    private String materialName;
    private String gradeCode;
    private String gradeName;
    /** Premix this weighment row belongs to (per material × premix). */
    private Integer premixNo;
    private Double percentage;
    private Double weightTransferred;
    private String containerType;
    private String containerNumber;
    private String weighScaleNumber;
    private LocalDateTime weighingDateTime;
}