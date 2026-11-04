package com.companya.resort.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestCounts = new ConcurrentHashMap<>();

    private int limitRpm;

    public RateLimitingFilter(@Value("${app.rate-limit.booking-rpm:${RATE_LIMIT_BOOKING_RPM:100}}") int limitRpm) {
        this.limitRpm = limitRpm > 0 ? limitRpm : 100;
    }

    public void setLimitRpm(int limitRpm) {
        this.limitRpm = limitRpm;
    }

    public int getLimitRpm() {
        return limitRpm;
    }

    public void reset() {
        requestCounts.clear();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Enforce rate limiting on public booking and payment endpoints
        boolean isProtectedEndpoint = uri.startsWith("/api/v1/public/bookings")
                || uri.startsWith("/api/v1/public/payments");

        if (!isProtectedEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        if (!tryAcquire(clientIp)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    public boolean tryAcquire(String clientIp) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;

        ConcurrentLinkedQueue<Long> timestamps = requestCounts.computeIfAbsent(clientIp, k -> new ConcurrentLinkedQueue<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
                timestamps.poll();
            }
            if (timestamps.size() < limitRpm) {
                timestamps.add(now);
                return true;
            }
            return false;
        }
    }

    public String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
