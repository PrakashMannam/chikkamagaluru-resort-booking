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
    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    @JsonAlias({"raw_message", "message"})
    private String rawMessage;

    @JsonAlias({"expected_guests", "guests_count", "guests"})
    private Integer expectedGuests;

    @JsonAlias({"rooms_count", "rooms", "keys"})
    private Integer roomsCount;

    @JsonAlias({"accommodation_choice", "accommodation"})
    private String accommodationChoice;

    public CreateEnquiryRequest() {
    }

    public CreateEnquiryRequest(String senderName, String senderContact, String rawMessage) {
        this.senderName = senderName;
        this.senderContact = senderContact;
        this.rawMessage = rawMessage;
    }

    public CreateEnquiryRequest(String senderName, String senderContact, String rawMessage, Integer expectedGuests, Integer roomsCount, String accommodationChoice) {
        this.senderName = senderName;
        this.senderContact = senderContact;
        this.rawMessage = rawMessage;
        this.expectedGuests = expectedGuests;
        this.roomsCount = roomsCount;
        this.accommodationChoice = accommodationChoice;
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

    public Integer getExpectedGuests() {
        return expectedGuests;
    }

    public void setExpectedGuests(Integer expectedGuests) {
        this.expectedGuests = expectedGuests;
    }

    public Integer getRoomsCount() {
        return roomsCount;
    }

    public void setRoomsCount(Integer roomsCount) {
        this.roomsCount = roomsCount;
    }

    public String getAccommodationChoice() {
        return accommodationChoice;
    }

    public void setAccommodationChoice(String accommodationChoice) {
        this.accommodationChoice = accommodationChoice;
    }
}
