package com.companya.resort.repository;

import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, Long>, JpaSpecificationExecutor<Enquiry> {

    Optional<Enquiry> findByEnquiryCode(String enquiryCode);

    boolean existsByEnquiryCode(String enquiryCode);

    List<Enquiry> findByCategoryOrderByReceivedAtDesc(EnquiryCategory category);

    List<Enquiry> findAllByOrderByReceivedAtDesc();
}
