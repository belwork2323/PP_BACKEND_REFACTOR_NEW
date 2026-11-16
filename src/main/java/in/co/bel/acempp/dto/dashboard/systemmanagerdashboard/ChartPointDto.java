package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChartPointDto {

    private String label;

    private Long value;
}