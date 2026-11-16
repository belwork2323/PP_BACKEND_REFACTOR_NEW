package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private String category;

    private List<ParameterDto> parameters;

    private Boolean isActive;
}