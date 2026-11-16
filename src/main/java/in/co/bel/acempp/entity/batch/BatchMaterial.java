package in.co.bel.acempp.entity.batch;

import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BatchMaterial {
    private Integer srNo;
    private String materialCode;
    private MaterialGrade grade;
    private String materialName;
    private List<String> lotIds;
    private String make;
    private Double requiredComposition;
    private Double quantityPerPremix;
    private LocalDate revalidationFromDate;
    private LocalDate revalidationToDate;
}