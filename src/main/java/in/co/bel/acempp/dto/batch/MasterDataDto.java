package in.co.bel.acempp.dto.batch;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDataDto {

    private Long id;

    private String code;

    private String name;
}