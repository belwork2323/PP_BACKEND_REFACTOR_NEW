package in.co.bel.acempp.dto.user;

import in.co.bel.acempp.dto.general.DeptsDto;
import in.co.bel.acempp.dto.general.RolesDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListItemDto {

    private String user_uuid;

    private String userId;

    private String username;

    private RolesDto role;

    private List<DeptsDto> department;

    private String status;

    private Boolean mustChangePassword;

    /** Present only when mustChangePassword is true. */
    private String temporaryPassword;
}
