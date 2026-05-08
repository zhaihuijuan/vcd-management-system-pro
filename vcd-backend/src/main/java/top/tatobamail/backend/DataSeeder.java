package top.tatobamail.backend;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import top.tatobamail.backend.RentalRecords.RentalRecords;
import top.tatobamail.backend.RentalRecords.RentalRecordsRepository;
import top.tatobamail.backend.SalesRecords.SaleRecords;
import top.tatobamail.backend.SalesRecords.SalesRecordsRepository;
import top.tatobamail.backend.Vcd.Vcd;
import top.tatobamail.backend.VcdInventory.VcdInventory;
import top.tatobamail.backend.VcdInventory.VcdInventoryRepository;
import top.tatobamail.backend.Vcd.VcdRepository;
import top.tatobamail.backend.VcdCategories.VcdCategories;
import top.tatobamail.backend.VcdCategories.VcdCategoriesRepository;
import top.tatobamail.backend.customers.customers;
import top.tatobamail.backend.customers.customersRepository;
import top.tatobamail.backend.purchaseRecords.PurchaseRecords;
import top.tatobamail.backend.purchaseRecords.PurchaseRecordsRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final VcdRepository vcdRepository;
    private final VcdCategoriesRepository categoryRepository;
    private final customersRepository customerRepository;
    private final RentalRecordsRepository rentalRepository;
    private final VcdInventoryRepository inventoryRepository;
    private final PurchaseRecordsRepository purchaseRepository;
    private final SalesRecordsRepository salesRepository;

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        // 1. 分别检查各项数据，确保种子数据能按需注入
        if (categoryRepository.count() == 0) {
            seedCategories();
        }
        
        if (vcdRepository.count() == 0) {
            seedVcds();
        }

        if (customerRepository.count() == 0) {
            seedCustomers();
        }

        if (rentalRepository.count() == 0) {
            seedRentals();
        }

        if (inventoryRepository.count() == 0) {
            seedInventory();
        }

        if (purchaseRepository.count() == 0) {
            seedPurchases();
        }

        if (salesRepository.count() == 0) {
            seedSales();
        }

        System.out.println("Data seeding process completed.");
    }

    private List<VcdCategories> seedCategories() {
        String[] catNames = {"动作", "喜剧", "科幻", "爱情", "悬疑", "动画", "恐怖", "纪录片"};
        List<VcdCategories> categories = new ArrayList<>();
        for (String name : catNames) {
            VcdCategories cat = new VcdCategories();
            cat.setName(name);
            cat.setDescription(name + "类影片");
            categories.add(categoryRepository.save(cat));
        }
        return categories;
    }

    private void seedVcds() {
        List<VcdCategories> categories = categoryRepository.findAll();
        String[] titles = {
            "黑客帝国", "泰坦尼克号", "盗梦空间", "星际穿越", "复仇者联盟", 
            "功夫", "大话西游", "英雄", "战狼", "流浪地球", 
            "龙猫", "千与千寻", "速度与激情", "变脸", "教父"
        };
        for (String title : titles) {
            Vcd vcd = new Vcd();
            vcd.setTitle(title);
            vcd.setDirector("导演" + random.nextInt(100));
            vcd.setActor("主演" + random.nextInt(100));
            vcd.setPublishYear(String.valueOf(2000 + random.nextInt(25)));
            vcd.setTime(90 + random.nextInt(60) + "分钟");
            vcd.setRentPrice(5.0f + random.nextInt(10));
            vcd.setSalesPrice(50.0f + random.nextInt(100));
            vcd.setDescription("这是一部经典的" + title + "电影。");
            vcd.setCategory(categories.get(random.nextInt(categories.size())));
            vcdRepository.save(vcd);
        }
    }

    private void seedCustomers() {
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十", "郑十一", "王十二"};
        for (int i = 0; i < 100; i++) {
            customers c = new customers();
            c.setName(names[random.nextInt(names.length)] + (i + 1));
            c.setPhoneNumber("138" + (10000000 + random.nextInt(90000000)));
            customerRepository.save(c);
        }
    }

    private void seedRentals() {
        List<Vcd> vcds = vcdRepository.findAll();
        List<customers> customersList = customerRepository.findAll();
        for (int i = 0; i < 500; i++) {
            RentalRecords r = new RentalRecords();
            r.setVcd(vcds.get(random.nextInt(vcds.size())));
            r.setCustomer(customersList.get(random.nextInt(customersList.size())));
            
            LocalDate rentalDate = LocalDate.now().minusMonths(random.nextInt(12)).minusDays(random.nextInt(28));
            r.setRentalDate(rentalDate);
            r.setExpectedReturnDate(rentalDate.plusDays(7));
            r.setDeposit(50.0f + random.nextInt(100));
            r.setReturned(random.nextBoolean());
            
            rentalRepository.save(r);
        }
    }

    private void seedInventory() {
        List<Vcd> vcds = vcdRepository.findAll();
        Map<Long, Long> activeRentalCountByVcdId = rentalRepository.findAll().stream()
                .filter(rental -> !rental.isReturned())
                .collect(Collectors.groupingBy(rental -> rental.getVcd().getId(), Collectors.counting()));

        for (Vcd vcd : vcds) {
            int rentCount = activeRentalCountByVcdId.getOrDefault(vcd.getId(), 0L).intValue();
            VcdInventory inventory = new VcdInventory();
            inventory.setVcd(vcd);
            inventory.setRentCount(rentCount);
            inventory.setStock(Math.max(rentCount + 2, 6 + random.nextInt(10)));
            inventoryRepository.save(inventory);
        }
    }

    private void seedPurchases() {
        List<Vcd> vcds = vcdRepository.findAll();
        for (Vcd vcd : vcds) {
            PurchaseRecords record = new PurchaseRecords();
            record.setVcd(vcd);
            record.setQuantity(4 + random.nextInt(8));
            record.setPrice(Math.max(10.0f, vcd.getSalesPrice() * 0.45f));
            record.setPurchaseDate(LocalDate.now().minusDays(10 + random.nextInt(120)));
            purchaseRepository.save(record);
        }
    }

    private void seedSales() {
        List<Vcd> vcds = vcdRepository.findAll();
        List<customers> customersList = customerRepository.findAll();

        for (int i = 0; i < 40; i++) {
            SaleRecords record = new SaleRecords();
            record.setVcd(vcds.get(random.nextInt(vcds.size())));
            record.setCustomer(customersList.get(random.nextInt(customersList.size())));
            record.setPrice(40.0f + random.nextInt(80));
            record.setSaleDate(LocalDate.now().minusDays(random.nextInt(90)));
            salesRepository.save(record);
        }
    }
}
