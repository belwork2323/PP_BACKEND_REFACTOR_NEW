package in.co.bel.acempp.dto.rawmaterialpreparation;

import in.co.bel.acempp.enums.Status;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PremixRequest {
    private int premixNo;
    private LocalDate premixDate;
    private String materialType;

    private Status premixSubmissionType;

    /**
     * Read-only on create/update; populated by form-details so the user UI can
     * show APPROVED / WAITING_FOR_APPROVAL locked state for each premix.
     */
    private Status premixSubmissionStatus;

    private List<MaterialProcessRequest> solidProcess;
    private List<MaterialProcessRequest> liquidProcess;
}