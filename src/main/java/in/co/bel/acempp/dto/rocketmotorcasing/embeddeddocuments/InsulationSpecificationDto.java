package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsulationSpecificationDto {

    private String insulationType;

    private List<SpecificationCategoryDto> specifications;

}