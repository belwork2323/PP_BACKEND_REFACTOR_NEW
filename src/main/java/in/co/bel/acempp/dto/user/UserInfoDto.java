package in.co.bel.acempp.dto.user;

import in.co.bel.acempp.dto.general.DeptsDto;
import in.co.bel.acempp.dto.general.RolesDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserInfoDto {
    private String userId;
    private String user_uuid;
    private String username;
    private RolesDto role;
    private List<DeptsDto> departments;
    private Boolean mustChangePassword;
}
