# Contributing

This is one of the six sites on the [Bennett platform](https://git.thebennett.net/austin/platform):
a Spring Boot 4 / Java 25 backend serving a Vite + React SPA from a single jar. The workflow below is
the same for every app — once you know one, you know all of them.

**The one rule:** `main` is protected. You cannot push to it. Every change lands through a pull request
whose CI passed. There is no exception, including for the repo owner — the server enforces it, so a
`git push origin main` will be rejected.

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 25 | `java -version` should say 25 |
| Maven | 3.9+ | or use the platform image below |
| Docker | any recent | **required to run the tests** — they use Testcontainers (a real Postgres) |
| Node | — | *not* installed by hand; the Maven build downloads a pinned Node and runs the SPA build |

You also need a **Gitea Maven token** to build, because the platform (`net.thebennett.platform:*`) is
pulled from the Gitea package registry, not Maven Central. Create a token at
`git.thebennett.net → Settings → Applications` with `read:package` scope, then export it:

```sh
export MAVEN_USER=<your-gitea-username>
export MAVEN_TOKEN=<the-token>
```

The repo ships `.gitea/ci-settings.xml`, which wires that registry up using those two variables — pass
it with `-s` and you never have to touch your global `~/.m2/settings.xml`.

---

## Build and test locally

```sh
# Full verify — compiles, builds the SPA, runs every test (needs Docker running).
mvn -B -s .gitea/ci-settings.xml verify

# Faster inner loop — skip the frontend build when you're only touching Java.
mvn -B -s .gitea/ci-settings.xml -DskipFrontend=true verify

# Run the app locally (Postgres + any OIDC/storage config supplied via env — see the app's README).
mvn -s .gitea/ci-settings.xml spring-boot:run
```

If `verify` fails with a Docker or "could not start container" error, the daemon isn't running — the
tests need it. Tests will not be skipped to work around this; a green build means the tests actually ran.

### Frontend

The SPA lives in `frontend/`. For a fast UI loop with hot reload:

```sh
cd frontend
npm install
npm run dev        # Vite dev server, proxying /api to the running backend
```

For anything that ships, let Maven build it (`mvn verify` runs `npm ci && npm run build` and folds
`dist/` into the jar) so what you test is what deploys. The frontend is **vendored per app** — there is
no shared frontend package, so a shared component you change here does not change in the other apps.

### Tests

- Name test classes `*Test` — Surefire runs those. A class named `*IT` is silently skipped.
- Integration tests use Testcontainers (`@ServiceConnection` + a real `postgres` container). No mocks
  for the database.
- Every app inherits a set of **contract tests** from `platform-starter-test` (unknown `/api` paths 404,
  health is UP, probes exist, SPA routes forward to `index.html`). You get these for free by extending
  the platform's base test — don't reimplement them.
- Protected-page paths return **302 for a browser** (`Accept: text/html`) and **401 for a fetch/XHR**
  (`Accept: */*`). If you're asserting that `/admin` "redirects to login," send the browser Accept
  header or the assertion will see a 401 and mislead you.

---

## The change workflow

```sh
git switch -c feature/short-description        # branch off main
# ... make your change, add tests ...
mvn -B -s .gitea/ci-settings.xml verify        # green locally before you push
git push -u origin feature/short-description    # push the BRANCH, never main
```

Then open a pull request against `main` (Gitea → *New Pull Request*, or `tea pr create`). CI runs on the
PR. When the check is green, merge it. On merge the image builds, publishes, and deploys automatically.

### What CI does (`.gitea/workflows/build.yml`, job `build-and-publish / build`)

Runs on every PR **and** on push to `main`:

1. **Test** — runs `mvn verify` in a sibling Maven container (Testcontainers can't run inside
   `docker build`). Tests gate both the build and the merge.
2. **Build image** — `docker build` (which also runs the SPA build); a broken Dockerfile fails here.
3. **Scan image (Trivy)** — fails on any fixable **HIGH/CRITICAL** library CVE. A new bad CVE blocks
   the merge even if your code is fine — bump the offending dependency.
4. **Push image** — **only on push to `main`** (`if: github.event_name != 'pull_request'`). PRs build
   and scan but never publish.

`main` requires the check **`build-and-publish / build (pull_request)`** to be green before merge. That
context name is what branch protection is pinned to; if you rename the workflow or job, update the
required-check setting to match or nothing will ever be mergeable.

Commits that only touch `**.md` or `renovate.json` skip the build on `main` (they can't change the
image) — but a PR that touches them still runs the PR check.

---

## Dependencies

Renovate watches this repo. Platform version bumps (`net.thebennett.platform`) are **auto-merged** once
their PR is green — that's how a security fix in the platform reaches every app without hand-editing six
`pom.xml` files. Other updates open PRs for a human to merge. Don't hand-bump the platform version to
chase a fix; cut a platform release and let Renovate carry it here.

Some toolchain upgrades are intentionally pinned back (e.g. ESLint / TS majors blocked by plugin peer
caps). If a Renovate PR is red on a peer-dependency conflict, that's why — don't force it.

---

## Deploy

Merge to `main` → CI publishes `:latest` and `:<sha>` to the Gitea registry → **Watchtower** pulls the
new `:latest` and restarts the container → **Caddy** fronts it. There is no manual deploy step and no
staging hop; the PR gate *is* the safety net, which is why the tests and scan are non-negotiable.

To force an urgent redeploy without a code change (e.g. rolling out a platform fix immediately), trigger
the workflow manually (`workflow_dispatch`) rather than pushing an empty commit.

---

## Conventions

- **Business logic lives in Java.** This is a Spring app, not a React app with a Java proxy — the server
  owns the data, the rules, and the validation. The SPA renders and calls `/api`. Prose and layout stay
  in the markup; only the parts that change without the page changing (prices, lists, links) become data.
- Keep commits scoped and their messages in the imperative ("add booking link", not "added").
- Don't commit secrets. Config comes from environment variables at deploy time (see the app's README for
  the list); there is no `.env` in the repo.
- Update the README when you change what the server owns or what env vars it needs — that's the file the
  next person reads first.
