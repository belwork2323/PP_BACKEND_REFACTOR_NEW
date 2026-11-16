package in.co.bel.acempp.entity.batch;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "blockchain_events")
@Data @Builder @NoArgsConstructor
@AllArgsConstructor
public class BlockchainEvent {
    @Id
    private String id;
    private String transactionId;
    private String batchId;
    private String eventType;
    @Indexed(unique = true) // Tells MongoDB to create a unique index on this field
    @NotBlank(message = "Asset ID cannot be blank")
    private String assetId;
    private String eventStatusMessage;
    private String department;
    private String subDepartment;
    private String performedBy;
    private LocalDateTime timestamp;
    private String payloadHash;
    private String channelName;
    private  String division;
}