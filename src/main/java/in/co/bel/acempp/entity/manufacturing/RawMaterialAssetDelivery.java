package in.co.bel.acempp.entity.manufacturing;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialAssetDelivery {
    private String assetId;
    private Map<String, Object> request;
    private Map<String, Object> response;
    private String status;
    private String performedBy;
    private boolean eventRegistered;
    private String eventRegistrationError;
    private int attempts;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime attemptedAt;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime deliveredAt;
}
