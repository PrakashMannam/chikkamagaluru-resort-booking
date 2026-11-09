package com.companya.resort.seeder;

import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.entity.DraftGeneratedBy;
import com.companya.resort.entity.DraftStatus;
import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.entity.EnquiryChannel;
import com.companya.resort.entity.EnquiryDraft;
import com.companya.resort.entity.EnquiryStatus;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.RoomCategory;
import com.companya.resort.entity.RoutingMethod;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.EnquiryDraftRepository;
import com.companya.resort.repository.EnquiryRepository;
import com.companya.resort.repository.RoomCategoryRepository;
import com.companya.resort.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final EnquiryRepository enquiryRepository;
    private final EnquiryDraftRepository enquiryDraftRepository;

    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null, null, null, null);
    }

    public DemoDataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RoomCategoryRepository roomCategoryRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository
    ) {
        this(userRepository, passwordEncoder, roomCategoryRepository, dailyInventoryRateRepository, null, null);
    }

    @Autowired
    public DemoDataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RoomCategoryRepository roomCategoryRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            @Autowired(required = false) EnquiryRepository enquiryRepository,
            @Autowired(required = false) EnquiryDraftRepository enquiryDraftRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roomCategoryRepository = roomCategoryRepository;
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryDraftRepository = enquiryDraftRepository;
    }

    public boolean isProductionEnvironment() {
        String railwayEnv = System.getenv("RAILWAY_ENVIRONMENT");
        String railwayProject = System.getenv("RAILWAY_PROJECT_ID");
        String appEnv = System.getenv("APP_ENV");
        String env = System.getenv("ENVIRONMENT");
        return (railwayEnv != null && !railwayEnv.isBlank()) ||
               (railwayProject != null && !railwayProject.isBlank()) ||
               "production".equalsIgnoreCase(appEnv) ||
               "production".equalsIgnoreCase(env);
    }

    @Override
    public void run(String... args) {
        if (isProductionEnvironment()) {
            return;
        }
        seedDemoUsers();
        seedDemoInventoryRates();
        seedDemoEnquiries();
    }

    public void seedDemoUsers() {
        if (isProductionEnvironment()) {
            return;
        }
        String encodedPassword = passwordEncoder.encode(DEMO_PASSWORD);

        List<User> demoUsers = List.of(
                new User("gm.demo@companya-estate.test", encodedPassword, "Vikram Rao (GM)", Role.STAFF, SpecialtyLane.GM_ESCALATION, true, false),
                new User("rooms.demo@companya-estate.test", encodedPassword, "Ananya Hegde (Reservations)", Role.STAFF, SpecialtyLane.ROOM, false, false),
                new User("weddings.demo@companya-estate.test", encodedPassword, "Karthik Urs (Weddings)", Role.STAFF, SpecialtyLane.WEDDING, false, false),
                new User("offsites.demo@companya-estate.test", encodedPassword, "Meera Nair (Corporate Offsites)", Role.STAFF, SpecialtyLane.OFFSITE, false, false)
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

    public void seedDemoEnquiries() {
        if (enquiryRepository == null || enquiryDraftRepository == null) {
            return;
        }

        User gmUser = userRepository.findFirstByIsGmTrueOrderByIdAsc().orElse(null);
        User weddingStaff = userRepository.findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane.WEDDING).orElse(null);
        User roomStaff = userRepository.findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane.ROOM).orElse(null);
        User offsiteStaff = userRepository.findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane.OFFSITE).orElse(null);

        // 1. Wedding Enquiry (Assigned to Weddings Specialist)
        if (!enquiryRepository.existsByEnquiryCode("ENQ-DEMO-WED1")) {
            Enquiry e = new Enquiry();
            e.setEnquiryCode("ENQ-DEMO-WED1");
            e.setSourceChannel(EnquiryChannel.EMAIL_SIM);
            e.setSenderName("Sunil & Radhika Rao");
            e.setSenderContact("sunil.rao@example.com");
            e.setRawMessage("We are planning a destination wedding in Chikkamagaluru for around 120 guests next year in December 2026. We need banquet lawns, catering, and 30 rooms for 3 nights. Could you please send packages?");
            e.setAiSummary("Destination wedding enquiry for 120 guests, 30 rooms, 3 nights in Dec 2026");
            e.setCategory(EnquiryCategory.WEDDING);
            e.setAiConfidence(new BigDecimal("0.950"));
            e.setRoutingMethod(RoutingMethod.AI_HIGH_CONFIDENCE);
            e.setExtractedGuests(120);
            e.setExtractedRooms(30);
            e.setExtractedNights(3);
            e.setAssignedToUser(weddingStaff != null ? weddingStaff : gmUser);
            e.setGmAlerted(false);
            e.setStatus(EnquiryStatus.OPEN);
            e.setReceivedAt(LocalDateTime.now().minusHours(4));
            enquiryRepository.save(e);

            EnquiryDraft d = new EnquiryDraft();
            d.setEnquiry(e);
            d.setGeneratedBy(DraftGeneratedBy.AI_ROUTER);
            d.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
            d.setDraftBody("Dear Sunil & Radhika,\n\nCongratulations on your upcoming wedding celebration! Our coffee estate offers scenic banquet lawn spaces and bespoke catering for events. Regarding accommodation for your requested dates in December 2026, room availability and packages require personalized coordinator review.\n\nOur wedding coordinator Karthik Urs will connect with you shortly with available dates and customized packages.\n\nWarm regards,\nEstate Weddings Team");
            d.setCreatedAt(LocalDateTime.now().minusHours(4));
            enquiryDraftRepository.save(d);
        }

        // 2. Room Booking Enquiry (Assigned to Reservations Specialist)
        if (!enquiryRepository.existsByEnquiryCode("ENQ-DEMO-ROOM1")) {
            Enquiry e = new Enquiry();
            e.setEnquiryCode("ENQ-DEMO-ROOM1");
            e.setSourceChannel(EnquiryChannel.WHATSAPP_SIM);
            e.setSenderName("Dr. Arvind Swaminathan");
            e.setSenderContact("+919876501234");
            e.setRawMessage("Looking to book a private pool villa for a quiet weekend getaway with my spouse from 2026-11-20 to 2026-11-22. 2 adults, 1 room, 2 nights. Please confirm availability and rates.");
            e.setAiSummary("Private pool villa enquiry for 2 adults, 2 nights (2026-11-20 to 2026-11-22)");
            e.setCategory(EnquiryCategory.ROOM_BOOKING);
            e.setAiConfidence(new BigDecimal("0.920"));
            e.setRoutingMethod(RoutingMethod.AI_HIGH_CONFIDENCE);
            e.setExtractedCheckIn(LocalDate.of(2026, 11, 20));
            e.setExtractedCheckOut(LocalDate.of(2026, 11, 22));
            e.setExtractedGuests(2);
            e.setExtractedRooms(1);
            e.setExtractedNights(2);
            e.setAssignedToUser(roomStaff != null ? roomStaff : gmUser);
            e.setGmAlerted(false);
            e.setStatus(EnquiryStatus.OPEN);
            e.setReceivedAt(LocalDateTime.now().minusHours(3));
            enquiryRepository.save(e);

            EnquiryDraft d = new EnquiryDraft();
            d.setEnquiry(e);
            d.setGeneratedBy(DraftGeneratedBy.AI_ROUTER);
            d.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
            d.setDraftBody("Dear Dr. Swaminathan,\n\nThank you for reaching out to us regarding Private Pool Villa availability for your requested dates (Nov 20–22, 2026). Please note that general enquiries do not hold room allotment until a reservation is formally created.\n\nOur reservations team will confirm current rate plans and availability with you shortly.\n\nWarm regards,\nAnanya Hegde\nReservations Team");
            d.setCreatedAt(LocalDateTime.now().minusHours(3));
            enquiryDraftRepository.save(d);
        }

        // 3. Corporate Offsite Enquiry (Assigned to Offsites Specialist)
        if (!enquiryRepository.existsByEnquiryCode("ENQ-DEMO-CORP1")) {
            Enquiry e = new Enquiry();
            e.setEnquiryCode("ENQ-DEMO-CORP1");
            e.setSourceChannel(EnquiryChannel.EMAIL_SIM);
            e.setSenderName("Kavita Sen");
            e.setSenderContact("kavita.sen@techcorp.io");
            e.setRawMessage("We are organizing our annual leadership team offsite and conference for 35 corporate executives. Looking for conference hall setup, team bonding activities, and 20 rooms for 2 nights.");
            e.setAiSummary("Corporate offsite and conference enquiry for 35 executives, 20 rooms, 2 nights");
            e.setCategory(EnquiryCategory.CORPORATE_OFFSITE);
            e.setAiConfidence(new BigDecimal("0.940"));
            e.setRoutingMethod(RoutingMethod.AI_HIGH_CONFIDENCE);
            e.setExtractedGuests(35);
            e.setExtractedRooms(20);
            e.setExtractedNights(2);
            e.setAssignedToUser(offsiteStaff != null ? offsiteStaff : gmUser);
            e.setGmAlerted(false);
            e.setStatus(EnquiryStatus.OPEN);
            e.setReceivedAt(LocalDateTime.now().minusHours(2));
            enquiryRepository.save(e);

            EnquiryDraft d = new EnquiryDraft();
            d.setEnquiry(e);
            d.setGeneratedBy(DraftGeneratedBy.AI_ROUTER);
            d.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
            d.setDraftBody("Dear Ms. Sen,\n\nThank you for considering our estate for your annual leadership offsite. Regarding meeting space, accommodation, and group activities for your team, our corporate specialist will confirm available facilities, room allotments, and event options with you directly.\n\nOur corporate specialist will review your requirements and follow up regarding suitable options.\n\nWarm regards,\nCorporate Events Team");
            d.setCreatedAt(LocalDateTime.now().minusHours(2));
            enquiryDraftRepository.save(d);
        }

        // 4. Complaint Enquiry (GM Escalation & Alerted)
        if (!enquiryRepository.existsByEnquiryCode("ENQ-DEMO-COMP1")) {
            Enquiry e = new Enquiry();
            e.setEnquiryCode("ENQ-DEMO-COMP1");
            e.setSourceChannel(EnquiryChannel.EMAIL_SIM);
            e.setSenderName("Rajesh Kulkarni");
            e.setSenderContact("rajesh.kulkarni@example.com");
            e.setRawMessage("I am writing to express my extreme disappointment with our recent stay. The villa hot water was completely failing for two days, and the restaurant service was unacceptable. Our anniversary weekend was completely ruined.");
            e.setAiSummary("Guest complaint regarding hot water failure and poor service; GM escalated");
            e.setCategory(EnquiryCategory.COMPLAINT);
            e.setAiConfidence(new BigDecimal("0.910"));
            e.setRoutingMethod(RoutingMethod.AI_HIGH_CONFIDENCE);
            e.setAssignedToUser(gmUser);
            e.setGmAlerted(true);
            e.setStatus(EnquiryStatus.OPEN);
            e.setReceivedAt(LocalDateTime.now().minusHours(1));
            enquiryRepository.save(e);

            EnquiryDraft d = new EnquiryDraft();
            d.setEnquiry(e);
            d.setGeneratedBy(DraftGeneratedBy.AI_ROUTER);
            d.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
            d.setDraftBody("Dear Mr. Kulkarni,\n\nI am deeply sorry to learn about the issues you experienced with hot water and restaurant service during your anniversary visit. This does not reflect the standard of hospitality we strive to provide.\n\nI am personally reviewing our engineering logs and will call you today to discuss how we can make this right.\n\nSincerely,\nVikram Rao\nGeneral Manager");
            d.setCreatedAt(LocalDateTime.now().minusHours(1));
            enquiryDraftRepository.save(d);
        }

        // 5. Low-Confidence / Ambiguous Enquiry (Unsorted Queue)
        if (!enquiryRepository.existsByEnquiryCode("ENQ-DEMO-UNS1")) {
            Enquiry e = new Enquiry();
            e.setEnquiryCode("ENQ-DEMO-UNS1");
            e.setSourceChannel(EnquiryChannel.WHATSAPP_SIM);
            e.setSenderName("Anonymous Guest");
            e.setSenderContact("+919800011122");
            e.setRawMessage("Hello, could someone please call me back regarding details? Thanks.");
            e.setAiSummary("Ambiguous inquiry with low extraction confidence; queued for manual GM triage");
            e.setCategory(EnquiryCategory.UNSORTED);
            e.setAiConfidence(new BigDecimal("0.400"));
            e.setRoutingMethod(RoutingMethod.AI_LOW_CONFIDENCE_GM);
            e.setAssignedToUser(gmUser);
            e.setGmAlerted(false);
            e.setStatus(EnquiryStatus.OPEN);
            e.setReceivedAt(LocalDateTime.now().minusMinutes(30));
            enquiryRepository.save(e);

            EnquiryDraft d = new EnquiryDraft();
            d.setEnquiry(e);
            d.setGeneratedBy(DraftGeneratedBy.AI_ROUTER);
            d.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
            d.setDraftBody("Dear Guest,\n\nThank you for reaching out to Chikkamagaluru Estate. How can we assist you with your upcoming visit or enquiry? Please let us know your preferred time for a call.\n\nWarm regards,\nEstate Front Desk Team");
            d.setCreatedAt(LocalDateTime.now().minusMinutes(30));
            enquiryDraftRepository.save(d);
        }
    }
}
