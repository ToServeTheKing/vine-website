package com.itsthevine.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves {@code index.html} with per-page title/description/OG tags filled in.
 *
 * <p>The site used to be server-rendered by Next, so every page came with its own metadata. A plain
 * SPA would hand crawlers and link-preview scrapers one generic shell for all four pages — a real
 * loss for a shop that people find by searching. Rendering just the {@code <head>} on the server keeps
 * that, without dragging SSR (and a Node runtime) into the one-jar model.
 *
 * <p>Only the four real routes are listed. Anything else falls through to the platform's SPA
 * fallback, which is what we want for 404s — no invented metadata for URLs that don't exist.
 */
@Controller
public class PageMetaController {

    private static final Logger log = LoggerFactory.getLogger(PageMetaController.class);

    private static final String NAME = "The Vine Coffeehouse + Bakery";
    private static final String HOME_DESCRIPTION =
            "A locally owned coffeehouse and bakery in downtown Princeville, Illinois. We bake pastries, "
            + "custom cakes, cookies, and cinnamon rolls, and serve sandwiches, paninis, and coffee.";

    private record PageMeta(String title, String description) {}

    private static final Map<String, PageMeta> PAGES = new LinkedHashMap<>(Map.of(
            "/", new PageMeta(NAME, HOME_DESCRIPTION),
            "/products", new PageMeta("Our products · " + NAME,
                    "Cinnamon rolls, caramel rolls, scones, cookie bars, macarons, brownies, pies, and "
                    + "made-to-order cakes and decorated cookies from The Vine in Princeville, Illinois."),
            "/history", new PageMeta("Our story · " + NAME,
                    "Morissa Bennett opened The Vine in 2024 at 215 E Main Street in downtown Princeville, "
                    + "Illinois. We bake in our own kitchen on Main Street."),
            "/contact", new PageMeta("Contact us · " + NAME,
                    "Get in touch with The Vine Coffeehouse + Bakery, 215 E Main Street, Princeville, "
                    + "Illinois. Call (309) 701-0660 or send us a message.")));

    private static final Pattern TITLE = Pattern.compile("<title>.*?</title>", Pattern.DOTALL);

    private final ResourceLoader resourceLoader;
    private final String baseUrl;

    /** Cached because the file never changes at runtime — it's baked into the jar. */
    private volatile String template;

    public PageMetaController(ResourceLoader resourceLoader,
                              @Value("${site.base-url:https://itsthevine.com}") String baseUrl) {
        this.resourceLoader = resourceLoader;
        this.baseUrl = baseUrl;
    }

    @GetMapping(value = {"/", "/products", "/history", "/contact"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String page(HttpServletRequest request) {
        String path = request.getRequestURI();
        PageMeta meta = PAGES.getOrDefault(path, PAGES.get("/"));
        String html = template();
        if (html == null) {
            // No built SPA (backend-only build). Nothing to decorate.
            return "<!doctype html><title>" + escape(meta.title()) + "</title>";
        }
        return render(html, meta, path);
    }

    private String render(String html, PageMeta meta, String path) {
        String out = TITLE.matcher(html).replaceFirst(
                Matcher.quoteReplacement("<title>" + escape(meta.title()) + "</title>"));
        out = setMeta(out, "name", "description", meta.description());
        out = setMeta(out, "property", "og:title", meta.title());
        out = setMeta(out, "property", "og:description", meta.description());
        out = setMeta(out, "property", "og:url", baseUrl + ("/".equals(path) ? "" : path));
        return out;
    }

    /**
     * Rewrites the {@code content} of an existing meta tag. Deliberately does not add missing tags —
     * index.html carries the full set, so a miss here means the template changed and should be fixed
     * there rather than papered over with a duplicate tag.
     */
    private static String setMeta(String html, String keyAttr, String key, String value) {
        Pattern p = Pattern.compile(
                "(<meta\\s+" + keyAttr + "=\"" + Pattern.quote(key) + "\"\\s+content=\")[^\"]*(\")");
        Matcher m = p.matcher(html);
        if (!m.find()) {
            log.warn("index.html has no <meta {}=\"{}\"> to fill in", keyAttr, key);
            return html;
        }
        // Splice by index rather than replaceFirst: replacement strings give $ and \ special meaning,
        // and these values are prose.
        return new StringBuilder(html)
                .replace(m.start(), m.end(), m.group(1) + escape(value) + m.group(2))
                .toString();
    }

    private String template() {
        String cached = template;
        if (cached == null) {
            synchronized (this) {
                if (template == null) {
                    template = load();
                }
                cached = template;
            }
        }
        return cached.isEmpty() ? null : cached;
    }

    private String load() {
        Resource resource = resourceLoader.getResource("classpath:/static/index.html");
        if (!resource.exists()) {
            log.warn("no classpath:/static/index.html — serving pages without metadata");
            return "";
        }
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("could not read index.html", e);
            return "";
        }
    }

    /** Escapes for both element text and double-quoted attribute values. */
    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
