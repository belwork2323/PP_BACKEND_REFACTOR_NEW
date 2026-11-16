package in.co.bel.acempp.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    private String id;

    private String token;                  // the JWT string itself
    private String username;
    private Date expiryDate;               // java.util.Date
    private Date lastUsed;
}