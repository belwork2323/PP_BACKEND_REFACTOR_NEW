package in.co.bel.acempp.dto.masterdata;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDataRecordDto {
    private Long id;
    private String code;
    private String name;
    private Boolean isActive;

    @Builder.Default
    private Map<String, Object> attributes = new LinkedHashMap<>();

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
