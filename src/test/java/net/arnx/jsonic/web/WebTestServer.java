package net.arnx.jsonic.web;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import javax.tools.ToolProvider;

import net.arnx.jsonic.JSON;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

/** Prepares isolated sample applications only when their HTTP tests run. */
final class WebTestServer implements AutoCloseable {
    private final Server server = new Server();
    private final ServerConnector connector = new ServerConnector(server);

    WebTestServer() throws Exception {
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        for (String name : List.of("basic", "spring")) {
            Path source = Path.of("sample", name);
            Path target = Path.of("target", "web-test", name);
            Path classes = target.resolve("WEB-INF/classes");
            Files.createDirectories(classes);
            Files.deleteIfExists(target.resolve("WEB-INF/database.dat"));
            List<String> args = new ArrayList<>(List.of("--release", "21", "-encoding", "UTF-8",
                    "-classpath", System.getProperty("java.class.path"), "-d", classes.toString()));
            try (var files = Files.walk(source)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    Path relative = source.relativize(file);
                    if (relative.startsWith("WEB-INF/classes") || file.toString().endsWith(".dat")) continue;
                    if (file.toString().endsWith(".java")) {
                        args.add(file.toString());
                    } else {
                        Path destination = relative.startsWith("WEB-INF/src")
                                ? classes.resolve(Path.of("WEB-INF/src").relativize(relative))
                                : target.resolve(relative);
                        Files.createDirectories(destination.getParent());
                        Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new)),
                    "Compile " + name);
            WebAppContext context = new WebAppContext(target.toString(), "/" + name);
            // Load JSONIC in the webapp alongside the sample's bundled Spring jars.
            context.setExtraClasspath(JSON.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm());
            contexts.addHandler(context);
        }
        server.setHandler(contexts);
        try {
            server.start();
            for (var handler : contexts.getHandlers()) {
                WebAppContext context = (WebAppContext) handler;
                assertTrue(context.isAvailable());
                if ("/spring".equals(context.getContextPath())) {
                    Class<?> version = context.getClassLoader().loadClass("org.springframework.core.SpringVersion");
                    assertEquals("7.0.9", version.getMethod("getVersion").invoke(null));
                    assertTrue(version.getProtectionDomain().getCodeSource().getLocation().toString()
                            .contains("/WEB-INF/lib/spring-core-7.0.9.jar"));
                }
            }
        } catch (Exception | AssertionError e) {
            server.stop();
            throw e;
        }
    }

    String url() {
        return "http://127.0.0.1:" + connector.getLocalPort();
    }

    @Override
    public void close() throws Exception {
        server.stop();
    }
}
