package in.co.bel.acempp.dto.ndt.motor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalExposureDetailsDto {

    private Integer sectionNumber;

    private String orientation;

    private Integer numberOfExposure;
}