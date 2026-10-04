package net.arnx.jsonic.web;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

class ExternalContextTest {
    private static void assertUnavailable() {
        assertThrows(UnsupportedOperationException.class, ExternalContext::getConfig);
        assertThrows(UnsupportedOperationException.class, ExternalContext::getApplication);
        assertThrows(UnsupportedOperationException.class, ExternalContext::getRequest);
        assertThrows(UnsupportedOperationException.class, ExternalContext::getResponse);
        assertThrows(UnsupportedOperationException.class, ExternalContext::getSession);
    }

    @Test
    void providesContextOnTheRequestThreadAndClearsItAtEnd() {
        MockServletContext application = new MockServletContext();
        MockServletConfig config = new MockServletConfig(application);
        MockHttpServletRequest request = new MockHttpServletRequest(application);
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            ExternalContext.start(config, application, request, response);
            assertSame(config, ExternalContext.getConfig());
            assertSame(application, ExternalContext.getApplication());
            assertSame(request, ExternalContext.getRequest());
            assertSame(response, ExternalContext.getResponse());
            assertSame(request.getSession(), ExternalContext.getSession());
        } finally {
            ExternalContext.end();
        }
        assertUnavailable();
    }

    @Test
    void doesNotInheritContextOnChildWorkersOrExposeItWhenTheyAreReused() throws Exception {
        for (ThreadFactory factory : List.of(Thread.ofPlatform().factory(), Thread.ofVirtual().factory())) {
            MockHttpServletRequest first = new MockHttpServletRequest();
            MockHttpServletRequest second = new MockHttpServletRequest();
            // The worker is created by the first submission while a request is bound.
            try (ExecutorService worker = Executors.newSingleThreadExecutor(factory)) {
                try {
                    ExternalContext.start(null, null, first, new MockHttpServletResponse());
                    worker.submit(ExternalContextTest::assertUnavailable).get(5, TimeUnit.SECONDS);
                    assertSame(first, ExternalContext.getRequest());
                    ExternalContext.end();
                    worker.submit(ExternalContextTest::assertUnavailable).get(5, TimeUnit.SECONDS);

                    ExternalContext.start(null, null, second, new MockHttpServletResponse());
                    worker.submit(() -> {
                        assertUnavailable();
                        MockHttpServletRequest own = new MockHttpServletRequest();
                        try {
                            ExternalContext.start(null, null, own, new MockHttpServletResponse());
                            assertSame(own, ExternalContext.getRequest());
                        } finally {
                            ExternalContext.end();
                        }
                        assertUnavailable();
                    }).get(5, TimeUnit.SECONDS);
                    assertSame(second, ExternalContext.getRequest());
                    ExternalContext.end();
                    worker.submit(ExternalContextTest::assertUnavailable).get(5, TimeUnit.SECONDS);
                } finally {
                    ExternalContext.end();
                }
            }
        }
    }
}
