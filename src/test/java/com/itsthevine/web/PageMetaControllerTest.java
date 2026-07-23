package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Runs against the REAL frontend/index.html rather than a fixture: the controller finds its tags by
 * pattern, so reformatting that file is exactly how this would silently break. Here it fails the build
 * instead.
 */
class PageMetaControllerTest {

    private static final File INDEX = new File("frontend/index.html");

    private static PageMetaController controller() {
        DefaultResourceLoader loader = new DefaultResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return new FileSystemResource(INDEX);
            }
        };
        return new PageMetaController(loader, "https://itsthevine.com");
    }

    private static String get(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return controller().page(request);
    }

    @Test
    void theIndexTemplateIsWhereTheControllerExpects() {
        assertThat(INDEX).exists();
    }

    @Test
    void productsPageGetsItsOwnTitleAndDescription() {
        String html = get("/products");

        assertThat(html).contains("<title>Our products · The Vine Coffeehouse + Bakery</title>");
        assertThat(html).contains("<meta name=\"description\" content=\"Cinnamon rolls, caramel rolls");
        assertThat(html).contains("<meta property=\"og:title\" content=\"Our products · The Vine");
        assertThat(html).contains("<meta property=\"og:url\" content=\"https://itsthevine.com/products\">");
    }

    @Test
    void everyRouteIsRewritten() {
        // A route the controller maps but forgot to describe would silently serve the homepage's
        // metadata, which is worse than none — it tells a crawler two URLs are the same page.
        assertThat(get("/history")).contains("<title>Our story · ");
        assertThat(get("/contact")).contains("<title>Contact us · ");
        assertThat(get("/")).contains("<title>The Vine Coffeehouse + Bakery</title>");
    }

    @Test
    void theHomepageOgUrlHasNoTrailingSlash() {
        assertThat(get("/")).contains("<meta property=\"og:url\" content=\"https://itsthevine.com\">");
    }

    @Test
    void noDefaultMetadataSurvivesOnASubPage() {
        // The template ships with the homepage copy. If a replacement misses, that copy leaks onto
        // every page and the whole exercise is pointless.
        String html = get("/contact");
        assertThat(html).doesNotContain("A locally owned coffeehouse and bakery in downtown Princeville, Illinois. We bake");
        assertThat(html).doesNotContain("<title>The Vine Coffeehouse + Bakery</title>");
    }

    @Test
    void theAppShellIsStillIntact() {
        // Rewriting the head must not disturb what actually boots the SPA.
        String html = get("/products");
        assertThat(html).contains("<div id=\"root\"></div>");
        assertThat(html).contains("/src/main.tsx");
    }
}
