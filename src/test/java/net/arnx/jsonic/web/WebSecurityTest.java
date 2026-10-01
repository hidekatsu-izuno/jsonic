package net.arnx.jsonic.web;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.arnx.jsonic.JSON;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WebSecurityTest {
    private static Server server;
    private static String base;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static class Service {
        static final AtomicInteger deletes = new AtomicInteger();
        static final AtomicInteger updates = new AtomicInteger();
        public List<String> find() { return List.of("data"); }
        public void delete() { deletes.incrementAndGet(); }
        public void update() { updates.incrementAndGet(); }
    }

    @BeforeAll
    static void start() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        for (String path : List.of("/", "/app")) {
            ServletContextHandler context = new ServletContextHandler();
            context.setContextPath(path);
            ServletHolder rest = new ServletHolder(new RESTServlet());
            rest.setInitParameter("config", "{\"mappings\":{\"/memo.json\":\""
                    + Service.class.getName() + "\"}}");
            context.addServlet(rest, "*.json");
            context.addServlet(new ServletHolder(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws IOException {
                    response.getWriter().write("protected");
                }
            }), "/admin/*");
            context.addServlet(new ServletHolder(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws IOException, ServletException {
                    request.getRequestDispatcher("/admin/secret").forward(request, response);
                }
            }), "/jump");
            context.addServlet(new ServletHolder(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws IOException {
                    response.getWriter().write("public");
                }
            }), "/open");
            FilterHolder filter = new FilterHolder(new GatewayFilter());
            filter.setInitParameter("config", "{\"/admin/.*\":{\"access\":[\"admin\"]},"
                    + "\"/rewrite\":{\"forward\":\"/admin/secret\"},"
                    + "\"/rewrite-open\":{\"forward\":\"/open\",\"compression\":true},"
                    + "\"/open\":{\"forward\":\"/open\"}}");
            context.addFilter(filter, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
            contexts.addHandler(context);
        }
        server.setHandler(contexts);
        server.start();
        base = "http://127.0.0.1:" + connector.getLocalPort();
    }

    @AfterAll
    static void stop() throws Exception {
        if (server != null) server.stop();
    }

    private static HttpResponse<String> request(String method, String path) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void protectsEverySpellingOfMappedPath() throws Exception {
        for (String context : List.of("", "/app")) {
            for (String path : List.of("/admin/secret", "/%61dmin/secret", "/admin;anything/secret")) {
                assertEquals(403, request("GET", context + path).statusCode(), context + path);
            }
            assertEquals(200, request("GET", context + "/memo.json").statusCode());
        }
    }

    @Test
    void authorizesServletAndFilterForwards() throws Exception {
        for (String context : List.of("", "/app")) {
            assertEquals(403, request("GET", context + "/jump").statusCode());
            assertEquals(403, request("GET", context + "/rewrite").statusCode());
            var response = CLIENT.send(HttpRequest.newBuilder(URI.create(base + context + "/rewrite-open"))
                    .header("Accept-Encoding", "gzip").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode());
            assertEquals("gzip", response.headers().firstValue("Content-Encoding").orElseThrow());
            try (var gzip = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(response.body()))) {
                assertEquals("public", new String(gzip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void rejectsUntrustedPostsBeforeServiceInvocation() throws Exception {
        Service.deletes.set(0);
        for (String origin : List.of("https://untrusted.example", "null", "invalid", base + "/path")) {
            for (String contentType : List.of("application/x-www-form-urlencoded", "application/json")) {
                var response = CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/memo.json?_method=DELETE"))
                        .header("Origin", origin).header("Content-Type", contentType)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(403, response.statusCode(), origin + " " + contentType);
            }
        }
        for (String contentType : List.of("application/x-www-form-urlencoded", "text/plain", "multipart/form-data")) {
            var response = CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/memo.json?_method=DELETE"))
                    .header("Content-Type", contentType).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(403, response.statusCode(), contentType);
        }
        assertEquals(0, Service.deletes.get());
    }

    @Test
    void acceptsSameOriginFormsAndOriginlessApiClients() throws Exception {
        Service.deletes.set(0);
        for (String[] headers : List.of(new String[]{"Origin", base},
                new String[]{"X-Requested-With", "XMLHttpRequest"})) {
            var response = CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/memo.json"))
                    .header(headers[0], headers[1]).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("_method=DELETE")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(204, response.statusCode());
        }
        assertEquals(204, request("POST", "/memo.json?_method=DELETE").statusCode());
        assertEquals(3, Service.deletes.get());
    }

    @Test
    void onlyPostCanOverrideMethod() throws Exception {
        Service.deletes.set(0);
        Service.updates.set(0);
        assertEquals(200, request("GET", "/memo.json?_method=DELETE").statusCode());
        assertEquals(405, request("HEAD", "/memo.json?_method=DELETE").statusCode());
        assertEquals(405, request("OPTIONS", "/memo.json?_method=DELETE").statusCode());
        assertEquals(204, request("PUT", "/memo.json?_method=DELETE").statusCode());
        assertEquals(0, Service.deletes.get());
        assertEquals(1, Service.updates.get());
        assertEquals(204, request("POST", "/memo.json?_method=delete").statusCode());
        assertEquals(1, Service.deletes.get());
        assertEquals(204, request("DELETE", "/memo.json?_method=PUT").statusCode());
        assertEquals(2, Service.deletes.get());
        assertEquals(1, Service.updates.get());
    }

    @Test
    void callbackNeverChangesJsonResponse() throws Exception {
        for (String callback : List.of("call", "alert%281%29%3B%2F%2F")) {
            var response = request("GET", "/memo.json?callback=" + callback);
            assertEquals(200, response.statusCode());
            assertTrue(response.headers().firstValue("content-type").orElseThrow()
                    .startsWith("application/json"));
            assertEquals(List.of("data"), JSON.decode(response.body()));
        }
    }
}
