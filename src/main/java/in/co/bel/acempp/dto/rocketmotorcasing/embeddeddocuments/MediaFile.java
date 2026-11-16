package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {
    private String fileId;

    private String fileName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String fileUrl;

    private String status;
    private Boolean readOnly;
}
