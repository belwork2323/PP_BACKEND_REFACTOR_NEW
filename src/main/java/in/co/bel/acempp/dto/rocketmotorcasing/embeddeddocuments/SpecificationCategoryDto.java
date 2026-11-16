package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationCategoryDto {

    private String category;

    private List<SpecificationParameterDto> parameters;
}