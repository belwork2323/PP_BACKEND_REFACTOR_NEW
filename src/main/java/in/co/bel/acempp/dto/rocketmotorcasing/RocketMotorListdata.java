package in.co.bel.acempp.dto.rocketmotorcasing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RocketMotorListdata {
    private List<RocketMotorMetadata> motors;
}
