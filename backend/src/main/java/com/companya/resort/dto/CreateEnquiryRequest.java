package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateEnquiryRequest {

    @NotBlank(message = "Sender name is required")
    @Size(max = 255)
    @JsonAlias({"sender_name", "name"})
    private String senderName;

    @NotBlank(message = "Sender contact (email or phone) is required")
    @Size(max = 255)
    @JsonAlias({"sender_contact", "contact", "email", "phone"})
    private String senderContact;

    @NotBlank(message = "Message cannot be empty")
    @JsonAlias({"raw_message", "message"})
    private String rawMessage;

    public CreateEnquiryRequest() {
    }

    public CreateEnquiryRequest(String senderName, String senderContact, String rawMessage) {
        this.senderName = senderName;
        this.senderContact = senderContact;
        this.rawMessage = rawMessage;
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
