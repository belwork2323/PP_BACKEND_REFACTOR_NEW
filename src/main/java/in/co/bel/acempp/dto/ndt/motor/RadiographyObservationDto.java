package in.co.bel.acempp.dto.ndt.motor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.qcdivision.QcCertificateListDeserializer;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiographyObservationDto {

    private Integer sectionNumber;

    private String orientation;

    private String observation;

    @JsonDeserialize(using = QcCertificateListDeserializer.class)
    private List<DocumentDto> uploadedImages;
}
