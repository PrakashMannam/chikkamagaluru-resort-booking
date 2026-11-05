package com.companya.resort.service;

import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryDraft;
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
        String deliveryId = "SIM-DEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String channel = enquiry.getSourceChannel() != null ? enquiry.getSourceChannel().name() : "UNKNOWN";
        String recipient = enquiry.getSenderContact();
        LocalDateTime now = LocalDateTime.now();

        String body = draft.getDraftBody();
        String preview = (body != null && body.length() > 100) ? body.substring(0, 100) + "..." : body;

        log.info("[SIMULATED DELIVERY] Dispatched draft id={} for enquiry={} via {} to {}. DeliveryId={}",
                draft.getId(), enquiry.getEnquiryCode(), channel, recipient, deliveryId);

        return new DeliveryResult(
                deliveryId,
                channel,
                recipient,
                "DELIVERED_SIMULATED",
                now,
                preview
        );
    }
}
