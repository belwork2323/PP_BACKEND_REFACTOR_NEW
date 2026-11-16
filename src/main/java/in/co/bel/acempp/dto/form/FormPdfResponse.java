package in.co.bel.acempp.dto.form;


import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class FormPdfResponse {
    private String  formId;
    private Long    subDepartmentId;
    private String  fileName;
    private String  fileUrl;
    private String  base64;
    private long    fileSizeBytes;
    private int     pageCount;
    private LocalDateTime generatedAt;
}