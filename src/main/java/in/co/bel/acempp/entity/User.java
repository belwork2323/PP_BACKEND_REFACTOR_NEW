package in.co.bel.acempp.entity;

import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;
    @Indexed(unique = true)
    private String userUUID;
    @Indexed(unique = true)
    private String userId;
    @Indexed(unique = true)
    private String username;
    private String password;
    /** When true, user must change password before accessing the app. */
    private Boolean mustChangePassword;
    /** Prior BCrypt hashes (max 3) — new password must not reuse these. */
    private List<String> passwordHistory;
    /** Plaintext temp password while mustChangePassword is true; cleared after change. */
    private String temporaryPassword;
    private Long roleId;
    private List<Long> subDepartmentIds;
    private Status status;
    private LocalDateTime createdAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime updatedAt;
}


