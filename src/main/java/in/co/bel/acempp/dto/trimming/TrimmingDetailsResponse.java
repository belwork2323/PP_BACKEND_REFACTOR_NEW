package in.co.bel.acempp.dto.trimming;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.trimming.trimmingcreation.MotorDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.*;
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrimmingDetailsResponse extends TrimmingResponse {
    private String batchType;
    private LocalDateTime motorReceivedDateTime;
    private Boolean allMotorsApproved;
    private PersonInfo submittedByDetails;
    private List<MotorDTO> motors;
}