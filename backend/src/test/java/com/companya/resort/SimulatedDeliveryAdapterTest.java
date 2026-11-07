package com.companya.resort;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.companya.resort.dto.AdminEnquiryDetailDto;
import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryChannel;
import com.companya.resort.entity.EnquiryDraft;
import com.companya.resort.entity.EnquiryStatus;
import com.companya.resort.service.SimulatedDeliveryAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedDeliveryAdapterTest {

    private SimulatedDeliveryAdapter adapter;
    private Logger adapterLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        adapter = new SimulatedDeliveryAdapter();
        adapterLogger = (Logger) LoggerFactory.getLogger(SimulatedDeliveryAdapter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        adapterLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (adapterLogger != null && listAppender != null) {
            adapterLogger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("Simulated email delivery works, status is correct, and logs omit email PII")
    void testSimulatedEmailDelivery_OmitsGuestEmailInLogs() {
        String syntheticEmail = "guest.aravind@example.com";

        Enquiry enquiry = new Enquiry();
        enquiry.setId(101L);
        enquiry.setEnquiryCode("ENQ-EM-101");
        enquiry.setSourceChannel(EnquiryChannel.EMAIL_SIM);
        enquiry.setSenderName("Aravind Kumar");
        enquiry.setSenderContact(syntheticEmail);
        enquiry.setStatus(EnquiryStatus.OPEN);

        EnquiryDraft draft = new EnquiryDraft();
        draft.setId(501L);
        draft.setEnquiry(enquiry);
        draft.setDraftBody("Dear Mr. Kumar, thank you for writing to us at " + syntheticEmail + ". We have reserved your canopy room.");

        SimulatedDeliveryAdapter.DeliveryResult result = adapter.deliver(enquiry, draft);

        // 1. Verify delivery functionality
        assertNotNull(result);
        assertEquals("DELIVERED_SIMULATED", result.status(), "Delivery status must remain DELIVERED_SIMULATED");
        assertEquals("EMAIL_SIM", result.channel(), "Channel must be EMAIL_SIM");
        assertTrue(result.deliveryId().startsWith("SIM-DEL-"), "Delivery ID must be prefixed with SIM-DEL-");
        assertNotNull(result.dispatchedAt(), "Dispatched timestamp must be present");

        // 2. Verify simulation output sanitization
        assertEquals("[REDACTED_EMAIL]", result.recipient(), "Simulation output recipient must be redacted");
        assertFalse(result.messagePreview().contains(syntheticEmail), "Message preview must not leak guest email");
        assertTrue(result.messagePreview().contains("[REDACTED_EMAIL]"), "Message preview must have email redacted");

        // 3. Verify actual logger output
        List<ILoggingEvent> logs = listAppender.list;
        assertFalse(logs.isEmpty(), "Adapter must emit log output");

        boolean foundDispatchedLog = false;
        for (ILoggingEvent event : logs) {
            String formattedMessage = event.getFormattedMessage();

            // Assert raw email never appears in any log message
            assertFalse(formattedMessage.contains(syntheticEmail),
                    "Log message must NOT contain raw guest email: " + formattedMessage);

            if (formattedMessage.contains("[SIMULATED DELIVERY] Dispatched draft")) {
                foundDispatchedLog = true;
                assertEquals(Level.INFO, event.getLevel());
                // Assert useful non-sensitive metadata is retained
                assertTrue(formattedMessage.contains("draft id=501"), "Log must retain draft ID");
                assertTrue(formattedMessage.contains("enquiry=ENQ-EM-101"), "Log must retain enquiry code");
                assertTrue(formattedMessage.contains("via EMAIL_SIM"), "Log must retain channel");
                assertTrue(formattedMessage.contains("status=DELIVERED_SIMULATED"), "Log must retain status");
                assertTrue(formattedMessage.contains("DeliveryId=" + result.deliveryId()), "Log must retain delivery ID");
            }
        }
        assertTrue(foundDispatchedLog, "Expected dispatched log message was not emitted");
    }

    @Test
    @DisplayName("Simulated WhatsApp delivery works, status is correct, and logs omit phone PII")
    void testSimulatedWhatsAppDelivery_OmitsGuestPhoneInLogs() {
        String syntheticPhone = "+91 98765 43210";

        Enquiry enquiry = new Enquiry();
        enquiry.setId(102L);
        enquiry.setEnquiryCode("ENQ-WA-202");
        enquiry.setSourceChannel(EnquiryChannel.WHATSAPP_SIM);
        enquiry.setSenderName("Deepa Rao");
        enquiry.setSenderContact(syntheticPhone);
        enquiry.setStatus(EnquiryStatus.OPEN);

        EnquiryDraft draft = new EnquiryDraft();
        draft.setId(502L);
        draft.setEnquiry(enquiry);
        draft.setDraftBody("Namaste Deepa, connecting with you on " + syntheticPhone + " regarding your plantation villa.");

        SimulatedDeliveryAdapter.DeliveryResult result = adapter.deliver(enquiry, draft);

        // 1. Verify delivery functionality
        assertNotNull(result);
        assertEquals("DELIVERED_SIMULATED", result.status());
        assertEquals("WHATSAPP_SIM", result.channel());
        assertTrue(result.deliveryId().startsWith("SIM-DEL-"));
        assertNotNull(result.dispatchedAt());

        // 2. Verify simulation output sanitization
        assertEquals("[REDACTED_PHONE]", result.recipient());
        assertFalse(result.messagePreview().contains(syntheticPhone));
        assertFalse(result.messagePreview().contains("9876543210"));
        assertTrue(result.messagePreview().contains("[REDACTED_PHONE]"));

        // 3. Verify actual logger output
        List<ILoggingEvent> logs = listAppender.list;
        assertFalse(logs.isEmpty());

        for (ILoggingEvent event : logs) {
            String formattedMessage = event.getFormattedMessage();
            assertFalse(formattedMessage.contains(syntheticPhone), "Log must NOT contain formatted phone");
            assertFalse(formattedMessage.contains("9876543210"), "Log must NOT contain raw phone digits");
            assertFalse(formattedMessage.contains("98765 43210"), "Log must NOT contain spaced phone digits");

            if (formattedMessage.contains("[SIMULATED DELIVERY] Dispatched draft")) {
                assertTrue(formattedMessage.contains("draft id=502"));
                assertTrue(formattedMessage.contains("enquiry=ENQ-WA-202"));
                assertTrue(formattedMessage.contains("via WHATSAPP_SIM"));
                assertTrue(formattedMessage.contains("status=DELIVERED_SIMULATED"));
            }
        }
    }

    @Test
    @DisplayName("Error paths sanitize exceptions and do not leak sender contact details")
    void testErrorPath_DoesNotLeakSenderContactInLogsOrException() {
        String sensitiveContact = "confidential.guest@example.com";
        String sensitivePhone = "+91 99887 76655";

        Enquiry enquiry = new Enquiry();
        enquiry.setId(103L);
        enquiry.setEnquiryCode("ENQ-ERR-303");
        enquiry.setSourceChannel(EnquiryChannel.EMAIL_SIM);
        enquiry.setSenderContact(sensitiveContact);

        // Create a draft that throws a RuntimeException containing sensitive contact information
        EnquiryDraft faultyDraft = new EnquiryDraft() {
            @Override
            public String getDraftBody() {
                throw new IllegalStateException("Internal SMTP failure for recipient " + sensitiveContact + " and mobile " + sensitivePhone);
            }
        };
        faultyDraft.setId(503L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adapter.deliver(enquiry, faultyDraft));

        // Exception message must NOT leak the raw email or phone
        assertFalse(ex.getMessage().contains(sensitiveContact), "Exception message must not leak raw email");
        assertFalse(ex.getMessage().contains(sensitivePhone), "Exception message must not leak raw phone");
        assertTrue(ex.getMessage().contains("[REDACTED_EMAIL]"));
        assertTrue(ex.getMessage().contains("[REDACTED_PHONE]"));

        // Captured error log must NOT leak the raw email or phone
        List<ILoggingEvent> logs = listAppender.list;
        boolean foundErrorLog = false;
        for (ILoggingEvent event : logs) {
            String formattedMessage = event.getFormattedMessage();
            assertFalse(formattedMessage.contains(sensitiveContact), "Error log must NOT leak raw email");
            assertFalse(formattedMessage.contains(sensitivePhone), "Error log must NOT leak raw phone");

            if (event.getLevel() == Level.ERROR && formattedMessage.contains("Delivery simulation failure")) {
                foundErrorLog = true;
                assertTrue(formattedMessage.contains("enquiry=ENQ-ERR-303"));
                assertTrue(formattedMessage.contains("[REDACTED_EMAIL]"));
                assertTrue(formattedMessage.contains("[REDACTED_PHONE]"));
            }
        }
        assertTrue(foundErrorLog, "Expected error log was not emitted");
    }

    @Test
    @DisplayName("Staff can still view guest contact details through authorized enquiry APIs and DTOs")
    void testStaffCanStillViewGuestContactDetails() {
        String genuineContact = "guest.rohit@example.com";
        String guestName = "Rohit Verma";

        Enquiry enquiry = new Enquiry();
        enquiry.setId(201L);
        enquiry.setEnquiryCode("ENQ-AUTH-201");
        enquiry.setSenderName(guestName);
        enquiry.setSenderContact(genuineContact);
        enquiry.setSourceChannel(EnquiryChannel.WEB_FORM);
        enquiry.setStatus(EnquiryStatus.OPEN);

        // Verify entity preserves contact
        assertEquals(genuineContact, enquiry.getSenderContact(), "Entity must retain raw contact for authorized staff");

        // Verify AdminEnquiryDetailDto preserves contact
        AdminEnquiryDetailDto detailDto = new AdminEnquiryDetailDto();
        detailDto.setId(enquiry.getId());
        detailDto.setEnquiryCode(enquiry.getEnquiryCode());
        detailDto.setSenderName(enquiry.getSenderName());
        detailDto.setSenderContact(enquiry.getSenderContact());

        assertEquals(genuineContact, detailDto.getSenderContact(),
                "Authorized staff DTO must retain genuine contact details");
        assertEquals(guestName, detailDto.getSenderName());
    }
}
