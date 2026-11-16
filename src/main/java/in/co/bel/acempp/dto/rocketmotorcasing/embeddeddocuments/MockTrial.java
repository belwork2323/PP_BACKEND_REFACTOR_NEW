package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockTrial {

    @Valid
    private MockTrialBasicDetails basicDetails;

    private List<@Valid MotorLengthMeasurement> motorDimensions;

    private List<@Valid MandrelAssemblyMeasurement> mandrelAssemblyMeasurements;
}
