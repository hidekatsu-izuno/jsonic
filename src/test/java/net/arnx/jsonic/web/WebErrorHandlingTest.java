package net.arnx.jsonic.web;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.arnx.jsonic.JSON;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

class WebErrorHandlingTest {
    public static class FailingStartContainer extends Container {
        @Override
        public void start(HttpServletRequest request, HttpServletResponse response) throws IOException {
            throw new IOException("startup failed");
        }
    }

    @Test
    void rpcSerializesErrorsBeforeJsonHasBeenCreated() throws Exception {
        RPCServlet servlet = new RPCServlet();
        MockServletConfig config = new MockServletConfig();
        config.addInitParameter("config", "{\"container\":\"" + FailingStartContainer.class.getName() + "\"}");
        servlet.init(config);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/rpc");
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            servlet.doPost(request, response);
            Map<?, ?> body = JSON.decode(response.getContentAsString(), Map.class);
            assertEquals("2.0", body.get("jsonrpc"));
            assertNull(body.get("id"));
            Map<?, ?> error = (Map<?, ?>)body.get("error");
            assertEquals("-32600", error.get("code").toString());
            assertEquals("Invalid Request.", error.get("message"));
            assertThrows(UnsupportedOperationException.class, ExternalContext::getRequest);
        } finally {
            servlet.destroy();
        }
    }

    @Test
    void unmatchedGatewayPathDoesNotContinueTheChain() throws Exception {
        GatewayFilter filter = new GatewayFilter();
        MockFilterConfig config = new MockFilterConfig();
        config.addInitParameter("config", "{}");
        filter.init(config);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/line\nbreak");
        request.setServletPath("/line\nbreak");
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            assertThrows(ServletException.class,
                    () -> filter.doFilter(request, response, (req, res) -> fail("Unexpected filter chain invocation")));
        } finally {
            filter.destroy();
        }
    }
}
