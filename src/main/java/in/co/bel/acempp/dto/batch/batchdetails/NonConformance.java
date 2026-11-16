package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data
@Builder
public class NonConformance {
    private String type;
    private String process;
    private String parameter;
    private String severity;
}