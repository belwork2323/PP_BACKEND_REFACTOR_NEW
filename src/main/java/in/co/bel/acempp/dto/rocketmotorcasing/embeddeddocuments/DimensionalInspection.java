package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DimensionalInspection {

    private String paramId;

    private String paramName;

    private String side;

    private Integer sequenceNo;

    private SpecifiedDimension specifiedDimension;

    private Readings readings;

    private LooseFlap looseFlap;

    private String remarks;

    private Boolean isWithinRange;
}