package in.co.bel.acempp.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

        @Id
        private String id;
        // Timestamp
        @Indexed
        private LocalDateTime timestamp;

        // User Information
        @Indexed
        private String userId;
        private String username;
        private String role;

        // Audit Information
        @Indexed
        private String action;


        // Request Metadata
        private String ipAddress;


}
