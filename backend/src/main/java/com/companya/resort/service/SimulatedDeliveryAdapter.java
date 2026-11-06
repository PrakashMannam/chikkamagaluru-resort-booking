package com.companya.resort.service;

import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryDraft;
import com.companya.resort.util.PiiRedactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class SimulatedDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatedDeliveryAdapter.class);

    public record DeliveryResult(
            String deliveryId,
            String channel,
            String recipient,
            String status,
            LocalDateTime dispatchedAt,
            String messagePreview
    ) {}

    public DeliveryResult deliver(Enquiry enquiry, EnquiryDraft draft) {
        String enquiryCode = (enquiry != null && enquiry.getEnquiryCode() != null)
                ? enquiry.getEnquiryCode()
                : "UNKNOWN";
        try {
            String deliveryId = "SIM-DEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String channel = (enquiry != null && enquiry.getSourceChannel() != null)
                    ? enquiry.getSourceChannel().name()
                    : "UNKNOWN";
            String rawContact = enquiry != null ? enquiry.getSenderContact() : null;
            String redactedRecipient = PiiRedactor.redact(rawContact);
            LocalDateTime now = LocalDateTime.now();

            String body = draft != null ? draft.getDraftBody() : null;
            String preview = (body != null && body.length() > 100) ? body.substring(0, 100) + "..." : body;
            String redactedPreview = PiiRedactor.redact(preview);

            // Log strictly non-sensitive delivery metadata; omit raw guest contact details entirely
            log.info("[SIMULATED DELIVERY] Dispatched draft id={} for enquiry={} via {} status={} dispatchedAt={}. DeliveryId={}",
                    draft != null ? draft.getId() : null,
                    enquiryCode,
                    channel,
                    "DELIVERED_SIMULATED",
                    now,
                    deliveryId);

            return new DeliveryResult(
                    deliveryId,
                    channel,
                    redactedRecipient,
                    "DELIVERED_SIMULATED",
                    now,
                    redactedPreview
            );
        } catch (Exception e) {
            String sanitizedError = PiiRedactor.redact(e.getMessage());
            log.error("[SIMULATED DELIVERY] Delivery simulation failure for enquiry={}: {}",
                    enquiryCode,
                    sanitizedError);
            throw new RuntimeException("Simulated delivery failed: " + sanitizedError, e);
        }
    }
}
