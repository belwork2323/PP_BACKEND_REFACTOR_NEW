package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDateRangeDto {

    private LocalDateTime from;
    private LocalDateTime to;
}