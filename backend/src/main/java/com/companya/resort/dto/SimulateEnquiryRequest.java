package com.companya.resort.dto;

import com.companya.resort.entity.EnquiryChannel;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SimulateEnquiryRequest {

    @NotNull(message = "Source channel is required (EMAIL_SIM or WHATSAPP_SIM)")
    @JsonAlias({"source_channel", "channel"})
    private EnquiryChannel sourceChannel;

    @NotBlank(message = "Sender name is required")
    @JsonAlias({"sender_name", "name"})
    private String senderName;

    @NotBlank(message = "Sender contact is required")
    @JsonAlias({"sender_contact", "contact"})
    private String senderContact;

    @NotBlank(message = "Raw message is required")
    @JsonAlias({"raw_message", "message"})
    private String rawMessage;

    public SimulateEnquiryRequest() {
    }

    public SimulateEnquiryRequest(EnquiryChannel sourceChannel, String senderName, String senderContact, String rawMessage) {
        this.sourceChannel = sourceChannel;
        this.senderName = senderName;
        this.senderContact = senderContact;
        this.rawMessage = rawMessage;
    }

    public EnquiryChannel getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(EnquiryChannel sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderContact() {
        return senderContact;
    }

    public void setSenderContact(String senderContact) {
        this.senderContact = senderContact;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }
}
