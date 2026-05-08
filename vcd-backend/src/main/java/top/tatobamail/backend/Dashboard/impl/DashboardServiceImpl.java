package top.tatobamail.backend.Dashboard.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.tatobamail.backend.Dashboard.DashboardDTO;
import top.tatobamail.backend.Dashboard.DashboardService;
import top.tatobamail.backend.RentalRecords.RentalRecords;
import top.tatobamail.backend.RentalRecords.RentalRecordsRepository;
import top.tatobamail.backend.Vcd.VcdRepository;
import top.tatobamail.backend.customers.customers;
import top.tatobamail.backend.customers.customersRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final RentalRecordsRepository rentalRepository;
    private final VcdRepository vcdRepository;
    private final customersRepository customerRepository;

    @Override
    public DashboardDTO getDashboardStats() {
        System.out.println("Fetching dashboard stats...");
        DashboardDTO dto = new DashboardDTO();
        
        // 1. Basic Stats
        List<customers> allCustomers = customerRepository.findAll();
        long totalCustomers = allCustomers.size();
        long totalRentals = rentalRepository.count();
        long totalVcds = vcdRepository.count();
        
        System.out.println("Total customers: " + totalCustomers);
        
        LocalDate today = LocalDate.now();
        List<RentalRecords> allRentals = rentalRepository.findAll();
        
        long overdueCount = allRentals.stream()
                .filter(r -> !r.isReturned() && r.getExpectedReturnDate().isBefore(today))
                .count();

        dto.setTotalRentals(totalRentals);
        dto.setTotalVcds(totalVcds);
        dto.setTotalCustomers(totalCustomers);
        dto.setOverdueCount(overdueCount);

        // 3. Recent Rentals (Last 10)
        List<RentalRecords> sortedRentals = allRentals.stream()
                .sorted(Comparator.comparing(RentalRecords::getRentalDate).reversed())
                .collect(Collectors.toList());

        List<DashboardDTO.RecentRental> recent = sortedRentals.stream()
                .limit(10)
                .map(r -> {
                    DashboardDTO.RecentRental item = new DashboardDTO.RecentRental();
                    // 这里由于 r.getVcd() 已经在 JPA 上下文中，且我们之前 findAll 过了，
                    // 但 findAll(RentalRecords) 不一定 fetch 了 vcd，
                    // 不过为了简单起见，暂时保留，如果太慢再优化为 fetch join
                    item.setVcdTitle(r.getVcd() != null ? r.getVcd().getTitle() : "Unknown");
                    item.setCustomerName(r.getCustomer() != null ? r.getCustomer().getName() : "Unknown");
                    item.setRentalDate(r.getRentalDate().toString());
                    item.setExpectedReturnDate(r.getExpectedReturnDate().toString());
                    item.setReturned(r.isReturned());
                    return item;
                })
                .collect(Collectors.toList());
        dto.setRecentRentals(recent);

        // 4. Top 10 VCDs
        Map<String, Long> vcdCounts = allRentals.stream()
                .filter(r -> r.getVcd() != null)
                .collect(Collectors.groupingBy(r -> r.getVcd().getTitle(), Collectors.counting()));
        
        List<DashboardDTO.VcdStat> topVcds = vcdCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    DashboardDTO.VcdStat s = new DashboardDTO.VcdStat();
                    s.setTitle(e.getKey());
                    s.setCount(e.getValue());
                    return s;
                })
                .collect(Collectors.toList());
        dto.setTop10Vcds(topVcds);

        // 5. Monthly Trend
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> monthlyCounts = allRentals.stream()
                .collect(Collectors.groupingBy(r -> r.getRentalDate().format(monthFormatter), Collectors.counting()));
        
        List<DashboardDTO.MonthlyTrend> trends = monthlyCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    DashboardDTO.MonthlyTrend t = new DashboardDTO.MonthlyTrend();
                    t.setMonth(e.getKey());
                    t.setCount(e.getValue());
                    return t;
                })
                .collect(Collectors.toList());
        dto.setMonthlyTrends(trends);

        // 6. Category Stats
        Map<String, Long> categoryCounts = allRentals.stream()
                .filter(r -> r.getVcd() != null && r.getVcd().getCategory() != null)
                .collect(Collectors.groupingBy(r -> r.getVcd().getCategory().getName(), Collectors.counting()));
        
        List<DashboardDTO.CategoryStat> catStats = categoryCounts.entrySet().stream()
                .map(e -> {
                    DashboardDTO.CategoryStat s = new DashboardDTO.CategoryStat();
                    s.setName(e.getKey());
                    s.setCount(e.getValue());
                    return s;
                })
                .collect(Collectors.toList());
        dto.setCategoryStats(catStats);

        // 7. Customer Frequency Distribution
        Map<Long, Long> customerRentalCounts = allRentals.stream()
                .filter(r -> r.getCustomer() != null)
                .collect(Collectors.groupingBy(r -> r.getCustomer().getId(), Collectors.counting()));
        
        Map<String, Long> freqDist = new LinkedHashMap<>();
        freqDist.put("1-2次", 0L);
        freqDist.put("3-5次", 0L);
        freqDist.put("6-10次", 0L);
        freqDist.put("10次以上", 0L);

        customerRentalCounts.values().forEach(count -> {
            if (count <= 2) freqDist.merge("1-2次", 1L, Long::sum);
            else if (count <= 5) freqDist.merge("3-5次", 1L, Long::sum);
            else if (count <= 10) freqDist.merge("6-10次", 1L, Long::sum);
            else freqDist.merge("10次以上", 1L, Long::sum);
        });

        List<DashboardDTO.CustomerStat> freqStats = freqDist.entrySet().stream()
                .map(e -> {
                    DashboardDTO.CustomerStat s = new DashboardDTO.CustomerStat();
                    s.setRange(e.getKey());
                    s.setCount(e.getValue());
                    return s;
                })
                .collect(Collectors.toList());
        dto.setCustomerFreqDistribution(freqStats);

        return dto;
    }
}
