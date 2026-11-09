package com.companya.resort.util;

import java.util.regex.Pattern;

public class EventAccommodationDetector {

    public enum AccommodationRequirement {
        REQUIRED,
        NOT_REQUIRED,
        UNSPECIFIED
    }

    private static final Pattern NO_ACCOMMODATION_PATTERN = Pattern.compile(
            "(?i)\\b(?:day\\s+event(?:\\s+only)?|daytime\\s+only|day\\s+use(?:\\s+only)?|" +
            "no\\s+overnight(?:\\s+accommodation|\\s+stay)?|" +
            "no\\s+accommodation(?:\\s+required|\\s+needed)?|" +
            "without\\s+accommodation|" +
            "no\\s+stay(?:\\s+required|\\s+needed)?|" +
            "not\\s+requir(?:e|ed|ing)\\s+(?:overnight\\s+)?accommodation|" +
            "no\\s+rooms?\\s+(?:needed|required)|" +
            "not\\s+staying\\s+overnight|not\\s+staying|" +
            "only\\s+venue|venue\\s+only|" +
            "just\\s+the\\s+lawn|just\\s+the\\s+venue|" +
            "conference\\s+hall\\s+only|single\\s+day)\\b"
    );

    private static final Pattern ACCOMMODATION_REQUIRED_PATTERN = Pattern.compile(
            "(?i)\\b(?:\\d+\\s*nights?|\\d+\\s*(?:rooms?|villas?|keys?)|" +
            "overnight\\s+stay|overnight\\s+accommodation|overnight|" +
            "accommodation\\s+required|need\\s+rooms|need\\s+accommodation|" +
            "with\\s+accommodation|with\\s+rooms|staying\\s+for|stay\\s+for|" +
            "nightly\\s+stay)\\b"
    );

    public static AccommodationRequirement detect(String message) {
        if (message == null || message.isBlank()) {
            return AccommodationRequirement.UNSPECIFIED;
        }
        if (NO_ACCOMMODATION_PATTERN.matcher(message).find()) {
            return AccommodationRequirement.NOT_REQUIRED;
        }
        if (ACCOMMODATION_REQUIRED_PATTERN.matcher(message).find()) {
            return AccommodationRequirement.REQUIRED;
        }
        return AccommodationRequirement.UNSPECIFIED;
    }
}
