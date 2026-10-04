package net.arnx.jsonic.web;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.Charset;
import java.util.*;
import net.arnx.jsonic.JSON;
import org.junit.jupiter.api.Test;

class SampleSecurityTest {
    @Test
    void blocksInfrastructureRpcAndQuotesCsvValues() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        try (WebTestServer server = new WebTestServer()) {
            for (String app : List.of("basic", "spring")) {
                String base = server.url() + "/" + app;
                var attack = client.send(HttpRequest.newBuilder(URI.create(base + "/rpc/rest/memo.json"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"method\":\"setServletContext\",\"params\":[null],\"id\":1}"))
                        .build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(404, attack.statusCode());
                for (String title : List.of("=1+1", " \t@SUM(1)", "normal,\"quoted\"")) {
                    String text = "safe\r\n999,=2+2,injected";
                    var created = client.send(HttpRequest.newBuilder(URI.create(base + "/rest/memo.json"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(JSON.encode(Map.of("title", title, "text", text))))
                            .build(), HttpResponse.BodyHandlers.ofString());
                    assertEquals(201, created.statusCode());
                }
                var csv = client.send(HttpRequest.newBuilder(URI.create(base + "/rest/memo.print.json")).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, csv.statusCode());
                String text = new String(csv.body(), Charset.forName("MS932"));
                assertEquals("0,\"'=1+1\",\"safe\r\n999,=2+2,injected\"\r\n"
                        + "1,\"' \t@SUM(1)\",\"safe\r\n999,=2+2,injected\"\r\n"
                        + "2,\"normal,\"\"quoted\"\"\",\"safe\r\n999,=2+2,injected\"\r\n", text);
                var read = client.send(HttpRequest.newBuilder(URI.create(base + "/rest/memo.json")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, read.statusCode());
            }
        }
    }
}
