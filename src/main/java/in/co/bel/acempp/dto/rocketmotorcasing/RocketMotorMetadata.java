package in.co.bel.acempp.dto.rocketmotorcasing;

import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.RadiographyDetails;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RocketMotorMetadata {

    private String motorCasingId;

    private String motorId;

    private String castingType;

    private String insulationType;
    private RadiographyDetails radiographyDetails;
}