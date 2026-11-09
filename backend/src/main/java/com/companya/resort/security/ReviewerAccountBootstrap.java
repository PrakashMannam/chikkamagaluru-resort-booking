package com.companya.resort.security;

import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ReviewerAccountBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReviewerAccountBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${reviewer.account.email:${REVIEWER_EMAIL:}}")
    private String reviewerEmail;

    @Value("${reviewer.account.password:${REVIEWER_PASSWORD:}}")
    private String reviewerPassword;

    @Value("${reviewer.account.name:${REVIEWER_NAME:Estate Staff Reviewer}}")
    private String reviewerName;

    @Value("${reviewer.account.lane:${REVIEWER_LANE:ROOM}}")
    private String reviewerLane;

    @Value("${reviewer.account.is-gm:${REVIEWER_IS_GM:false}}")
    private boolean reviewerIsGm;

    public ReviewerAccountBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (reviewerEmail == null || reviewerEmail.isBlank() ||
            reviewerPassword == null || reviewerPassword.isBlank()) {
            log.info("[AUTH BOOTSTRAP] No REVIEWER_EMAIL / REVIEWER_PASSWORD configured. Skipping reviewer account auto-provisioning.");
            return;
        }

        String normalizedEmail = reviewerEmail.trim().toLowerCase();
        SpecialtyLane lane;
        try {
            lane = SpecialtyLane.valueOf(reviewerLane.trim().toUpperCase());
        } catch (Exception e) {
            lane = SpecialtyLane.ROOM;
        }

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            user = new User(
                    normalizedEmail,
                    passwordEncoder.encode(reviewerPassword.trim()),
                    reviewerName != null && !reviewerName.isBlank() ? reviewerName.trim() : "Estate Staff Reviewer",
                    Role.STAFF,
                    lane,
                    reviewerIsGm,
                    true
            );
            userRepository.save(user);
            log.info("[AUTH BOOTSTRAP] Provisioned new reviewer staff account for email: {}", normalizedEmail);
        } else {
            user.setPasswordHash(passwordEncoder.encode(reviewerPassword.trim()));
            user.setFullName(reviewerName != null && !reviewerName.isBlank() ? reviewerName.trim() : user.getFullName());
            user.setRole(Role.STAFF);
            user.setSpecialtyLane(lane);
            user.setIsGm(reviewerIsGm);
            user.setIsActive(true);
            userRepository.save(user);
            log.info("[AUTH BOOTSTRAP] Updated existing reviewer staff account for email: {} (active=true, isGm={})", normalizedEmail, reviewerIsGm);
        }
    }
}
