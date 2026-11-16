package in.co.bel.acempp.dto.user;

import in.co.bel.acempp.dto.general.PaginationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponse {

    private List<UserListItemDto> users;

    private PaginationDto pagination;
}
