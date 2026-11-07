package com.companya.resort.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RateLimitingFilter extends OncePerRequestFilter {

    public static final int MAX_MAP_KEYS = 10_000;
    public static final long WINDOW_MS = 60_000L;
    /** Enquiry submissions use a 10-minute sliding window (design-pack §3: 5 requests per 10 minutes per IP). */
    public static final long ENQUIRY_WINDOW_MS = 600_000L;

    public enum EndpointGroup {
        BOOKING_CREATION(10),
        PAYMENT_MUTATION(30),
        PUBLIC_READ(120),
        /** Public enquiry submission — stricter limit with a longer window (design-pack: 5 per 10 min per IP). */
        ENQUIRY_SUBMISSION(5);

        private final int defaultRpm;

        EndpointGroup(int defaultRpm) {
            this.defaultRpm = defaultRpm;
        }

        public int getDefaultRpm() {
            return defaultRpm;
        }
    }

    public static class IpBucket {
        private long windowStartMs;
        private int count;

        public IpBucket(long windowStartMs, int count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }

        public synchronized boolean tryAcquire(long now, int limit, long windowMs) {
            if (now - windowStartMs >= windowMs) {
                windowStartMs = now;
                count = 0;
            }
            if (count < limit) {
                count++;
                return true;
            }
            return false;
        }

        public synchronized boolean isExpired(long now, long windowMs) {
            return (now - windowStartMs) >= windowMs;
        }

        public synchronized int getCount() {
            return count;
        }

        public synchronized long getWindowStartMs() {
            return windowStartMs;
        }
    }

    private final ConcurrentHashMap<String, IpBucket> requestCounts = new ConcurrentHashMap<>();

    private int bookingCreationLimit;
    private int paymentMutationLimit;
    private int readLimit;
    private int enquirySubmissionLimit;
    private int trustedProxyHops;

    @Autowired
    public RateLimitingFilter(
            @Value("${app.rate-limit.booking-rpm:${RATE_LIMIT_BOOKING_RPM:10}}") int bookingCreationLimit,
            @Value("${app.rate-limit.payment-mutation-rpm:30}") int paymentMutationLimit,
            @Value("${app.rate-limit.read-rpm:120}") int readLimit,
            @Value("${app.rate-limit.enquiry-max-requests:${RATE_LIMIT_ENQUIRY_MAX_REQUESTS:5}}") int enquirySubmissionLimit,
            @Value("${app.rate-limit.trusted-proxy-hops:${TRUSTED_PROXY_HOPS:1}}") int trustedProxyHops
    ) {
        this.bookingCreationLimit = bookingCreationLimit > 0 ? bookingCreationLimit : 10;
        this.paymentMutationLimit = paymentMutationLimit > 0 ? paymentMutationLimit : 30;
        this.readLimit = readLimit > 0 ? readLimit : 120;
        this.enquirySubmissionLimit = enquirySubmissionLimit > 0 ? enquirySubmissionLimit : 5;
        this.trustedProxyHops = trustedProxyHops > 0 ? trustedProxyHops : 1;
    }

    public RateLimitingFilter() {
        this(10, 30, 120, 5, resolveTrustedProxyHops());
    }

    public RateLimitingFilter(int uniformLimit) {
        this(uniformLimit, uniformLimit, uniformLimit, uniformLimit, resolveTrustedProxyHops());
    }

    public RateLimitingFilter(int uniformLimit, int trustedProxyHops) {
        this(uniformLimit, uniformLimit, uniformLimit, uniformLimit, trustedProxyHops);
    }

    public RateLimitingFilter(int bookingCreationLimit, int paymentMutationLimit, int readLimit, int trustedProxyHops) {
        this(bookingCreationLimit, paymentMutationLimit, readLimit, 5, trustedProxyHops);
    }

    private static int resolveTrustedProxyHops() {
        String env = System.getenv("TRUSTED_PROXY_HOPS");
        if (env != null && !env.isBlank()) {
            try {
                int val = Integer.parseInt(env.trim());
                if (val > 0) {
                    return val;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    public void setBookingCreationLimit(int bookingCreationLimit) {
        this.bookingCreationLimit = bookingCreationLimit;
    }

    public int getBookingCreationLimit() {
        return bookingCreationLimit;
    }

    public void setPaymentMutationLimit(int paymentMutationLimit) {
        this.paymentMutationLimit = paymentMutationLimit;
    }

    public int getPaymentMutationLimit() {
        return paymentMutationLimit;
    }

    public void setReadLimit(int readLimit) {
        this.readLimit = readLimit;
    }

    public int getReadLimit() {
        return readLimit;
    }

    public void setEnquirySubmissionLimit(int enquirySubmissionLimit) {
        this.enquirySubmissionLimit = enquirySubmissionLimit;
    }

    public int getEnquirySubmissionLimit() {
        return enquirySubmissionLimit;
    }

    public void setLimitRpm(int limitRpm) {
        this.bookingCreationLimit = limitRpm;
        this.paymentMutationLimit = limitRpm;
        this.readLimit = limitRpm;
        this.enquirySubmissionLimit = limitRpm;
    }

    public int getLimitRpm() {
        return bookingCreationLimit;
    }

    public int getLimitForGroup(EndpointGroup group) {
        if (group == null) {
            return bookingCreationLimit;
        }
        return switch (group) {
            case BOOKING_CREATION -> bookingCreationLimit;
            case PAYMENT_MUTATION -> paymentMutationLimit;
            case PUBLIC_READ -> readLimit;
            case ENQUIRY_SUBMISSION -> enquirySubmissionLimit;
        };
    }

    /**
     * Returns the sliding window duration for the given endpoint group.
     * ENQUIRY_SUBMISSION uses a 10-minute window; all other groups use 1 minute.
     */
    public long getWindowForGroup(EndpointGroup group) {
        if (group == EndpointGroup.ENQUIRY_SUBMISSION) {
            return ENQUIRY_WINDOW_MS;
        }
        return WINDOW_MS;
    }

    public void setTrustedProxyHops(int trustedProxyHops) {
        this.trustedProxyHops = trustedProxyHops > 0 ? trustedProxyHops : 1;
    }

    public int getTrustedProxyHops() {
        return trustedProxyHops;
    }

    public void reset() {
        requestCounts.clear();
    }

    public int getTrackedIpCount() {
        return requestCounts.size();
    }

    public boolean isMapEmpty() {
        return requestCounts.isEmpty();
    }

    public IpBucket getBucket(String clientIp) {
        return getBucket(clientIp, EndpointGroup.BOOKING_CREATION);
    }

    public IpBucket getBucket(String clientIp, EndpointGroup group) {
        if (group == null) {
            group = EndpointGroup.BOOKING_CREATION;
        }
        return requestCounts.get(clientIp + ":" + group.name());
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledCleanup() {
        cleanupExpiredEntries();
    }

    public void cleanupExpiredEntries() {
        cleanupExpiredEntries(System.currentTimeMillis());
    }

    /**
     * Removes expired entries using the longest window duration to ensure
     * enquiry buckets (10-minute window) are not prematurely evicted.
     */
    public void cleanupExpiredEntries(long now) {
        requestCounts.entrySet().removeIf(entry -> {
            long windowMs = entry.getKey().endsWith(":" + EndpointGroup.ENQUIRY_SUBMISSION.name())
                    ? ENQUIRY_WINDOW_MS
                    : WINDOW_MS;
            return entry.getValue().isExpired(now, windowMs);
        });
    }

    public EndpointGroup resolveEndpointGroup(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if ("POST".equalsIgnoreCase(method)) {
            if (uri.equals("/api/v1/public/bookings") || uri.equals("/api/v1/public/bookings/")) {
                return EndpointGroup.BOOKING_CREATION;
            }
            if (uri.startsWith("/api/v1/public/payments/") || uri.endsWith("/cancel-hold")) {
                return EndpointGroup.PAYMENT_MUTATION;
            }
            if (uri.equals("/api/v1/public/enquiries") || uri.equals("/api/v1/public/enquiries/")) {
                return EndpointGroup.ENQUIRY_SUBMISSION;
            }
        } else if ("GET".equalsIgnoreCase(method)) {
            if (uri.startsWith("/api/v1/public/rooms/") || uri.startsWith("/api/v1/public/bookings")) {
                return EndpointGroup.PUBLIC_READ;
            }
        }
        return null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        EndpointGroup group = resolveEndpointGroup(request);
        if (group == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        if (!tryAcquire(clientIp, group, System.currentTimeMillis())) {
            long retryAfterSeconds = getWindowForGroup(group) / 1000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    public boolean tryAcquire(String clientIp) {
        return tryAcquire(clientIp, EndpointGroup.BOOKING_CREATION, System.currentTimeMillis());
    }

    public boolean tryAcquire(String clientIp, long now) {
        return tryAcquire(clientIp, EndpointGroup.BOOKING_CREATION, now);
    }

    public boolean tryAcquire(String clientIp, EndpointGroup group, long now) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "unknown";
        }
        if (group == null) {
            group = EndpointGroup.BOOKING_CREATION;
        }

        String key = clientIp + ":" + group.name();
        long windowMs = getWindowForGroup(group);

        // When map is full and IP+group is new, run cleanup first
        if (!requestCounts.containsKey(key) && requestCounts.size() >= MAX_MAP_KEYS) {
            cleanupExpiredEntries(now);
            if (requestCounts.size() >= MAX_MAP_KEYS) {
                return false;
            }
        }

        int limit = getLimitForGroup(group);
        IpBucket bucket = requestCounts.compute(key, (k, existing) -> {
            if (existing == null) {
                if (requestCounts.size() >= MAX_MAP_KEYS) {
                    return null;
                }
                return new IpBucket(now, 0);
            }
            return existing;
        });

        if (bucket == null) {
            return false;
        }

        return bucket.tryAcquire(now, limit, windowMs);
    }

    public String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            int targetIndex = parts.length - trustedProxyHops;
            if (targetIndex >= 0 && targetIndex < parts.length) {
                return parts[targetIndex].trim();
            }
            // If the header has fewer entries than trustedProxyHops, fall back to remote address
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
