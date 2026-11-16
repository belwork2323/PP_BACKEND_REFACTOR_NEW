package in.co.bel.acempp.dto.rocketmotorcasing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentDto {
    @NotBlank(message = "File ID is mandatory")
    private String fileId;
    private String status;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Boolean readOnly;

}