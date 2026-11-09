package com.companya.resort.security;

import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this(jwtService, null);
    }

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, @Autowired(required = false) UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Claims claims = jwtService.validateAndExtractClaims(token);
                Long userId = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);
                Boolean isGm = claims.get("is_gm", Boolean.class);
                String specialtyLane = claims.get("specialty_lane", String.class);

                // Security check: verify database account is not disabled or deactivated
                String userEmail = null;
                if (userRepository != null) {
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        User dbUser = userOpt.get();
                        if (Boolean.FALSE.equals(dbUser.getIsActive())) {
                            SecurityContextHolder.clearContext();
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Account is disabled or does not exist\"}");
                            return;
                        }
                        userEmail = dbUser.getEmail();
                    }
                }

                UserPrincipal principal = new UserPrincipal(
                        userId,
                        userEmail,
                        role,
                        isGm != null && isGm,
                        specialtyLane
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or expired token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
