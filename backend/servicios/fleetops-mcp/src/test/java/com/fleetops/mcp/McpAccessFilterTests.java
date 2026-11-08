package com.fleetops.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fleetops.mcp.security.McpAccessFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class McpAccessFilterTests {
    private static final String TOKEN = "a-long-local-test-token-with-32-characters";

    @Test
    void acceptsBearerTokenAndRejectsMissingOrWrongToken() throws Exception {
        McpAccessFilter filter = new McpAccessFilter(TOKEN, "");
        MockHttpServletResponse missing = run(filter, null, null);
        assertEquals(401, missing.getStatus());
        assertEquals("Bearer", missing.getHeader("WWW-Authenticate"));
        assertEquals(401, run(filter, "wrong-token", null).getStatus());
        assertEquals(200, run(filter, TOKEN, null).getStatus());
    }

    @Test
    void validatesOriginWhenPresent() throws Exception {
        McpAccessFilter filter = new McpAccessFilter(TOKEN, "http://localhost:5173");
        assertEquals(403, run(filter, TOKEN, "https://other.example").getStatus());
        assertEquals(200, run(filter, TOKEN, "http://localhost:5173").getStatus());
    }

    private MockHttpServletResponse run(McpAccessFilter filter, String token, String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setServletPath("/mcp");
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        if (origin != null) request.addHeader("Origin", origin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        if (response.getStatus() == 200) assertNotNull(chain.getRequest());
        else assertNull(chain.getRequest());
        return response;
    }
}
