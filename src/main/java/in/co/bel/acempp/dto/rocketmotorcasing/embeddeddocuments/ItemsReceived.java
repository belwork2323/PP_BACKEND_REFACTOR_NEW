package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemsReceived {

    private String itemType;

    private String description;

    private String dimension;

    private String unit;

    private String receiptStatus;

    private String observations;
}
