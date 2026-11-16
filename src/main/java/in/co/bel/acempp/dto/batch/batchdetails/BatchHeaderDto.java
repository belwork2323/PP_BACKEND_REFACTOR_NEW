package in.co.bel.acempp.dto.batch.batchdetails;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class BatchHeaderDto {
    private String batchId;
    private String motorId;              // first motor ID
    private String batchType;
    private String priority;
    private String status;               // overall batch status (currentStatus)
    private String facility;             // placeholder or derived from system manager
    private PersonInfo assignedTo;
    private LocalDateTime createdOn;
    private int ageInDays;
    private int overallProgress;         // 0-100
}