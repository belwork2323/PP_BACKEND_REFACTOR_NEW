package in.co.bel.acempp.entity.batch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "subscale_article_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscaleArticleMaster implements MasterDataAuditable {

    @Id
    private Long subscaleArticleId;

    private String subscaleArticleCode;

    private String subscaleArticleName;

    /** True when this master row is a BEM article (structured size + unit name). */
    private Boolean isBem;

    /** Positive integer size when {@code isBem}; otherwise null. */
    private Integer bemSize;

    /** Display unit when {@code isBem} (currently {@code Kg}); otherwise null. */
    private String bemUnit;

    @JsonIgnore
    private Integer displayOrder;

    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}