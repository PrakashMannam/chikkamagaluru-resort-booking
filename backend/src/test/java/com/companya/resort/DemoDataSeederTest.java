package com.companya.resort;

import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.entity.RoomCategory;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.RoomCategoryRepository;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.seeder.DemoDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DemoDataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoomCategoryRepository roomCategoryRepository;

    @Mock
    private DailyInventoryRateRepository dailyInventoryRateRepository;

    @Mock
    private com.companya.resort.repository.EnquiryRepository enquiryRepository;

    @Mock
    private com.companya.resort.repository.EnquiryDraftRepository enquiryDraftRepository;

    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void setUp() {
        demoDataSeeder = new DemoDataSeeder(userRepository, passwordEncoder, roomCategoryRepository, dailyInventoryRateRepository, enquiryRepository, enquiryDraftRepository);
    }

    @Test
    void seedDemoInventoryRates_seeds181DaysBothCategories_andIsIdempotent() {
        RoomCategory roomCat = new RoomCategory(1L, "ROOM", "Room", 64, 2, 2, "Desc", "[]");
        RoomCategory villaCat = new RoomCategory(2L, "VILLA", "Villa", 12, 3, 2, "Desc", "[]");

        when(roomCategoryRepository.findByCode("ROOM")).thenReturn(Optional.of(roomCat));
        when(roomCategoryRepository.findByCode("VILLA")).thenReturn(Optional.of(villaCat));
        when(dailyInventoryRateRepository.existsByRoomCategoryIdAndStayDate(anyLong(), any())).thenReturn(false);

        demoDataSeeder.seedDemoInventoryRates();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DailyInventoryRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyInventoryRateRepository).saveAll(captor.capture());

        List<DailyInventoryRate> saved = captor.getValue();
        // 181 days from 2026-11-01 to 2027-04-30 * 2 categories = 362 rows
        assertEquals(362, saved.size());

        // Check 2026-11-01 (Sunday): ROOM is 8500 with allotment 15; VILLA is 15000 with allotment 4
        DailyInventoryRate roomNov1 = saved.stream()
                .filter(r -> r.getRoomCategory().getId().equals(1L) && r.getStayDate().equals(LocalDate.of(2026, 11, 1)))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("8500.00"), roomNov1.getBaseRateInr());
        assertEquals(15, roomNov1.getWebsiteAllotmentTotal());
        assertEquals(0, roomNov1.getBookedCount());

        // Check 2026-11-06 (Friday): ROOM is 10500 with allotment 15; VILLA is 18000 with allotment 4
        DailyInventoryRate roomNov6 = saved.stream()
                .filter(r -> r.getRoomCategory().getId().equals(1L) && r.getStayDate().equals(LocalDate.of(2026, 11, 6)))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("10500.00"), roomNov6.getBaseRateInr());
        assertEquals(15, roomNov6.getWebsiteAllotmentTotal());

        // Check VILLA on 2026-11-14 (Saturday): rate 18000, allotment 2
        DailyInventoryRate villaNov14 = saved.stream()
                .filter(r -> r.getRoomCategory().getId().equals(2L) && r.getStayDate().equals(LocalDate.of(2026, 11, 14)))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("18000.00"), villaNov14.getBaseRateInr());
        assertEquals(2, villaNov14.getWebsiteAllotmentTotal(), "2026-11-14 villa allotment must be 2");

        // Check VILLA on 2026-11-15 (Sunday): rate 15000, allotment 2
        DailyInventoryRate villaNov15 = saved.stream()
                .filter(r -> r.getRoomCategory().getId().equals(2L) && r.getStayDate().equals(LocalDate.of(2026, 11, 15)))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("15000.00"), villaNov15.getBaseRateInr());
        assertEquals(2, villaNov15.getWebsiteAllotmentTotal(), "2026-11-15 villa allotment must be 2");

        // Idempotency: when all records already exist, nothing saved
        reset(dailyInventoryRateRepository);
        when(dailyInventoryRateRepository.existsByRoomCategoryIdAndStayDate(anyLong(), any())).thenReturn(true);
        demoDataSeeder.seedDemoInventoryRates();
        verify(dailyInventoryRateRepository, never()).saveAll(any());
    }

    @Test
    void seedDemoEnquiries_seedsFiveEnquiriesWithDrafts_andIsIdempotent() {
        when(enquiryRepository.existsByEnquiryCode(anyString())).thenReturn(false);

        demoDataSeeder.seedDemoEnquiries();

        ArgumentCaptor<com.companya.resort.entity.EnquiryDraft> draftCaptor = ArgumentCaptor.forClass(com.companya.resort.entity.EnquiryDraft.class);
        verify(enquiryRepository, times(5)).save(any(com.companya.resort.entity.Enquiry.class));
        verify(enquiryDraftRepository, times(5)).save(draftCaptor.capture());

        for (com.companya.resort.entity.EnquiryDraft d : draftCaptor.getAllValues()) {
            assertFalse(d.getDraftBody().toLowerCase().contains("we have held"), "Draft must not claim a room hold");
            assertFalse(d.getDraftBody().toLowerCase().contains("gourmet breakfast"), "Draft must not claim unverified inclusions");
        }

        // Idempotency: when all 5 codes exist, nothing is saved
        reset(enquiryRepository, enquiryDraftRepository);
        when(enquiryRepository.existsByEnquiryCode(anyString())).thenReturn(true);

        demoDataSeeder.seedDemoEnquiries();

        verify(enquiryRepository, never()).save(any());
        verify(enquiryDraftRepository, never()).save(any());
    }
}
