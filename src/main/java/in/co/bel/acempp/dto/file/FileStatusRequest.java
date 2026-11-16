package in.co.bel.acempp.dto.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileStatusRequest {
    private Long subDepartmentId;
    /** Optional hint for indexed lookup when the file is shown inside a motor row. */
    private String motorId;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> fileId;
    public String getSingleFileId() {
        return (fileId != null && !fileId.isEmpty()) ? fileId.get(0) : null;
    }
}
