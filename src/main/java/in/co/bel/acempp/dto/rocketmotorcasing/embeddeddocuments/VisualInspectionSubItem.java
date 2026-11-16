package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisualInspectionSubItem {

    private String itemKey;

    private String description;

    private String observations;

    private String remark;
}
