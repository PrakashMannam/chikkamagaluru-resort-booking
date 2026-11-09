package com.companya.resort.security;

import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewerAccountBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ReviewerAccountBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        bootstrap = new ReviewerAccountBootstrap(userRepository, passwordEncoder);
        lenient().when(passwordEncoder.encode(any())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
    }

    @Test
    @DisplayName("When REVIEWER_EMAIL is missing, bootstrap skips provisioning")
    void skipsWhenNoCredentialsConfigured() {
        ReflectionTestUtils.setField(bootstrap, "reviewerEmail", "");
        ReflectionTestUtils.setField(bootstrap, "reviewerPassword", "");

        bootstrap.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("When REVIEWER_EMAIL and REVIEWER_PASSWORD are provided, provisions new active reviewer account")
    void provisionsNewReviewerAccount() {
        ReflectionTestUtils.setField(bootstrap, "reviewerEmail", "reviewer.staff@companya-estate.test");
        ReflectionTestUtils.setField(bootstrap, "reviewerPassword", "ReviewerSecret2026!");
        ReflectionTestUtils.setField(bootstrap, "reviewerName", "Reviewer Staff");
        ReflectionTestUtils.setField(bootstrap, "reviewerLane", "ROOM");
        ReflectionTestUtils.setField(bootstrap, "reviewerIsGm", false);

        when(userRepository.findByEmail("reviewer.staff@companya-estate.test")).thenReturn(Optional.empty());

        bootstrap.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("reviewer.staff@companya-estate.test", saved.getEmail());
        assertEquals("Reviewer Staff", saved.getFullName());
        assertEquals("encoded_ReviewerSecret2026!", saved.getPasswordHash());
        assertEquals(Role.STAFF, saved.getRole());
        assertEquals(SpecialtyLane.ROOM, saved.getSpecialtyLane());
        assertFalse(saved.getIsGm());
        assertTrue(saved.getIsActive());
    }
}
