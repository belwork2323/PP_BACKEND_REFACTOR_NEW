package in.co.bel.acempp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import in.co.bel.acempp.enums.StageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "file_attachments")
@CompoundIndex(name = "subdept_file_idx", def = "{'subDepartmentId': 1, 'fileId': 1}")
@CompoundIndex(name = "motor_file_idx", def = "{'motorId': 1, 'fileId': 1}", sparse = true)
public class FileAttachments {
    @Id
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String id;
    private Long subDepartmentId;
    @Indexed(unique = true)
    private String fileId;
    private String originalFileName;
    private String storedFileName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String filePath;
    private String mimeType;
    private Long fileSize;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String fileHash;
    private String status;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
    private String formId;
    private boolean readOnly = false;
    private String lotId;
    private String motorCasingId;
    private String motorId;
    private Integer premixNo;
    private StageType stageType;
    private Long divisionId;
    private String division;
    private String subType;
    private String stfTestNo;
    private String bemNo;
    private boolean isDeleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;

}
