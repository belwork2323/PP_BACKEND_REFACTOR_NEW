package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.enums.MaterialType;
import in.co.bel.acempp.enums.RmpFormTemplate;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BatchMaterialDto {
    private Integer srNo;
    private Long materialId;
    private String materialCode;
    private String gradeCode;
    private String gradeName;
    private String materialName;
    private MaterialType materialType;
    private RmpFormTemplate rmpFormTemplate;
    private List<String> lotIds;
    private String make;
    private Double requiredComposition;
    private Double quantityPerPremix;
    private LocalDate revalidationFromDate;
    private LocalDate revalidationToDate;
}