package in.co.bel.acempp.dto.general;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {

    private Integer page;

    private Integer pageSize;

    private Long totalRecords;

    private Integer totalPages;
}
