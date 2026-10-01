package net.arnx.jsonic.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RESTCsrfTest {
    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/memo.json");
        request.setScheme("https");
        request.setServerName("example.com");
        request.setServerPort(443);
        request.setContentType("application/json");
        return request;
    }

    @Test
    void checksSchemeHostPortAndRejectsAmbiguousOrigins() {
        for (String origin : new String[]{"https://example.com", "https://EXAMPLE.com:443"}) {
            var request = request();
            request.addHeader("Origin", origin);
            assertTrue(RESTServlet.isCsrfSafeRequest(request), origin);
        }
        for (String origin : new String[]{"http://example.com", "https://example.com:444",
                "https://example.com.attacker.test", "https://example.com@attacker.test",
                "https://attacker@example.com", "https://example.com?x", "https://example.com#x",
                "https://example.com/path", "null", "", "https://example.com https://attacker.test"}) {
            var request = request();
            request.addHeader("Origin", origin);
            request.addHeader("X-Requested-With", "XMLHttpRequest");
            assertFalse(RESTServlet.isCsrfSafeRequest(request), origin);
        }
        var duplicate = request();
        duplicate.addHeader("Origin", "https://example.com");
        duplicate.addHeader("Origin", "https://attacker.test");
        assertFalse(RESTServlet.isCsrfSafeRequest(duplicate));
    }

    @Test
    void rejectsCrossSiteMetadataAndUnverifiedForms() {
        var request = request();
        request.addHeader("Sec-Fetch-Site", "cross-site");
        assertFalse(RESTServlet.isCsrfSafeRequest(request));
        request = request();
        request.setContentType(null);
        assertFalse(RESTServlet.isCsrfSafeRequest(request));
        request.addHeader("Forwarded", "proto=https;host=attacker.test");
        request.addHeader("X-Forwarded-Host", "attacker.test");
        request.addHeader("Origin", "https://attacker.test");
        assertFalse(RESTServlet.isCsrfSafeRequest(request));
    }
}
