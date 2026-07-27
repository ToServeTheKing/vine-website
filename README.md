# The Vine Coffeehouse + Bakery — itsthevine.com

Site for The Vine, 215 E Main Street, Princeville, Illinois. Spring Boot rendering its own pages with
Thymeleaf, on [the Bennett platform](https://git.thebennett.net/austin/platform).

Previously a Next.js app on Cloudflare, then a React SPA on Spring, now server-rendered end to end —
there is no JavaScript framework in this repo. The look has not changed through any of it.

## Shape

| | |
|---|---|
| Backend | Spring Boot 4 / Java 25, `com.itsthevine.web` |
| Pages | Thymeleaf, `src/main/resources/templates` — **no JavaScript** except one 100-line file for the product-card arrows |
| Styling | Tailwind v4, compiled from the templates by the Tailwind CLI into `static/css/site.css`. `src/main/styles` is the whole asset pipeline |
| Admin | Thymeleaf forms at `/admin`, behind Authentik |
| Database | Postgres (`itsthevine` on the shared `app-db` cluster), Flyway |
| Photos | public MinIO bucket `itsthevine` — **not** in the repo or the image |
| Deploy | Gitea CI → image → Watchtower → Caddy |

### The look, in one place

`src/main/styles` is the whole of it — no bundler, no framework, one stylesheet:

| file | what it holds |
|---|---|
| `theme.css` | the three typefaces and the sage-and-cream palette. Names, nothing drawn |
| `base.css` | bare elements: page background, body type, the fade-in, the focus ring, the z-index scale |
| `type.css` | the typographic ladder (below) |
| `components.css` | `panel`, `pill-*`, `chip`, `section`/`band` — the site's own classes |
| `admin.css` | the admin's controls, as `@utility` so `file:` variants work on the photo pickers |

**The type ladder comes out of the logo.** The lockup is "The Vine" in LeJour Script over COFFEEHOUSE +
BAKERY in AdBhashitha, letterspaced — and everything below is that idea made progressively more readable:
the script stays in the wordmark and nowhere else; AdBhashitha carries `h1`–`h4` and `price`, keeping the
wordmark's 0.01em letter-spacing so a heading sits on the same rhythm as the logo above it; `eyebrow` and
`label` are the hinge, sans but letterspaced like the tagline; then plain Raleway for anything you have to
read a paragraph of. None of the type classes set a colour — the same heading appears in sage on cream and
cream on sage.

Write those class names in templates rather than the utilities behind them. The full vocabulary:

| | |
|---|---|
| surfaces | `panel`, `panel-lift`, `panel-head`, `panel-mark`, `photo` |
| buttons | `pill-sage`, `pill-cream`, `pill-outline`, `pill-ghost`, `chip`/`chip-on` |
| links | `link`, `link-plain`, `link-on-dark` |
| type | `h1`–`h4`, `price`, `eyebrow`, `label`, `lede`, `wordmark` |
| layout | `container`, `measure`, `measure-wide`, `section`, `band`, `page-head` |
| forms | `field` — one input for the public form and the admin |

Four colourways of one button exist because the site puts buttons on cream *and* on sage. Corners come
from `--radius-panel` (12px) and `--radius-field` (8px), so "how round is a card" is one decision.

If you find yourself writing `bg-white rounded-… shadow-…` or `underline underline-offset-4` in a
template, there is already a name for it.

### Why server-rendered

The pages are content: a menu, a story, opening hours, a price list. Rendering them in the browser meant
shipping a router and a component tree to show them, and it meant `PageMetaController` — a class whose
only job was to splice per-page `<title>` and OG tags into one shell with regular expressions, because a
crawler or a link-preview scraper got nothing useful otherwise. A page that is rendered on the server
writes its own head, so that whole mechanism is deleted rather than ported. The category filter is a
`?category=` link instead of a click handler, which also makes every filtered view a URL you can send
someone, and the contact form is a form post.

`platform.web.spa.enabled=false` follows from that: the platform's fallback forwards extension-less paths
to `/index.html` so a React SPA can own routing, and there is no SPA here — leaving it on would answer a
mistyped URL with a blank page and a 200 instead of the site's own 404.

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

Forms and redirects. Every write is a POST followed by a redirect back to the page, so the back button
and reload do what they look like they do, a double-tap can't repeat an upload, and there is no
client-side state to lose — a reload is always the truth. Two screens: `/admin` is the catalogue,
`/admin/catering` lists the price tables and `/admin/catering/tables/{id}` edits one.

The catalogue is editable from the site: add an item with a photo and a name, reorder it, rename or
reorder the category filters. Nothing there needs a deploy or a migration — which is the point, since
the person adding a cake is the person who baked it.

Photos are resized, stripped of EXIF, converted to webp and put in the bucket on upload
(`ProductPhotoService`, using `cwebp` from `libwebp-tools` — the pure-Java encoders either can't write
webp or ship glibc natives that don't run on Alpine).

The catering tables are edited a table at a time rather than a field at a time. That isn't a taste in
interfaces: a column heading, its price and the entries beneath it only mean anything together, so
`CateringPackage#arrange` takes the whole table and refuses one whose lines and columns disagree. Drop
the middle column on its own and every remaining entry shifts one place left — the Large box then
advertises the Medium box's contents at the Large price, and nothing about the page looks broken.

**How that works without JavaScript.** One form holds the whole table and every button in it submits
that form; `name="do"` says which was pressed and its value carries the position it applies to
(`remove-column:2`). So "add a column" arrives with every cell the editor has typed, adds the column to
what arrived — plus an empty entry on every line — and re-renders. Nothing typed is lost, and **only
Save writes**: a half-built table with a blank column heading never reaches the live page, and the
aggregate would refuse it anyway. A failed save comes back the same way, with the work still in the
form and the reason above it, because a redirect would throw the work away and leave the editor guessing
which cell the message was about.

**The admin only exists when `SECURITY_MODE=OIDC`.** `AdminController` and `AdminCateringController` are
`@ConditionalOnProperty` on it, so a deployment that forgets to configure Authentik gets 404s rather than
catalogue writes open to the internet. `/admin/**` is an authenticated path, so a browser opening it is
sent to Authentik and comes back signed in. There is no JSON admin any more: `AdminProductController`,
`AdminCategoryController` and the old `/api/admin/**` endpoints existed for the React screen and went
with it. Their logic lives in `Catalogue` and `CateringMenu`, which the pages call.

**Testing a protected page needs `Accept: text/html`.** curl and MockMvc both send `*/*`, which the
platform answers with a bare 401; only a request that prefers HTML gets the 302 to Authentik. Asserting
the 401 and calling the page broken is a mistake worth not making twice.

Known gap: `StorageService` has no delete, so removing a product or a photo leaves the object in the
bucket. Harmless — nothing links to it — but it accumulates.

## Photos

Re-encoded to webp and uploaded to the bucket once (50 MB of originals → 14 MB), served with a
year-long cache. `site.assets.base-url` says where they live. The originals remain in this repo's
history. EXIF (including GPS from phone photos) is stripped by the re-encode.

## Local development

```bash
# the whole site, admin included (needs Postgres on :5432 with an itsthevine database)
mvn spring-boot:run     # http://localhost:8080

# the stylesheet, while editing templates — watches and recompiles
cd src/main/styles && npm install && npm run watch
```

`/admin` only exists when `SECURITY_MODE=OIDC`, so a plain local run has the site and no admin. To work
on the admin without an identity provider, run with `SECURITY_MODE=OIDC`, dummy
`spring.security.oauth2.client.*` values (see `AdminPagesTest` for a set that starts without touching
the network) and `platform.security.authenticated-paths=/nothing/**` so nothing asks you to sign in.

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
