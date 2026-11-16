package in.co.bel.acempp.dto.postcureoperations;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.BatchFormRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PostCureRequest extends BatchFormRequest {

    @NotEmpty
    @JsonDeserialize(using = PostCureMotorListDeserializer.class)
    private List<@Valid PostCureMotor> motors;

}
