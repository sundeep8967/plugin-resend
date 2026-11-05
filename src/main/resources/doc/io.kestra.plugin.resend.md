# Resend

The Resend plugin allows sending transactional emails through the [Resend API](https://resend.com/docs).

Resend is an email platform designed for modern software teams, offering deliverability, clean developer APIs, and structured email delivery tracking.

## Getting Started

To use the Resend plugin, obtain an API key from your [Resend API Keys Dashboard](https://resend.com/api-keys). Store the generated key securely inside your Kestra Secrets manager under the key `RESEND_API_KEY`.

## Tasks

### `io.kestra.plugin.resend.Send`

Dispatches a single transactional email using HTML or plain text with optional CC, BCC, custom headers, and reply-to addresses.

Key features:
- Send modern HTML templates or plain text messages.
- Full support for carbon copy (`cc`), blind carbon copy (`bcc`), and custom `replyTo` lists.
- Custom headers support for tracking IDs and compliance metadata.
- Native expression rendering for dynamic recipient routing and template variables.
