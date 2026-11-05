<p align="center">
  <a href="https://www.kestra.io">
    <img src="https://kestra.io/banner.png" alt="Kestra workflow orchestrator" />
  </a>
</p>

# Kestra Plugin Resend

This plugin enables sending transactional emails through the [Resend API](https://resend.com) directly from Kestra workflows.

## Features

- **Transactional Emails**: Send formatted HTML and plain text messages via `io.kestra.plugin.resend.Send`.
- **Recipient Routing**: Support for primary recipients (`to`), carbon copy (`cc`), blind carbon copy (`bcc`), and `replyTo`.
- **Custom RFC 822 Headers**: Attach custom tracking, compliance, and routing headers.
- **Dynamic Expression Support**: Full Pebble templating across recipient lists, subjects, and email bodies.

## Example Flow

```yaml
id: send_resend_notification
namespace: company.team

tasks:
  - id: send_email
    type: io.kestra.plugin.resend.Send
    apiKey: "{{ secret('RESEND_API_KEY') }}"
    from: "Acme Notifications <onboarding@resend.dev>"
    to:
      - "dev@example.com"
    subject: "Pipeline execution completed"
    html: "<h1>Success</h1><p>The daily ETL job has executed successfully.</p>"
```

## Building and Testing

```bash
# Compile and run unit tests
./gradlew test

# Full check including plugin documentation linting
./gradlew check
```
