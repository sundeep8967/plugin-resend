# Kestra Resend Plugin

## What

- Provides email dispatch components under `io.kestra.plugin.resend`.
- Includes classes such as `AbstractResendTask` and `Send`.

## Why

- What user problem does this solve? Teams need a modern, developer-friendly transactional email delivery option alongside SendGrid and Mailgun.
- Why would a team adopt this plugin in a workflow? It allows direct delivery of pipeline reports, alerts, and customer notifications via Resend without writing custom scripts.
- What operational/business outcome does it enable? High deliverability transactional emails with native Kestra secret handling and zero external dependencies.

## Key Plugin Classes

- `io.kestra.plugin.resend.AbstractResendTask`
- `io.kestra.plugin.resend.Send`

## Development

```bash
./gradlew test
./gradlew check
```
