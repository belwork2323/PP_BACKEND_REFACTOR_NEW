package in.co.bel.acempp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "captcha_store")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Captcha {

    @Id
    private String captchaId;                // matches the old primary key

    private String captchaText;

    private LocalDateTime createdAt;         // still fine as LocalDateTime (converted to Date by driver)

    @Indexed(expireAfter = "0s")
    private Date expiresAt;                  // <-- use Date, not LocalDateTime

    private boolean isUsed;
}