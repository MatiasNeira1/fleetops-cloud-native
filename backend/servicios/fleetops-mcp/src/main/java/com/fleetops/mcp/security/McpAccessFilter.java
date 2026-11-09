package com.fleetops.mcp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class McpAccessFilter extends OncePerRequestFilter {
    private final byte[] clientToken;
    private final Set<String> allowedOrigins;

    public McpAccessFilter(@Value("${fleetops.mcp.client-token}") String clientToken,
                           @Value("${fleetops.mcp.allowed-origins:}") String allowedOrigins) {
        if (clientToken == null || clientToken.length() < 32 || clientToken.isBlank()) {
            throw new IllegalArgumentException("MCP_CLIENT_TOKEN debe tener al menos 32 caracteres");
        }
        this.clientToken = clientToken.getBytes(StandardCharsets.UTF_8);
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/mcp".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !allowedOrigins.contains(origin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origen no permitido");
            return;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ") ||
            !MessageDigest.isEqual(clientToken,
                authorization.substring("Bearer ".length()).getBytes(StandardCharsets.UTF_8))) {
            response.setHeader("WWW-Authenticate", "Bearer");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Credencial MCP inválida");
            return;
        }
        chain.doFilter(request, response);
    }
}
