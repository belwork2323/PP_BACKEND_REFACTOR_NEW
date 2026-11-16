package in.co.bel.acempp.dto.subscale;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BemMotorDto {
    /** Article No / BEM No identity (legacy JSON key: bemMouldNo). */
    @JsonAlias("bemMouldNo")
    private String bemNo;

    private String articleType;
}
