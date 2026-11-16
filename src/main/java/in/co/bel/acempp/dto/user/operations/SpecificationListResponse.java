package in.co.bel.acempp.dto.user.operations;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpecificationListResponse {
    private String               materialCode;
    private String grade;
    private List<SpecificationDto> specifications;
}