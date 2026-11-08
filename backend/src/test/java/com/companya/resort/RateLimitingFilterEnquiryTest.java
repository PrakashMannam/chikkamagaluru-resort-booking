package com.companya.resort;

import com.companya.resort.security.RateLimitingFilter;
import com.companya.resort.security.RateLimitingFilter.EndpointGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targeted tests for public enquiry rate limiting (design-pack §3: 5 requests per 10 min per IP)
 * and regression coverage for existing endpoint groups.
 */
class RateLimitingFilterEnquiryTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        // Use production-default limits: booking=10, payment=30, read=120, enquiry=5, 1 trusted hop
        filter = new RateLimitingFilter(10, 30, 120, 5, 1);
    }

    // -----------------------------------------------------------------------
    // 1. Enquiry POST is matched by the filter
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/public/enquiries resolves to ENQUIRY_SUBMISSION group")
    void resolveEndpointGroup_enquiryPost_returnsEnquirySubmission() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        assertEquals(EndpointGroup.ENQUIRY_SUBMISSION, filter.resolveEndpointGroup(request));
    }

    @Test
    @DisplayName("POST /api/v1/public/enquiries/ (trailing slash) resolves to ENQUIRY_SUBMISSION group")
    void resolveEndpointGroup_enquiryPostTrailingSlash_returnsEnquirySubmission() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/enquiries/");
        assertEquals(EndpointGroup.ENQUIRY_SUBMISSION, filter.resolveEndpointGroup(request));
    }

    @Test
    @DisplayName("GET /api/v1/public/enquiries does NOT match ENQUIRY_SUBMISSION (wrong method)")
    void resolveEndpointGroup_enquiryGet_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/enquiries");
        assertNull(filter.resolveEndpointGroup(request));
    }

    // -----------------------------------------------------------------------
    // 2. Requests below the limit are accepted
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Enquiry requests within the 5-request limit pass through the filter")
    void enquiryRequestsBelowLimit_areAccepted() throws Exception {
        String clientIp = "198.51.100.10";

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
            request.addHeader("X-Forwarded-For", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(200, response.getStatus(), "Request " + (i + 1) + " of 5 should pass");
        }
    }

    // -----------------------------------------------------------------------
    // 3. Request exceeding the limit receives HTTP 429
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("6th enquiry request from same IP within 10-min window receives HTTP 429")
    void enquiryRequestExceedingLimit_returns429() throws Exception {
        String clientIp = "198.51.100.20";

        // First 5 should pass
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
            request.addHeader("X-Forwarded-For", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(200, response.getStatus());
        }

        // 6th request should be rate-limited
        MockHttpServletRequest request6 = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        request6.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse response6 = new MockHttpServletResponse();
        filter.doFilter(request6, response6, (req, res) -> {});
        assertEquals(429, response6.getStatus());
        assertTrue(response6.getContentAsString().contains("Rate limit exceeded"));
        assertEquals("600", response6.getHeader("Retry-After"),
                "Enquiry Retry-After should reflect the 10-minute (600s) window");
    }

    // -----------------------------------------------------------------------
    // 4. Buckets are isolated by client IP
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Different IPs get independent enquiry rate-limit buckets")
    void enquiryBucketsIsolatedByIp() throws Exception {
        // Exhaust IP-A's enquiry quota
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
            request.addHeader("X-Forwarded-For", "10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(200, response.getStatus());
        }

        // IP-A's 6th request is blocked
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        blockedReq.addHeader("X-Forwarded-For", "10.0.0.1");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilter(blockedReq, blockedRes, (req, res) -> {});
        assertEquals(429, blockedRes.getStatus());

        // IP-B's first request should pass
        MockHttpServletRequest ipBRequest = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        ipBRequest.addHeader("X-Forwarded-For", "10.0.0.2");
        MockHttpServletResponse ipBResponse = new MockHttpServletResponse();
        filter.doFilter(ipBRequest, ipBResponse, (req, res) -> {});
        assertEquals(200, ipBResponse.getStatus());
    }

    // -----------------------------------------------------------------------
    // 5. Existing endpoints retain their previous behavior
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Booking, payment, and read endpoints retain their existing rate limits")
    void existingEndpoints_retainPreviousBehavior() throws Exception {
        String clientIp = "203.0.113.50";

        // Booking creation: limit 10
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/bookings");
            request.addHeader("X-Forwarded-For", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(200, response.getStatus(), "Booking request " + (i + 1) + " of 10 should pass");
        }
        // 11th booking request blocked
        MockHttpServletRequest bookingBlocked = new MockHttpServletRequest("POST", "/api/v1/public/bookings");
        bookingBlocked.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse bookingBlockedRes = new MockHttpServletResponse();
        filter.doFilter(bookingBlocked, bookingBlockedRes, (req, res) -> {});
        assertEquals(429, bookingBlockedRes.getStatus());

        // Payment mutation: limit 30
        for (int i = 0; i < 30; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/payments/BK123/mock-webhook");
            request.addHeader("X-Forwarded-For", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(200, response.getStatus(), "Payment request " + (i + 1) + " of 30 should pass");
        }
        // 31st payment request blocked
        MockHttpServletRequest payBlocked = new MockHttpServletRequest("POST", "/api/v1/public/payments/BK123/mock-webhook");
        payBlocked.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse payBlockedRes = new MockHttpServletResponse();
        filter.doFilter(payBlocked, payBlockedRes, (req, res) -> {});
        assertEquals(429, payBlockedRes.getStatus());

        // Public read: limit 120 — just verify a GET still passes (quota for this IP is not yet exhausted)
        MockHttpServletRequest readReq = new MockHttpServletRequest("GET", "/api/v1/public/rooms/availability");
        readReq.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse readRes = new MockHttpServletResponse();
        filter.doFilter(readReq, readRes, (req, res) -> {});
        assertEquals(200, readRes.getStatus());
    }

    @Test
    @DisplayName("Health and admin routes bypass limiting while login is rate limited")
    void unrelatedRoutes_bypassRateLimiter() throws Exception {
        // Health endpoint
        MockHttpServletRequest healthReq = new MockHttpServletRequest("GET", "/health");
        assertNull(filter.resolveEndpointGroup(healthReq));

        // Auth login
        MockHttpServletRequest loginReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertEquals(EndpointGroup.LOGIN_ATTEMPT, filter.resolveEndpointGroup(loginReq));

        // Admin endpoint
        MockHttpServletRequest adminReq = new MockHttpServletRequest("GET", "/api/v1/admin/enquiries");
        assertNull(filter.resolveEndpointGroup(adminReq));
    }

    @Test
    @DisplayName("11th login attempt from one client IP within a minute receives HTTP 429")
    void loginAttemptsExceedingLimit_returns429() throws Exception {
        String clientIp = "198.51.100.55";
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.addHeader("X-Forwarded-For", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest blocked = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        blocked.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blocked, blockedResponse, (req, res) -> {});
        assertEquals(429, blockedResponse.getStatus());
        assertEquals("60", blockedResponse.getHeader("Retry-After"));
    }

    // -----------------------------------------------------------------------
    // 6. Forwarded-IP parsing respects trusted-proxy policy
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Forged leftmost X-Forwarded-For does not bypass enquiry rate limit")
    void forgedXForwardedFor_doesNotBypassEnquiryLimit() throws Exception {
        RateLimitingFilter proxyFilter = new RateLimitingFilter(10, 30, 120, 2, 1); // enquiry limit = 2 for ease of testing

        // Both requests have different forged leftmost IPs but same trusted proxy hop IP
        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        req1.addHeader("X-Forwarded-For", "1.2.3.4, 9.9.9.9");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        proxyFilter.doFilter(req1, res1, (req, res) -> {});
        assertEquals(200, res1.getStatus());

        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        req2.addHeader("X-Forwarded-For", "5.6.7.8, 9.9.9.9");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        proxyFilter.doFilter(req2, res2, (req, res) -> {});
        assertEquals(200, res2.getStatus());

        // 3rd request from same trusted IP gets blocked despite different forged leftmost
        MockHttpServletRequest req3 = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        req3.addHeader("X-Forwarded-For", "99.99.99.99, 9.9.9.9");
        MockHttpServletResponse res3 = new MockHttpServletResponse();
        proxyFilter.doFilter(req3, res3, (req, res) -> {});
        assertEquals(429, res3.getStatus());

        // Different trusted proxy hop IP should still succeed
        MockHttpServletRequest req4 = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        req4.addHeader("X-Forwarded-For", "1.2.3.4, 8.8.8.8");
        MockHttpServletResponse res4 = new MockHttpServletResponse();
        proxyFilter.doFilter(req4, res4, (req, res) -> {});
        assertEquals(200, res4.getStatus());
    }

    @Test
    @DisplayName("No X-Forwarded-For header falls back to remoteAddr for enquiry limiting")
    void noXForwardedFor_fallsBackToRemoteAddr() throws Exception {
        RateLimitingFilter strictFilter = new RateLimitingFilter(10, 30, 120, 1, 1); // enquiry limit = 1

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        req1.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        strictFilter.doFilter(req1, res1, (req, res) -> {});
        assertEquals(200, res1.getStatus());

        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        req2.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        strictFilter.doFilter(req2, res2, (req, res) -> {});
        assertEquals(429, res2.getStatus());
    }

    // -----------------------------------------------------------------------
    // 7. Enquiry bucket uses 10-minute window, booking bucket uses 1-minute window
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Enquiry bucket uses 10-minute window; booking bucket uses 1-minute window")
    void enquiryWindow_is10Minutes_bookingWindow_is1Minute() {
        assertEquals(600_000L, filter.getWindowForGroup(EndpointGroup.ENQUIRY_SUBMISSION));
        assertEquals(60_000L, filter.getWindowForGroup(EndpointGroup.BOOKING_CREATION));
        assertEquals(60_000L, filter.getWindowForGroup(EndpointGroup.PAYMENT_MUTATION));
        assertEquals(60_000L, filter.getWindowForGroup(EndpointGroup.PUBLIC_READ));
    }

    @Test
    @DisplayName("Enquiry bucket resets after 10-minute window passes")
    void enquiryBucket_resetsAfterWindowPasses() {
        long now = System.currentTimeMillis();
        String clientIp = "198.51.100.30";

        // Exhaust the 5-request limit
        for (int i = 0; i < 5; i++) {
            assertTrue(filter.tryAcquire(clientIp, EndpointGroup.ENQUIRY_SUBMISSION, now));
        }
        assertFalse(filter.tryAcquire(clientIp, EndpointGroup.ENQUIRY_SUBMISSION, now),
                "6th request within window should be rejected");

        // 9 minutes later: still blocked
        assertFalse(filter.tryAcquire(clientIp, EndpointGroup.ENQUIRY_SUBMISSION, now + 540_000L),
                "Request at 9 minutes should still be blocked");

        // 10 minutes later: window resets, request passes
        assertTrue(filter.tryAcquire(clientIp, EndpointGroup.ENQUIRY_SUBMISSION, now + 600_001L),
                "Request after 10-minute window should pass");
    }

    @Test
    @DisplayName("Booking bucket resets after 1-minute window, enquiry bucket still blocked")
    void bookingResets_enquiryStillBlocked_withinWindow() {
        long now = System.currentTimeMillis();
        String clientIp = "198.51.100.40";

        // Exhaust both quotas
        for (int i = 0; i < 10; i++) {
            filter.tryAcquire(clientIp, EndpointGroup.BOOKING_CREATION, now);
        }
        for (int i = 0; i < 5; i++) {
            filter.tryAcquire(clientIp, EndpointGroup.ENQUIRY_SUBMISSION, now);
        }

        // Both blocked at now
        assertFalse(filter.tryAcquire(clientIp, EndpointGroup.BOOKING_CREATION, now));
        assertFalse(filter.tryAcquire(clientIp, EndpointGroup.ENQUIRY_SUBMISSION, now));

        // After 61 seconds: booking resets, enquiry still blocked
        long afterOneMinute = now + 61_000L;
        assertTrue(filter.tryAcquire(clientIp, EndpointGroup.BOOKING_CREATION, afterOneMinute),
                "Booking should reset after 1-minute window");
        assertFalse(filter.tryAcquire(clientIp, EndpointGroup.ENQUIRY_SUBMISSION, afterOneMinute),
                "Enquiry should still be blocked — 10-minute window hasn't passed");
    }

    // -----------------------------------------------------------------------
    // 8. Enquiry and booking buckets are independent for the same IP
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Enquiry and booking rate-limit buckets are independent for the same IP")
    void enquiryAndBookingBuckets_areIndependent() throws Exception {
        String clientIp = "203.0.113.99";

        // Exhaust enquiry quota (5)
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
            request.addHeader("X-Forwarded-For", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(200, response.getStatus());
        }

        // Enquiry is now blocked
        MockHttpServletRequest enquiryBlocked = new MockHttpServletRequest("POST", "/api/v1/public/enquiries");
        enquiryBlocked.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse enquiryBlockedRes = new MockHttpServletResponse();
        filter.doFilter(enquiryBlocked, enquiryBlockedRes, (req, res) -> {});
        assertEquals(429, enquiryBlockedRes.getStatus());

        // But booking creation from same IP should still work
        MockHttpServletRequest bookingReq = new MockHttpServletRequest("POST", "/api/v1/public/bookings");
        bookingReq.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse bookingRes = new MockHttpServletResponse();
        filter.doFilter(bookingReq, bookingRes, (req, res) -> {});
        assertEquals(200, bookingRes.getStatus());

        // And public read from same IP should still work
        MockHttpServletRequest readReq = new MockHttpServletRequest("GET", "/api/v1/public/rooms/availability");
        readReq.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse readRes = new MockHttpServletResponse();
        filter.doFilter(readReq, readRes, (req, res) -> {});
        assertEquals(200, readRes.getStatus());
    }

    // -----------------------------------------------------------------------
    // 9. Cleanup handles mixed-window entries correctly
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Cleanup expires booking entries after 1 min but retains enquiry entries until 10 min")
    void cleanup_respectsGroupSpecificWindows() {
        long now = System.currentTimeMillis();

        filter.tryAcquire("10.0.0.1", EndpointGroup.BOOKING_CREATION, now);
        filter.tryAcquire("10.0.0.1", EndpointGroup.ENQUIRY_SUBMISSION, now);
        assertEquals(2, filter.getTrackedIpCount());

        // After 61s: booking entry expires, enquiry entry survives
        filter.cleanupExpiredEntries(now + 61_000L);
        assertEquals(1, filter.getTrackedIpCount());
        assertNull(filter.getBucket("10.0.0.1", EndpointGroup.BOOKING_CREATION));
        assertNotNull(filter.getBucket("10.0.0.1", EndpointGroup.ENQUIRY_SUBMISSION));

        // After 601s: enquiry entry also expires
        filter.cleanupExpiredEntries(now + 601_000L);
        assertEquals(0, filter.getTrackedIpCount());
        assertTrue(filter.isMapEmpty());
    }
}
