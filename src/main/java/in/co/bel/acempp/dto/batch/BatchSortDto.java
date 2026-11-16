package in.co.bel.acempp.dto.batch;

import lombok.Data;

@Data
public class BatchSortDto {
    private String field;  // createdOn / priority / status
    private String order;  // asc / desc
}
