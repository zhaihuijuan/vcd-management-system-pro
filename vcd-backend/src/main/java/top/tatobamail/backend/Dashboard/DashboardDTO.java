package top.tatobamail.backend.Dashboard;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardDTO {
    private long totalRentals;
    private long totalVcds;
    private long totalCustomers;
    private long overdueCount;

    private List<VcdStat> top10Vcds;
    private List<MonthlyTrend> monthlyTrends;
    private List<CategoryStat> categoryStats;
    private List<CustomerStat> customerFreqDistribution;
    private List<RecentRental> recentRentals;

    @Data
    public static class VcdStat {
        private String title;
        private long count;
    }

    @Data
    public static class MonthlyTrend {
        private String month;
        private long count;
    }

    @Data
    public static class CategoryStat {
        private String name;
        private long count;
    }

    @Data
    public static class CustomerStat {
        private String range; // e.g., "1-2次", "3-5次", etc.
        private long count;
    }

    @Data
    public static class RecentRental {
        private String vcdTitle;
        private String customerName;
        private String rentalDate;
        private String expectedReturnDate;
        private boolean returned;
    }
}
