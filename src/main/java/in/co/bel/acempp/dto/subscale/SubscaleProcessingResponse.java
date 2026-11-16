package in.co.bel.acempp.dto.subscale;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscaleProcessingResponse {
    private String formId;
    private String batchId;
    private String batchType;
    private Status status;
    private String createdBy;
    private LocalDateTime createdOn;
    private String submittedBy;
    private LocalDateTime submittedOn;
}