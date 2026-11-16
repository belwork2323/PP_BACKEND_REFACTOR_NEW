package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisualInspection {

    private Integer srNo;

    private String itemKey;

    private String description;

    private String observations;

    private String remark;

    private DocumentDto media;

    private List<VisualInspectionSubItem> subItems;
}
