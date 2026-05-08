package top.tatobamail.backend.RentalRecords.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.tatobamail.backend.RentalRecords.RentalRecords;
import top.tatobamail.backend.RentalRecords.RentalRecordsRepository;
import top.tatobamail.backend.RentalRecords.RentalRecordsService;
import top.tatobamail.backend.VcdInventory.VcdInventory;
import top.tatobamail.backend.VcdInventory.VcdInventoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RentalRecordsServiceImpl implements RentalRecordsService {

    @Autowired
    private RentalRecordsRepository rentalRecordsRepository;

    @Autowired
    private VcdInventoryRepository vcdInventoryRepository;

    @Override
    @Transactional
    public RentalRecords createRentalRecord(RentalRecords record) {
        if (record.getRentalDate() == null) {
            record.setRentalDate(LocalDate.now());
        }

        VcdInventory inventory = vcdInventoryRepository.findByVcd(record.getVcd())
                .orElseThrow(() -> new NoSuchElementException("VCD\u5e93\u5b58\u4e0d\u5b58\u5728"));

        int availableStock = inventory.getStock() - inventory.getRentCount();
        if (availableStock <= 0) {
            throw new IllegalStateException("VCD\u5e93\u5b58\u4e0d\u8db3\uff0c\u65e0\u6cd5\u79df\u8d41");
        }

        inventory.setRentCount(inventory.getRentCount() + 1);
        vcdInventoryRepository.save(inventory);

        record.setReturned(false);
        return rentalRecordsRepository.save(record);
    }

    @Override
    public RentalRecords getRentalRecordById(Long id) {
        return rentalRecordsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental record not found"));
    }

    @Override
    public List<RentalRecords> getAllRentalRecords() {
        return rentalRecordsRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteRentalRecord(Long id) {
        RentalRecords rental = rentalRecordsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("\u79df\u8d41\u8bb0\u5f55\u4e0d\u5b58\u5728"));

        if (!rental.isReturned()) {
            VcdInventory inventory = vcdInventoryRepository.findByVcd(rental.getVcd())
                    .orElseThrow(() -> new NoSuchElementException("VCD\u5e93\u5b58\u4e0d\u5b58\u5728"));
            int newRentCount = Math.max(0, inventory.getRentCount() - 1);
            inventory.setRentCount(newRentCount);
            vcdInventoryRepository.save(inventory);
        }

        rentalRecordsRepository.deleteById(id);
    }
}