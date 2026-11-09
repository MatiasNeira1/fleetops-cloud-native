package com.fleetops.simulator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminTokenFilter extends OncePerRequestFilter {
    private final byte[] token;

    public AdminTokenFilter(SimulatorProperties properties) {
        this.token = properties.adminToken() == null ? new byte[0]
            : properties.adminToken().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/v1/simulations");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (token.length < 32) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")
            || !MessageDigest.isEqual(token,
                authorization.substring(7).getBytes(StandardCharsets.UTF_8))) {
            response.setHeader("WWW-Authenticate", "Bearer");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}
