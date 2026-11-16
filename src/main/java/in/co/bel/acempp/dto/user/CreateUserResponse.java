package in.co.bel.acempp.dto.user;


import in.co.bel.acempp.dto.general.DeptsDto;
import in.co.bel.acempp.dto.general.RolesDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CreateUserResponse {

    private String userId;
    private String username;
    private String user_uuid;
//    private String defaultPassword;
    private RolesDto role;
    private List<DeptsDto> departments;
    private String status;
    private LocalDateTime createdAt;
}
