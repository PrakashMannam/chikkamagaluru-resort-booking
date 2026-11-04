package com.companya.resort.seeder;

import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.RoomCategory;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.RoomCategoryRepository;
import com.companya.resort.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "DEMO-ONLY-NOT-REAL-PASS-2026!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomCategoryRepository roomCategoryRepository;
    private final DailyInventoryRateRepository dailyInventoryRateRepository;

    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null, null);
    }

    public DemoDataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RoomCategoryRepository roomCategoryRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roomCategoryRepository = roomCategoryRepository;
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
    }

    @Override
    public void run(String... args) {
        seedDemoUsers();
        seedDemoInventoryRates();
    }

    public void seedDemoUsers() {
        String encodedPassword = passwordEncoder.encode(DEMO_PASSWORD);

        List<User> demoUsers = List.of(
                new User("gm.demo@companya-estate.test", encodedPassword, "Vikram Rao (GM)", Role.STAFF, SpecialtyLane.GM_ESCALATION, true),
                new User("rooms.demo@companya-estate.test", encodedPassword, "Ananya Hegde (Reservations)", Role.STAFF, SpecialtyLane.ROOM, false),
                new User("weddings.demo@companya-estate.test", encodedPassword, "Karthik Urs (Weddings)", Role.STAFF, SpecialtyLane.WEDDING, false),
                new User("offsites.demo@companya-estate.test", encodedPassword, "Meera Nair (Corporate Offsites)", Role.STAFF, SpecialtyLane.OFFSITE, false)
        );

        for (User user : demoUsers) {
            if (!userRepository.existsByEmail(user.getEmail())) {
                userRepository.save(user);
            }
        }
    }

    public void seedDemoInventoryRates() {
        if (roomCategoryRepository == null || dailyInventoryRateRepository == null) {
            return;
        }
        RoomCategory roomCat = roomCategoryRepository.findByCode("ROOM").orElse(null);
        RoomCategory villaCat = roomCategoryRepository.findByCode("VILLA").orElse(null);

        if (roomCat == null || villaCat == null) {
            return;
        }

        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2027, 4, 30);

        List<DailyInventoryRate> toSave = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY);

            // ROOM: base 8500 Sun-Thu, 10500 Fri-Sat, allotment 15. booked_count 0.
            if (!dailyInventoryRateRepository.existsByRoomCategoryIdAndStayDate(roomCat.getId(), date)) {
                BigDecimal roomBase = isWeekend ? new BigDecimal("10500.00") : new BigDecimal("8500.00");
                DailyInventoryRate rate = new DailyInventoryRate();
                rate.setRoomCategory(roomCat);
                rate.setStayDate(date);
                rate.setBaseRateInr(roomBase);
                rate.setWebsiteAllotmentTotal(15);
                rate.setBookedCount(0);
                rate.setVersion(0L);
                toSave.add(rate);
            }

            // VILLA: base 15000 Sun-Thu, 18000 Fri-Sat, allotment 4, except 2026-11-14 and 2026-11-15 where allotment is 2. booked_count 0.
            if (!dailyInventoryRateRepository.existsByRoomCategoryIdAndStayDate(villaCat.getId(), date)) {
                BigDecimal villaBase = isWeekend ? new BigDecimal("18000.00") : new BigDecimal("15000.00");
                int villaAllotment = 4;
                if (date.equals(LocalDate.of(2026, 11, 14)) || date.equals(LocalDate.of(2026, 11, 15))) {
                    villaAllotment = 2;
                }
                DailyInventoryRate rate = new DailyInventoryRate();
                rate.setRoomCategory(villaCat);
                rate.setStayDate(date);
                rate.setBaseRateInr(villaBase);
                rate.setWebsiteAllotmentTotal(villaAllotment);
                rate.setBookedCount(0);
                rate.setVersion(0L);
                toSave.add(rate);
            }
        }

        if (!toSave.isEmpty()) {
            dailyInventoryRateRepository.saveAll(toSave);
        }
    }
}
