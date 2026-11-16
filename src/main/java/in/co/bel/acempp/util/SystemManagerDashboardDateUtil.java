package in.co.bel.acempp.util;
import in.co.bel.acempp.dto.dashboard.systemmanagerdashboard.ManagerDateRangeDto;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class SystemManagerDashboardDateUtil {

    public ManagerDateRangeDto resolveDateRange(
            String filterType,
            String startDate,
            String endDate) {

        LocalDate today = LocalDate.now();

        switch (filterType.toLowerCase()) {

            case "day":
                return new ManagerDateRangeDto(
                        today.atStartOfDay(),
                        today.atTime(23, 59, 59));

            case "week":

                LocalDate weekStart =
                        today.with(DayOfWeek.MONDAY);

                LocalDate weekEnd =
                        weekStart.plusDays(6);

                return new ManagerDateRangeDto(
                        weekStart.atStartOfDay(),
                        weekEnd.atTime(23, 59, 59));

            case "month":

                LocalDate monthStart =
                        today.withDayOfMonth(1);

                LocalDate monthEnd =
                        today.withDayOfMonth(today.lengthOfMonth());

                return new ManagerDateRangeDto(
                        monthStart.atStartOfDay(),
                        monthEnd.atTime(23, 59, 59));

            case "custom":

                LocalDate customStart =
                        DateTimeConversionUtil.parseLocalDate(startDate);

                LocalDate customEnd =
                        DateTimeConversionUtil.parseLocalDate(endDate);

                return new ManagerDateRangeDto(
                        customStart.atStartOfDay(),
                        customEnd.atTime(23, 59, 59));

            default:
                throw new IllegalArgumentException(
                        "Invalid filter type");
        }
    }
}
