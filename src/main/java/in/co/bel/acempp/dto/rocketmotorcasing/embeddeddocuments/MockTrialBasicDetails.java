package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockTrialBasicDetails {
    private String castingStation;
    private String mandrelId;
    private String bottomCupId;
}
