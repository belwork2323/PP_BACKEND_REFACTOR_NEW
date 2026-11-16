package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import lombok.Data;

@Data
public class MotorLengthDetails {
    private MockTrialMeasurementValue heOuterToNeOuter;
    private MockTrialMeasurementValue heInnerToNeInner;
    private MockTrialMeasurementValue neOuterToHeInner;
}
