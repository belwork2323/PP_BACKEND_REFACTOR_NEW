package in.co.bel.acempp.dto.rawmaterialpreparation;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimedOperationParameterRowDto {
    private String operation;
    private String setParameter;
    private String actualParameter;
    @JsonAlias("startTime")
    @Field("startTime")
    private LocalDateTime startDateTime;
    @JsonAlias("endTime")
    @Field("endTime")
    private LocalDateTime endDateTime;
}
