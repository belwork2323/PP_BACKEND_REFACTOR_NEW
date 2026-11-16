package in.co.bel.acempp.dto.batch.batchdetails;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.*;

@Data
@Builder
public class TimelineEvent {
    private String event;
    private PersonInfo by;
    private String time;
}