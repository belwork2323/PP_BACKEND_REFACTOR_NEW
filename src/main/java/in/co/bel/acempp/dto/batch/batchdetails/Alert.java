package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data
@Builder
public class Alert {
    private String type;
    private String message;
    private String time;
}