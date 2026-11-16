package in.co.bel.acempp.entity.batch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAlert {

    @Id
    private String id;

    private String systemManagerId;

    private String type;              // error | warning | info

    private String msg;

    private String batchId;

    private String motorId;

    private String stage;

    private LocalDateTime createdAt;
}