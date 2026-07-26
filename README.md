# The Vine Coffeehouse + Bakery — itsthevine.com

Site for The Vine, 215 E Main Street, Princeville, Illinois. Spring Boot rendering its own pages with
Thymeleaf, on [the Bennett platform](https://git.thebennett.net/austin/platform).

Previously a Next.js app on Cloudflare, then a React SPA on Spring, now server-rendered. The look has
not changed through any of it.

## Shape

| | |
|---|---|
| Backend | Spring Boot 4 / Java 25, `com.itsthevine.web` |
| Pages | Thymeleaf, `src/main/resources/templates` — **no JavaScript** except one 100-line file for the product-card arrows |
| Styling | Tailwind v4, compiled from the templates by the Tailwind CLI into `static/css/site.css` |
| Admin | the one React screen that is left, served at `/admin` only |
| Database | Postgres (`itsthevine` on the shared `app-db` cluster), Flyway |
| Photos | public MinIO bucket `itsthevine` — **not** in the repo or the image |
| Deploy | Gitea CI → image → Watchtower → Caddy |

### Why server-rendered

The pages are content: a menu, a story, opening hours, a price list. Rendering them in the browser meant
shipping a router and a component tree to show them, and it meant `PageMetaController` — a class whose
only job was to splice per-page `<title>` and OG tags into one shell with regular expressions, because a
crawler or a link-preview scraper got nothing useful otherwise. A page that is rendered on the server
writes its own head, so that whole mechanism is deleted rather than ported. The category filter is a
`?category=` link instead of a click handler, which also makes every filtered view a URL you can send
someone, and the contact form is a form post.

`platform.web.spa.enabled=false` follows from that: the platform's fallback forwards extension-less paths
to `/index.html`, which now holds nothing but the admin. `SiteController` maps `/admin` to it explicitly.

## What the server owns

Everything. The pages arrive complete.

- **`/api/products`**, **`/api/categories`** — the catalogue, its curated order, the category filter
  and the absolute image URLs. This was a TypeScript array shipped to every visitor; it's now a table
  (`V2__products.sql`) read through `ProductCatalog`.
- **`/catering`** — the goodie box and catering page. Each table is rendered twice from the same model
  and CSS shows one: a real `<table>` on a wide screen, because that is what a price list is and a screen
  reader then announces the size and the item together; stacked cards on a phone, because a four-column
  price table there is either illegible or a sideways scroll, and this page is mostly read on phones.
- **`/api/catering`** — the same tables as JSON: the
  columns, the prices already written the way they should be read, the entries under each column, and
  the small print. These came from the bakery as a spreadsheet and are stored as one (`V4__catering.sql`,
  read through `CateringMenu`) rather than as markup, because the prices move and the last line of that
  spreadsheet says the tables are "mostly just an idea for people". `Money` is the only thing that
  decides what a typed price means or how it prints. A table with no columns or no lines is left off the
  public response — adding a table and filling it in are two separate acts in the admin, and the gap
  between them shouldn't put a bare heading on the live page.
- **`/contact`** — the form posts here and gets a page back. It renders rather than redirects on failure,
  so a refused relay comes back with what the visitor typed still in the boxes: they wrote it once, and
  the failure is ours. `/api/contact` still exists and answers JSON; both go through `Enquiries`, so
  there is one order of operations for taking an enquiry.
- **`/api/contact`** — validates, **records the enquiry**, emails it, then fans out to the n8n hub.
  Recorded before sending on purpose: a relay outage costs a notification, not the enquiry. Undelivered
  ones are `enquiry.delivered = false`. Validation and delivery come from `platform-starter-contact`,
  shared with the other sites.
- **Per-page metadata** — each route states its own title and description in `SiteController`, next to
  the handler that serves it, and `fragments/head.html` lays them out. `SiteControllerTest` asserts the
  real `<title>` of every page.

## /admin

**The last React in the repo.** The public pages are server-rendered; this screen is a Vite/React app
because it is not content — it is an editor, and the instant-feedback editing (reorder that applies
before the network answers, a whole price table arranged on screen and saved in one go) is the point of
it. Everything under `frontend/` builds only this, plus the site's stylesheet.

The catalogue is editable from the site: add an item with a photo and a name, reorder it, rename or
reorder the category filters. Nothing there needs a deploy or a migration — which is the point, since
the person adding a cake is the person who baked it.

Photos are resized, stripped of EXIF, converted to webp and put in the bucket on upload
(`ProductPhotoService`, using `cwebp` from `libwebp-tools` — the pure-Java encoders either can't write
webp or ship glibc natives that don't run on Alpine).

The catering tables are editable there too, but a table at a time rather than a field at a time. That
isn't a different taste in interfaces: a column heading, its price and the entries beneath it only mean
anything together, so `CateringPackage#arrange` takes the whole table and refuses one whose lines and
columns disagree. Drop the middle column on its own and every remaining entry shifts one place left —
the Large box then advertises the Medium box's contents at the Large price, and nothing about the page
looks broken.

**The admin only exists when `SECURITY_MODE=OIDC`.** `AdminProductController`,
`AdminCategoryController` and `AdminCateringController` are `@ConditionalOnProperty` on it, so a deployment that forgets to configure
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
# the whole site (needs Postgres on :5432 with an itsthevine database)
mvn spring-boot:run     # http://localhost:8080

# just the stylesheet, while editing templates — watches and recompiles
cd frontend && npm install && npx tailwindcss -i site.css -o ../target/classes/static/css/site.css --watch

# the admin screen, proxying /api to :8080
cd frontend && npm run dev   # http://localhost:2024/admin
```

`mvn spring-boot:run` compiles the stylesheet on the way (the Tailwind step is bound to
`process-classes` for exactly that reason). `-DskipFrontend=true` skips both frontend steps for a fast
backend loop — the pages then render **unstyled** until you build the CSS once.

Templates are cached by default, so a template edit needs a restart; add
`spring.thymeleaf.cache=false` to a local run if you are editing markup.

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
| `site.assets.base-url` | photo bucket. Server-side only now — the browser is handed finished URLs |
| `GIT_SHA` | passed by the image build; becomes `?v=` on the stylesheet so a deploy invalidates the cached CSS |
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
