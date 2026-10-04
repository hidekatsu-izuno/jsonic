package net.arnx.jsonic.web;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class GatewayFilterTest {
    @Test
    public void authorizesTheIncludeTargetAndRejectsNamedIncludes() throws Exception {
        for (boolean authorized : new boolean[]{false, true}) {
            GatewayFilter filter = new GatewayFilter();
            MockFilterConfig config = new MockFilterConfig();
            config.addInitParameter("config", "{\"/admin/.*\":{\"access\":[\"admin\"]}}");
            filter.init(config);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open");
            request.setServletPath("/open");
            request.setDispatcherType(DispatcherType.INCLUDE);
            if (authorized) request.addUserRole("admin");
            MockHttpServletResponse response = new MockHttpServletResponse();
            try {
                // A named include has no target path, even for an authorized caller.
                filter.doFilter(request, response, (req, res) -> fail("Named include was allowed"));
                request.setAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH, "/admin");
                request.setAttribute(RequestDispatcher.INCLUDE_PATH_INFO, "/secret");
                filter.doFilter(request, response, (req, res) -> res.getWriter().write("allowed"));
                assertEquals(authorized ? "allowed" : "", response.getContentAsString());
                assertEquals(200, response.getStatus());
            } finally {
                filter.destroy();
            }
        }
    }

    @Test
    public void rechecksRolesWhenAnExistingRequestIsForwarded() throws Exception {
        for (boolean authorized : new boolean[]{false, true}) {
            GatewayFilter filter = new GatewayFilter();
            MockFilterConfig config = new MockFilterConfig();
            config.addInitParameter("config", "{\"/admin/.*\":{\"access\":[\"admin\"]}}");
            filter.init(config);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jump");
            request.setServletPath("/jump");
            if (authorized) request.addUserRole("admin");
            MockHttpServletResponse response = new MockHttpServletResponse();
            try {
                filter.doFilter(request, response, (req, res) -> {
                    assertNotNull(req.getAttribute(GatewayFilter.GATEWAY_KEY));
                    request.setServletPath("/admin");
                    request.setPathInfo("/secret");
                    request.setDispatcherType(jakarta.servlet.DispatcherType.FORWARD);
                    filter.doFilter(request, response,
                            (forwardedRequest, forwardedResponse) -> forwardedResponse.getWriter().write("allowed"));
                });
                assertEquals(authorized ? 200 : 403, response.getStatus());
                assertEquals(authorized ? "allowed" : "", response.getContentAsString());
            } finally {
                filter.destroy();
            }
        }
    }

    @Test
    public void allowsAuthorizedUserOnDecodedPath() throws Exception {
        GatewayFilter filter = new GatewayFilter();
        MockFilterConfig config = new MockFilterConfig();
        config.addInitParameter("config", "{\"/admin/.*\":{\"access\":[\"admin\"]}}");
        filter.init(config);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/%61dmin/secret");
        request.setContextPath("/app");
        request.setServletPath("/admin");
        request.setPathInfo("/secret");
        request.addUserRole("admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            filter.doFilter(request, response, (req, res) -> res.getWriter().write("allowed"));
            assertEquals(200, response.getStatus());
            assertEquals("allowed", response.getContentAsString());
        } finally {
            filter.destroy();
        }
    }

    @Test
    public void compressesJakartaServletResponse() throws Exception {
        GatewayFilter filter = new GatewayFilter();
        MockFilterConfig config = new MockFilterConfig();
        config.addInitParameter("config", "{\"compression\":true,\"encoding\":\"UTF-8\"}");
        filter.init(config);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/memo.json");
        request.addHeader("Accept-Encoding", "gzip");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String body = "{\"title\":\"日本語のメモ\"}";
        try {
            filter.doFilter(request, response, (req, res) -> {
                assertTrue(res.getOutputStream().isReady());
                res.getWriter().write(body);
            });
            assertEquals("gzip", response.getHeader("Content-Encoding"));
            try (GZIPInputStream in = new GZIPInputStream(
                    new ByteArrayInputStream(response.getContentAsByteArray()))) {
                assertEquals(body, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            filter.destroy();
        }
    }
}
