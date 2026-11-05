package io.kestra.plugin.resend;

import com.sun.net.httpserver.HttpServer;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@KestraTest
class SendTest {
    @Inject
    private RunContextFactory runContextFactory;

    private HttpServer server;
    private String serverUrl;
    private final AtomicReference<String> lastAuthHeader = new AtomicReference<>();
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private int responseStatus = 200;
    private String responseBody = "{\"id\": \"resend-test-msg-123\"}";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        serverUrl = "http://localhost:" + server.getAddress().getPort();

        server.createContext("/emails", exchange -> {
            lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSendEmailSuccessfullyWhenParametersValid() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", "re_secret_123",
            "recipient", "dev@example.com"
        ));

        Send send = Send.builder()
            .apiKey(Property.ofExpression("{{ apiKey }}"))
            .baseUrl(Property.ofValue(serverUrl))
            .from(Property.ofValue("Acme <onboarding@resend.dev>"))
            .to(Property.ofExpression("[\"{{ recipient }}\"]"))
            .subject(Property.ofValue("Daily Report"))
            .html(Property.ofValue("<h1>Report</h1><p>All systems operational.</p>"))
            .build();

        Send.Output output = send.run(runContext);

        assertThat(output).isNotNull();
        assertThat(output.getId()).isEqualTo("resend-test-msg-123");
        assertThat(output.getFrom()).isEqualTo("Acme <onboarding@resend.dev>");
        assertThat(output.getTo()).containsExactly("dev@example.com");

        assertThat(lastAuthHeader.get()).isEqualTo("Bearer re_secret_123");
        assertThat(lastRequestBody.get()).contains("\"from\":\"Acme <onboarding@resend.dev>\"");
        assertThat(lastRequestBody.get()).contains("\"to\":[\"dev@example.com\"]");
        assertThat(lastRequestBody.get()).contains("\"subject\":\"Daily Report\"");
        assertThat(lastRequestBody.get()).contains("\"html\":\"<h1>Report</h1><p>All systems operational.</p>\"");
    }

    @Test
    void shouldThrowExceptionWhenNeitherHtmlNorTextProvided() {
        RunContext runContext = runContextFactory.of();

        Send send = Send.builder()
            .apiKey(Property.ofValue("re_secret_123"))
            .baseUrl(Property.ofValue(serverUrl))
            .from(Property.ofValue("onboarding@resend.dev"))
            .to(Property.ofValue(List.of("dev@example.com")))
            .subject(Property.ofValue("No Content"))
            .build();

        assertThatThrownBy(() -> send.run(runContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Either 'html' or 'text' body must be provided");
    }

    @Test
    void shouldThrowExceptionWhenResendApiReturnsErrorStatus() {
        responseStatus = 403;
        responseBody = "{\"name\": \"forbidden\", \"message\": \"Domain not verified\"}";

        RunContext runContext = runContextFactory.of();

        Send send = Send.builder()
            .apiKey(Property.ofValue("re_secret_123"))
            .baseUrl(Property.ofValue(serverUrl))
            .from(Property.ofValue("onboarding@unverified.com"))
            .to(Property.ofValue(List.of("dev@example.com")))
            .subject(Property.ofValue("Test"))
            .text(Property.ofValue("Hello"))
            .build();

        assertThatThrownBy(() -> send.run(runContext))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Resend API responded with HTTP 403");
    }
}
