package in.co.bel.acempp.entity;

import in.co.bel.acempp.enums.FileCategory;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "file_upload_config")
public class FileUploadConfig {

    @Id
    private String id;

    @Indexed(unique = true)
    private FileCategory category;      // IMAGE, VIDEO, PDF, GRAPH, etc.

    private List<String> allowedExtensions; // ["jpg", "png"]
    private List<String> allowedMimeTypes;  // ["image/jpeg", "image/png"]
    private Long minSizeBytes;          // e.g., 1024 (1 KB)
    private Long maxSizeBytes;          // e.g., 10485760 (10 MB)
    private Boolean isActive;
}