# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| main    | :white_check_mark: (best-effort, no releases yet) |

This project has not published a tagged release yet. Security fixes land on
`main` and are backported to the `release/v1` branch on request.

## Reporting a Vulnerability

Please report security vulnerabilities privately. Do **not** open a public
issue.

- Open a GitHub Security Advisory on this repository, or
- Email the maintainers directly (see the latest commit authors).

You can expect an acknowledgement within 48 hours and a follow-up within 5
business days. If the issue is confirmed, we will coordinate a fix and
disclosure timeline with you.

## Security Notes for This Tool

- **Credentials**: AEM credentials are encrypted with AES-256-GCM before being
  stored in `~/.aem-api/config.yaml`. The master key lives in
  `~/.aem-api/.key` (or is supplied via the `AEM_API_MASTER_KEY` environment
  variable). This protects credentials at rest on a single machine; it is not a
  substitute for a proper secret manager in shared or CI environments.
- **Secrets in scripts**: Prefer `aem-api connect --save` over passing
  `--password` on the command line, where the value can be observed in process
  listings and shell history.
- **HTTPS**: Use `--https-only` to refuse plain-HTTP AEM connections.
- **Dependency hygiene**: Keep dependencies current; see `pom.xml`. Known CVEs
  are tracked via the GitHub Dependabot alerts on this repository.
