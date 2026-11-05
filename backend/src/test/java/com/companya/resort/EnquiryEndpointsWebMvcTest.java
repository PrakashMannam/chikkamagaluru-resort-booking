package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.AdminEnquiryController;
import com.companya.resort.controller.PublicEnquiryController;
import com.companya.resort.dto.AdminEnquiryDetailDto;
import com.companya.resort.dto.AdminEnquirySummaryDto;
import com.companya.resort.dto.ApproveDraftRequest;
import com.companya.resort.dto.CreateEnquiryRequest;
import com.companya.resort.dto.DraftApprovalResponse;
import com.companya.resort.dto.PublicEnquiryResponse;
import com.companya.resort.dto.RouteEnquiryRequest;
import com.companya.resort.dto.SimulateEnquiryRequest;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.entity.EnquiryChannel;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.security.JwtAccessDeniedHandler;
import com.companya.resort.security.JwtAuthenticationEntryPoint;
import com.companya.resort.security.JwtAuthenticationFilter;
import com.companya.resort.security.JwtService;
import com.companya.resort.service.EnquiryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PublicEnquiryController.class, AdminEnquiryController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        JwtService.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "cors.allowed-origin=http://localhost:5173"
})
public class EnquiryEndpointsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnquiryService enquiryService;

    @MockBean
    private UserRepository userRepository;

    private User staffUser;
    private String staffToken;

    @BeforeEach
    void setUp() {
        staffUser = new User(
                "gm@estate.test",
                "hash",
                "Vikram Rao",
                Role.STAFF,
                SpecialtyLane.GM_ESCALATION,
                true
        );
        staffUser.setId(1L);

        staffToken = jwtService.generateToken(staffUser);
        when(userRepository.findByEmail("gm@estate.test")).thenReturn(Optional.of(staffUser));
    }

    @Test
    @DisplayName("Public enquiry submission is accessible without token and returns 200 without leaking internal details")
    void testPublicEnquirySubmission() throws Exception {
        PublicEnquiryResponse response = new PublicEnquiryResponse(
                "ENQ-9999", "OPEN", LocalDateTime.now(), "Thank you! Your enquiry has been received."
        );
        when(enquiryService.createPublicEnquiry(any(CreateEnquiryRequest.class))).thenReturn(response);

        CreateEnquiryRequest request = new CreateEnquiryRequest("John Doe", "john@example.com", "Looking for 2 rooms");

        mockMvc.perform(post("/api/v1/public/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enquiry_code").value("ENQ-9999"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.message").value("Thank you! Your enquiry has been received."))
                .andExpect(jsonPath("$.assigned_to_user_id").doesNotExist())
                .andExpect(jsonPath("$.active_draft").doesNotExist());
    }

    @Test
    @DisplayName("Unauthorized requests to /api/v1/admin/enquiries endpoints are rejected with 401")
    void testUnauthorizedAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/enquiries"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/enquiries/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/enquiries/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sender_name\":\"Test\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/enquiries/1/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"WEDDING\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/enquiries/1/approve-draft"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorized staff can list enquiries with filters")
    void testAuthorizedStaffListEnquiries() throws Exception {
        AdminEnquirySummaryDto summary = new AdminEnquirySummaryDto();
        summary.setId(10L);
        summary.setEnquiryCode("ENQ-1000");
        summary.setCategory("WEDDING");

        when(enquiryService.listEnquiries(any(), eq(EnquiryCategory.WEDDING), any(), any(), any(), any()))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/admin/enquiries")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("category", "WEDDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enquiry_code").value("ENQ-1000"))
                .andExpect(jsonPath("$[0].category").value("WEDDING"));
    }

    @Test
    @DisplayName("Authorized staff can simulate incoming email/WhatsApp enquiry")
    void testAuthorizedStaffSimulateEnquiry() throws Exception {
        AdminEnquiryDetailDto detail = new AdminEnquiryDetailDto();
        detail.setId(11L);
        detail.setEnquiryCode("ENQ-1001");
        detail.setCategory("CORPORATE_OFFSITE");

        when(enquiryService.simulateEnquiry(any(SimulateEnquiryRequest.class))).thenReturn(detail);

        SimulateEnquiryRequest simReq = new SimulateEnquiryRequest(
                EnquiryChannel.WHATSAPP_SIM, "Partner Lead", "+919999988888", "Offsite for 30"
        );

        mockMvc.perform(post("/api/v1/admin/enquiries/simulate")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(simReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enquiry_code").value("ENQ-1001"))
                .andExpect(jsonPath("$.category").value("CORPORATE_OFFSITE"));
    }

    @Test
    @DisplayName("Authorized staff can reroute enquiry")
    void testAuthorizedStaffRouteEnquiry() throws Exception {
        AdminEnquiryDetailDto detail = new AdminEnquiryDetailDto();
        detail.setId(12L);
        detail.setCategory("WEDDING");

        when(enquiryService.routeEnquiry(eq(12L), any(), any(RouteEnquiryRequest.class))).thenReturn(detail);

        RouteEnquiryRequest routeReq = new RouteEnquiryRequest(EnquiryCategory.WEDDING, 3L);

        mockMvc.perform(patch("/api/v1/admin/enquiries/12/route")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(routeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("WEDDING"));
    }

    @Test
    @DisplayName("Authorized staff can approve draft and trigger delivery")
    void testAuthorizedStaffApproveDraft() throws Exception {
        DraftApprovalResponse approvalResp = new DraftApprovalResponse(
                15L, "ENQ-1005", 20L, "REPLIED", "SENT_MANUALLY", "guest@test.com", "EMAIL_SIM", "Approved text", LocalDateTime.now()
        );

        when(enquiryService.approveDraft(eq(15L), any(com.companya.resort.security.UserPrincipal.class), any())).thenReturn(approvalResp);

        ApproveDraftRequest approveReq = new ApproveDraftRequest("Approved text");

        mockMvc.perform(post("/api/v1/admin/enquiries/15/approve-draft")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enquiry_status").value("REPLIED"))
                .andExpect(jsonPath("$.draft_status").value("SENT_MANUALLY"))
                .andExpect(jsonPath("$.delivered_body").value("Approved text"));
    }

    @Test
    @DisplayName("Unauthorized staff trying to access complaint queue or another lane receives 403 Forbidden")
    void testUnauthorizedStaffForbiddenAccess() throws Exception {
        User roomStaff = new User("rooms@estate.test", "hash", "Rooms Staff", Role.STAFF, SpecialtyLane.ROOM, false);
        roomStaff.setId(2L);
        String roomStaffToken = jwtService.generateToken(roomStaff);

        when(enquiryService.listEnquiries(any(), eq(EnquiryCategory.COMPLAINT), any(), any(), any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Complaints restricted to GM"));

        mockMvc.perform(get("/api/v1/admin/enquiries")
                        .header("Authorization", "Bearer " + roomStaffToken)
                        .param("category", "COMPLAINT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        when(enquiryService.getEnquiryDetails(eq(99L), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/admin/enquiries/99")
                        .header("Authorization", "Bearer " + roomStaffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Repeated draft approval request is rejected with 409 Conflict")
    void testRepeatedDraftApprovalConflict() throws Exception {
        when(enquiryService.approveDraft(eq(15L), any(com.companya.resort.security.UserPrincipal.class), any()))
                .thenThrow(new IllegalStateException("No pending draft available for approval on enquiry id: 15"));

        ApproveDraftRequest approveReq = new ApproveDraftRequest("Approved text");

        mockMvc.perform(post("/api/v1/admin/enquiries/15/approve-draft")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("No pending draft available for approval on enquiry id: 15"));
    }

    @Test
    @DisplayName("Unauthorized staff trying to route cross-lane enquiry receives 403 Forbidden")
    void testUnauthorizedStaffRouteEnquiryForbidden() throws Exception {
        User roomStaff = new User("rooms@estate.test", "hash", "Rooms Staff", Role.STAFF, SpecialtyLane.ROOM, false);
        roomStaff.setId(2L);
        String roomStaffToken = jwtService.generateToken(roomStaff);

        when(enquiryService.routeEnquiry(eq(12L), any(com.companya.resort.security.UserPrincipal.class), any(RouteEnquiryRequest.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Staff in lane ROOM cannot route enquiries to WEDDING"));

        RouteEnquiryRequest routeReq = new RouteEnquiryRequest(EnquiryCategory.WEDDING, 3L);

        mockMvc.perform(patch("/api/v1/admin/enquiries/12/route")
                        .header("Authorization", "Bearer " + roomStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(routeReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Staff in lane ROOM cannot route enquiries to WEDDING"));
    }
}
