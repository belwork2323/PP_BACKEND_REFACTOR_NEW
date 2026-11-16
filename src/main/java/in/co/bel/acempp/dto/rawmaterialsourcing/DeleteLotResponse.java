package in.co.bel.acempp.dto.rawmaterialsourcing;

import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteLotResponse {
    private String lotId;
    private Status status;   // "DELETED"
}