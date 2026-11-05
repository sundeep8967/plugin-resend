package io.kestra.plugin.resend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Send a transactional email using Resend",
    description = "Dispatches a single transactional HTML or plain-text email through the Resend API."
)
@Plugin(
    examples = {
        @Example(
            title = "Send a notification email",
            full = true,
            code = """
                id: send_resend_email
                namespace: company.team

                tasks:
                  - id: send_mail
                    type: io.kestra.plugin.resend.Send
                    apiKey: "{{ secret('RESEND_API_KEY') }}"
                    from: "Acme <onboarding@resend.dev>"
                    to:
                      - "developer@example.com"
                    subject: "Workflow execution completed"
                    html: "<h1>Success</h1><p>Your scheduled data pipeline has finished running.</p>"
                """
        )
    }
)
public class Send extends AbstractResendTask implements RunnableTask<Send.Output> {
    private static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    @NotNull
    @Schema(
        title = "Sender Email Address",
        description = "Sender email, optionally with display name (e.g. `Acme <onboarding@resend.dev>`)."
    )
    @PluginProperty(group = "main")
    private Property<String> from;

    @NotNull
    @Schema(
        title = "Recipient Email Address(es)",
        description = "One or more destination email addresses."
    )
    @PluginProperty(group = "main")
    private Property<List<String>> to;

    @NotNull
    @Schema(
        title = "Email Subject",
        description = "The subject line of the email."
    )
    @PluginProperty(group = "main")
    private Property<String> subject;

    @Schema(
        title = "HTML Body",
        description = "The HTML message content."
    )
    @PluginProperty(group = "main")
    private Property<String> html;

    @Schema(
        title = "Plain Text Body",
        description = "The plain text fallback message content."
    )
    @PluginProperty(group = "main")
    private Property<String> text;

    @Schema(
        title = "CC Email Address(es)",
        description = "Carbon copy recipient email address(es)."
    )
    @PluginProperty(group = "advanced")
    private Property<List<String>> cc;

    @Schema(
        title = "BCC Email Address(es)",
        description = "Blind carbon copy recipient email address(es)."
    )
    @PluginProperty(group = "advanced")
    private Property<List<String>> bcc;

    @Schema(
        title = "Reply-To Email Address(es)",
        description = "Optional reply-to address(es)."
    )
    @PluginProperty(group = "advanced")
    private Property<List<String>> replyTo;

    @Schema(
        title = "Custom Headers",
        description = "Key-value map of custom RFC 822 email headers."
    )
    @PluginProperty(group = "advanced")
    private Property<Map<String, String>> headers;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String token = runContext.render(this.apiKey).as(String.class).orElseThrow();
        String base = runContext.render(this.baseUrl).as(String.class).orElse("https://api.resend.com");
        Duration reqTimeout = runContext.render(this.timeout).as(Duration.class).orElse(Duration.ofSeconds(15));

        String fromVal = runContext.render(this.from).as(String.class).orElseThrow();
        List<String> toVal = runContext.render(this.to).asList(String.class);
        String subjectVal = runContext.render(this.subject).as(String.class).orElseThrow();
        String htmlVal = runContext.render(this.html).as(String.class).orElse(null);
        String textVal = runContext.render(this.text).as(String.class).orElse(null);
        List<String> ccVal = runContext.render(this.cc).asList(String.class);
        List<String> bccVal = runContext.render(this.bcc).asList(String.class);
        List<String> replyToVal = runContext.render(this.replyTo).asList(String.class);
        Map<String, String> headersVal = runContext.render(this.headers).asMap(String.class, String.class);

        if (htmlVal == null && textVal == null) {
            throw new IllegalArgumentException("Either 'html' or 'text' body must be provided when sending an email with Resend.");
        }

        Payload payload = Payload.builder()
            .from(fromVal)
            .to(toVal)
            .subject(subjectVal)
            .html(htmlVal)
            .text(textVal)
            .cc(ccVal.isEmpty() ? null : ccVal)
            .bcc(bccVal.isEmpty() ? null : bccVal)
            .replyTo(replyToVal.isEmpty() ? null : replyToVal)
            .headers(headersVal.isEmpty() ? null : headersVal)
            .build();

        String jsonPayload = MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(base.replaceAll("/+$", "") + "/emails"))
            .timeout(reqTimeout)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        logger.debug("Dispatching email via Resend to {} recipients", toVal.size());

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(String.format(
                    "Resend API responded with HTTP %d: %s",
                    response.statusCode(),
                    response.body()
                ));
            }

            ResendResponse resendResponse = MAPPER.readValue(response.body(), ResendResponse.class);

            logger.info("Successfully sent email via Resend with ID '{}'", resendResponse.id());

            return Output.builder()
                .id(resendResponse.id())
                .from(fromVal)
                .to(toVal)
                .build();
        }
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Payload(
        String from,
        List<String> to,
        String subject,
        String html,
        String text,
        List<String> cc,
        List<String> bcc,
        @JsonProperty("reply_to") List<String> replyTo,
        Map<String, String> headers
    ) {}

    private record ResendResponse(String id) {}

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Email ID",
            description = "The unique identifier generated by Resend for the dispatched email."
        )
        private final String id;

        @Schema(
            title = "Sender",
            description = "The sender address used for the email."
        )
        private final String from;

        @Schema(
            title = "Recipients",
            description = "The list of recipient addresses."
        )
        private final List<String> to;
    }
}
