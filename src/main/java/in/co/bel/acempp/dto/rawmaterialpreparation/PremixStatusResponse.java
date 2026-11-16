package in.co.bel.acempp.dto.rawmaterialpreparation;

import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremixStatusResponse {

    private int premixNo;

    private String division;

    private String subType;

    private StageType stageType;

    private Status premixSubmissionType;

    private Status premixSubmissionStatus;

    /** true = UI must disable this unit; server will reject edits. Computed on read, not persisted. */
    private Boolean locked;
}
