# Production go-live

This is the operator runbook for the one-server Docker installation. The code side is prepared; the items marked **Operator** need accounts, credentials or decisions that cannot be made in the repository.

## What the installation does by itself

| Area | Provided |
| --- | --- |
| HTTPS | Caddy obtains and renews certificates for `DOMAIN`; HSTS, a strict Content-Security-Policy (no inline scripts, no third-party hosts) and other security headers are set in `Caddyfile`. Request bodies are capped at 220 MB. |
| Supervision | Every long-running service has `restart: unless-stopped` and a health check. Caddy starts only when the web front end and the API report healthy. Enable the Docker service at boot (`systemctl enable docker`) so the stack returns after a reboot. |
| AI model | `ollama-model` downloads `OLLAMA_MODEL` (default `qwen2.5:7b`, about 5 GB) into the `ollama_models` volume on first start and is skipped once present. The office functions start even when the model is unavailable; research then reports it as not ready. |
| OCR | German/English OCR models are built into the API image (`server/Dockerfile`). |
| Backups | The `backup` service creates an encrypted backup every day at `BACKUP_HOUR_UTC`, verifies it by decrypting and checking every file, and logs one JSON line per run (`docker compose logs backup`). The key is created once in the `backup_keys` volume and is never replaced while backups exist. No backup is deleted unless `BACKUP_KEEP_DAYS` is set; the newest is always kept. |
| Payments | Stripe checkout, customer portal and signed webhooks. A failed renewal payment (`past_due`) keeps write access while Stripe retries and shows a notice; `unpaid`/`canceled` end write access, reading and export stay available. |
| Tests | `.github/workflows/ci.yml` runs the server tests, the front-end build and tests, and validates `compose.yaml` on every pull request. |

## Go-live steps

1. **Operator – server and domain.** Linux host with Docker Engine and the Compose plugin, at least 8 vCPU / 16 GB RAM / 100 GB disk for the 7B model (measure answer time before advertising response times). Point the domain's A/AAAA record to the host and open only ports 80 and 443.
2. **Operator – configuration.** `cp .env.example .env`, then set `DOMAIN`, `PUBLIC_APP_URL=https://<domain>`, the SMTP values and, when payments start, the Stripe values (below). Keep `.env` out of Git (already ignored).
3. **Start.** `docker compose up -d --build`. Watch `docker compose ps` until all services are `healthy`; `docker compose logs -f ollama-model` shows the model download.
4. **First administrator.** `docker compose exec api python -m server.bootstrap_admin --data-dir /data --email <your-admin-email>`; read the generated password once with `docker compose exec api cat /data/admin-access.json`, sign in, enable two-factor sign-in (required for operator functions), then delete the file: `docker compose exec api rm /data/admin-access.json`.
5. **Operator – Stripe.** Create three monthly prices in EUR with *tax behaviour: inclusive* (9.90 / 29.90 / 49.90), add a webhook endpoint `https://<domain>/api/billing/webhook` for `customer.subscription.created`, `customer.subscription.updated` and `customer.subscription.deleted`, configure the customer portal (payment methods, invoices, cancellation), and put the keys and price IDs in `.env`. Test a purchase, cancellation and failed payment in **test mode** first. The server refuses checkout when a configured price does not match the advertised amount.
6. **Operator – email.** Set `SMTP_*`; send yourself a password reset to confirm delivery. Configure SPF/DKIM for the sender domain.
7. **Operator – off-site backup.** Copy `BACKUP_PATH` **and** the key (`docker run --rm -v <project>_backup_keys:/k alpine cat /k/master.key`) to a second location, kept apart from each other. Rehearse a restore as described in [backup-restore.md](backup-restore.md) before real data arrives.
8. **Operator – legal and privacy.** Before charging or onboarding consumers: imprint, privacy notice, terms, a data-processing agreement for law-firm customers, and a review by a qualified lawyer of whether the consumer research assistant falls under the Rechtsdienstleistungsgesetz. Hosting country is part of that review.
9. **Smoke test on the live domain.** Register, create a case, upload and review a PDF, ask the research assistant, save and export a draft as PDF/Word, add a deadline and import the calendar file, run `docker compose exec backup python -m server.backup_scheduler --once`.

## Updating

`git pull && docker compose up -d --build`. Database migrations are additive and run at API start. Take a manual backup first (`docker compose exec backup python -m server.backup_scheduler --once`).

## Not provided

No electronic court filing (beA/EGVP), no automatic legal approval, no external uptime alerting: connect an external monitor (any HTTPS check on `https://<domain>/api/health`) if you need notifications.
