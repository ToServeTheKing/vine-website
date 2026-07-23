# The Vine Coffeehouse + Bakery — itsthevine.com

Site for The Vine, 215 E Main Street, Princeville, Illinois. Spring Boot serving a Vite/React SPA,
on [the Bennett platform](https://git.thebennett.net/austin/platform).

Previously a Next.js app on Cloudflare, then self-hosted; the look is unchanged.

## Shape

| | |
|---|---|
| Backend | Spring Boot 4 / Java 25, `com.itsthevine.web` |
| Frontend | Vite + React 19 + TypeScript + Tailwind v4, served from the jar |
| Database | Postgres (`itsthevine` on the shared `app-db` cluster), Flyway |
| Photos | public MinIO bucket `itsthevine` — **not** in the repo or the image |
| Deploy | Gitea CI → image → Watchtower → Caddy |

## What the server owns

The SPA renders; it doesn't decide anything.

- **`/api/products`**, **`/api/categories`** — the catalogue, its curated order, the category filter
  and the absolute image URLs. This was a TypeScript array shipped to every visitor; it's now a table
  (`V2__products.sql`) read through `ProductCatalog`.
- **`/api/contact`** — validates, **records the enquiry**, emails it, then fans out to the n8n hub.
  Recorded before sending on purpose: a relay outage costs a notification, not the enquiry. Undelivered
  ones are `enquiry.delivered = false`. Validation and delivery come from `platform-starter-contact`,
  shared with the other sites.
- **Per-page metadata** — `PageMetaController` rewrites `<title>`/`<meta>`/OG tags per route. Next used
  to server-render these; a plain SPA would hand crawlers and link-preview scrapers one generic shell.

## /admin

The catalogue is editable from the site: add an item with a photo and a name, reorder it, rename or
reorder the category filters. Nothing there needs a deploy or a migration — which is the point, since
the person adding a cake is the person who baked it.

Photos are resized, stripped of EXIF, converted to webp and put in the bucket on upload
(`ProductPhotoService`, using `cwebp` from `libwebp-tools` — the pure-Java encoders either can't write
webp or ship glibc natives that don't run on Alpine).

**The admin only exists when `SECURITY_MODE=OIDC`.** `AdminProductController` and
`AdminCategoryController` are `@ConditionalOnProperty` on it, so a deployment that forgets to configure
Authentik gets 404s rather than catalogue writes open to the internet. `/admin` and `/api/admin/**` are
both authenticated paths: a browser opening the page is sent to Authentik first, while `fetch` calls get
a bare 401 to handle.

Known gap: `StorageService` has no delete, so removing a product or a photo leaves the object in the
bucket. Harmless — nothing links to it — but it accumulates.

## Photos

Re-encoded to webp and uploaded to the bucket once (50 MB of originals → 14 MB), served with a
year-long cache. `site.assets.base-url` says where they live. The originals remain in this repo's
history. EXIF (including GPS from phone photos) is stripped by the re-encode.

## Local development

```bash
# backend (needs Postgres on :5432 with an itsthevine database)
mvn spring-boot:run

# frontend, proxies /api to :8080
cd frontend && npm install && npm run dev   # http://localhost:2024
```

Build without the SPA for quick backend loops: `mvn -DskipFrontend=true package`.

Tests need Docker (Testcontainers):

```bash
mvn verify
```

## Configuration

| Variable | Purpose |
|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Postgres |
| `SMTP_SERVER` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_TOKEN` | relay for the contact form |
| `CONTACT_TO` / `CONTACT_FROM` | enquiry recipient and envelope sender |
| `CONTACT_HUB_URL` | optional n8n webhook; best-effort, never blocks a submission |
| `SITE_BASE_URL` | absolute base for `og:url` |
| `VITE_ASSET_BASE` / `site.assets.base-url` | photo bucket |
| `SECURITY_MODE` | `OIDC` turns on Authentik login **and brings `/admin` into existence**. Unset = brochure site, no admin |
| `STORAGE_ENDPOINT` / `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` / `STORAGE_BUCKET` | MinIO, for admin photo uploads. Blank endpoint leaves storage switched off |

With `SECURITY_MODE=OIDC` the app also needs the standard Spring OAuth2 client properties for the
Authentik application — `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_*` and
`..._PROVIDER_*_ISSUER_URI`. The starter configures the filter chain, not the identity provider.

A missing `CONTACT_TO` **stops the app from starting**. That is deliberate: `application.yaml` maps it
to `platform.contact.to`, and an unset variable leaves the property present-but-empty, which is enough
to activate the contact starter. Without the `@NotBlank` check in `platform-starter-contact` the site
would come up, show a working contact form, and mail every enquiry to nobody. Better to fail on deploy
than to lose a week of orders.
