package in.co.bel.acempp.dto.user.operations;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CastingStationDto {

    private Long stationId;

    private String stationCode;

    private String stationName;

    private String stationType;

    private String status;
}
