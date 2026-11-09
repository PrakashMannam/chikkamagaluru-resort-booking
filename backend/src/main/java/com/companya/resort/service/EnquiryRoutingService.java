package com.companya.resort.service;

import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EnquiryRoutingService {

    private final UserRepository userRepository;

    public EnquiryRoutingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void assignStaff(Enquiry enquiry) {
        EnquiryCategory category = enquiry.getCategory();
        if (category == null) {
            category = EnquiryCategory.UNSORTED;
            enquiry.setCategory(category);
        }

        User assignedStaff = null;

        switch (category) {
            case ROOM_BOOKING -> assignedStaff = findStaffByLane(SpecialtyLane.ROOM);
            case WEDDING -> assignedStaff = findStaffByLane(SpecialtyLane.WEDDING);
            case CORPORATE_OFFSITE -> assignedStaff = findStaffByLane(SpecialtyLane.OFFSITE);
            case COMPLAINT -> {
                assignedStaff = findGeneralManager();
                enquiry.setGmAlerted(true);
            }
            case UNSORTED -> assignedStaff = findGeneralManager();
        }

        // If the expected staff member was not found, provide deterministic fallback to GM
        if (assignedStaff == null && category != EnquiryCategory.COMPLAINT && category != EnquiryCategory.UNSORTED) {
            assignedStaff = findGeneralManager();
        }

        enquiry.setAssignedToUser(assignedStaff);

        // Complaints always trigger GM alert
        if (category == EnquiryCategory.COMPLAINT) {
            enquiry.setGmAlerted(true);
        }
    }

    public User findStaffByLane(SpecialtyLane lane) {
        return userRepository.findFirstBySpecialtyLaneAndIsActiveTrueOrderByIdAsc(lane)
                .or(() -> userRepository.findFirstBySpecialtyLaneOrderByIdAsc(lane))
                .orElse(null);
    }

    public User findGeneralManager() {
        Optional<User> gm = userRepository.findFirstByIsGmTrueAndIsActiveTrueOrderByIdAsc()
                .or(userRepository::findFirstByIsGmTrueOrderByIdAsc);
        if (gm.isPresent()) {
            return gm.get();
        }
        return userRepository.findFirstBySpecialtyLaneAndIsActiveTrueOrderByIdAsc(SpecialtyLane.GM_ESCALATION)
                .or(() -> userRepository.findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane.GM_ESCALATION))
                .orElse(null);
    }
}
