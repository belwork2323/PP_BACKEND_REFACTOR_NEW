package in.co.bel.acempp.dto.masterdata.insulation;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.CategoryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsulationSpecRecordDto {
    private Long insulationSpecId;
    private String code;
    private String insulationType;
    private Boolean isActive;
    @Builder.Default
    private List<CategoryDto> specifications = new ArrayList<>();

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
