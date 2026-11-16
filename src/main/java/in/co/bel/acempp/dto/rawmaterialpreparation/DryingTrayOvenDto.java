package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DryingTrayOvenDto {
    private String ovenType;
    private String ovenNumber;
    private String ovenSetTemperature;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String moisture;
    private String observation;
    /** QA Raw Material Processing only — ignored by manufacturing RMP UI. */
    private String qaRemarks;
}
