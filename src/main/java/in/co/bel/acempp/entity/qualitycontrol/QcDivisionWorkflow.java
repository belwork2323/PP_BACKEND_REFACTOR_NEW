package in.co.bel.acempp.entity.qualitycontrol;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionWorkflow {
    private Long divisionId;
    private String division;
    private Integer sequence;
    private boolean locked;
    private Status status;
    private PersonInfo unlockedBy;
    private LocalDateTime unlockedAt;
}
