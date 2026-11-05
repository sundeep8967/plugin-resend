package io.kestra.plugin.resend;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractResendTask extends Task {
    @NotNull
    @Schema(
        title = "Resend API Key",
        description = "The API key starting with `re_` used to authenticate against the Resend API."
    )
    @PluginProperty(group = "connection", secret = true)
    @ToString.Exclude
    protected Property<String> apiKey;

    @Schema(
        title = "Base URL",
        description = "The Resend API base endpoint. Defaults to `https://api.resend.com`."
    )
    @PluginProperty(group = "connection")
    @Builder.Default
    protected Property<String> baseUrl = Property.ofValue("https://api.resend.com");

    @Schema(
        title = "HTTP Request Timeout",
        description = "Maximum duration to wait for the HTTP request to finish."
    )
    @PluginProperty(group = "connection")
    @Builder.Default
    protected Property<Duration> timeout = Property.ofValue(Duration.ofSeconds(15));
}
