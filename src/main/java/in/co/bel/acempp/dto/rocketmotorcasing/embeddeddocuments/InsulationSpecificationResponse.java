package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import lombok.*;

import java.util.List;

@Data
@Builder
public class InsulationSpecificationResponse {

    private String insulationType;

    private List<CategoryDto> specifications;
}