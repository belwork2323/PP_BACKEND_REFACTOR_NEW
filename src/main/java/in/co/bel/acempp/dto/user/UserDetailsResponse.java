package in.co.bel.acempp.dto.user;


import lombok.Builder;
import lombok.Data;
import in.co.bel.acempp.dto.general.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserDetailsResponse {

    private String userId;

    private String username;

    private RolesDto role;

    private List<DeptsDto> departments;

    private String status;

    private LocalDateTime createdOn;

    private CreatedBy createdBy;

    @Data
    @Builder
    public static class CreatedBy {

        private String username;

        private RolesDto role;
    }
}
