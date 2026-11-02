package com.companya.resort.seeder;

import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "DEMO-ONLY-NOT-REAL-PASS-2026!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedDemoUsers();
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
}
