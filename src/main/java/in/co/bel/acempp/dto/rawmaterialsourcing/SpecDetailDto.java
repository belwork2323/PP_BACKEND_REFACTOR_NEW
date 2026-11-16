package in.co.bel.acempp.dto.rawmaterialsourcing;


import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.user.operations.SpecificationRangeDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecDetailDto {
    private String               specificationCode;
    private String               specificationName;
    private SpecificationRangeDto referenceRange;  // reuse existing
    private String               analysedResult;
    private String               acemQcResult;
    private String               status;           // Passed / Failed / Pending
}
