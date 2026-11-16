package in.co.bel.acempp.dto.auth.resetpswd;

import lombok.Data;

@Data
public class PasswordResetListRequest {
    private String search;
    private String status;    // ALL / PENDING / APPROVED / REJECTED
    private String startDate; // YYYY-MM-DD
    private String endDate;
    private int page     = 1;
    private int pageSize = 10;
}