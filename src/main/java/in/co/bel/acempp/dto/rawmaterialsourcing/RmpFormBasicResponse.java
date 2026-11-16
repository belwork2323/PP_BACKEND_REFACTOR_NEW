package in.co.bel.acempp.dto.rawmaterialsourcing;


import in.co.bel.acempp.enums.Status;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RmpFormBasicResponse {
    private String sourcingId;
    private Status status;

}