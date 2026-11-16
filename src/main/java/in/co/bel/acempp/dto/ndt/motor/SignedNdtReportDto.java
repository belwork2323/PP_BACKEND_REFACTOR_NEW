package in.co.bel.acempp.dto.ndt.motor;

import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedNdtReportDto {
    private String additionalRemarks;

    /** QA section remarks (QC-entered); ignored by manufacturing UI. */
    private String qaRemarks;

    private DocumentDto report;
}
