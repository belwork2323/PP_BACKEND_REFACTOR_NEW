package in.co.bel.acempp.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteUserResponse {

    private String userId;
    private String status;
}
