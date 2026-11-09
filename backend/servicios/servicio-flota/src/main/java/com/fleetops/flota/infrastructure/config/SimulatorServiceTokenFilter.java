package com.fleetops.flota.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SimulatorServiceTokenFilter extends OncePerRequestFilter {
    private final byte[] token;

    public SimulatorServiceTokenFilter(@Value("${fleetops.simulator.service-token:}") String token) {
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST")
            || !(request.getServletPath().equals("/api/v1/trips/demo")
                || request.getServletPath().matches("/api/v1/trips/[^/]+/complete"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (token.length < 32) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")
            || !MessageDigest.isEqual(token,
                header.substring(7).getBytes(StandardCharsets.UTF_8))) {
            response.setHeader("WWW-Authenticate", "Bearer");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}
