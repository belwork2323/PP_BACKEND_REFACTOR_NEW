package in.co.bel.acempp.dto.rawmaterialsourcing;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotSummary {
    private String lotId;
    private String sourcingId;
    private String materialCode;
    private String materialName;
    private MaterialGrade grade;
    private String supplyOrderNo;
    private LocalDate receiptDate;
    private String manufacturerName;
    private Status status;
    private PersonInfo createdBy;
    private LocalDateTime createdOn;
}
