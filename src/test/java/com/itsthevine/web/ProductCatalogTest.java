package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Exercises the catalogue against the real seeded data, so the migration is covered too. */
@SpringBootTest
@Testcontainers
class ProductCatalogTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("site.assets.base-url", () -> "https://s3.example.test/itsthevine");
        // The contact starter refuses to start on a blank recipient, and this app has a
        // ContactController, so the context needs one even to test the catalogue.
        registry.add("platform.contact.to", () -> "test@example.com");
        registry.add("platform.contact.from", () -> "noreply@example.com");
        registry.add("platform.storage.access-key", () -> "test");
        registry.add("platform.storage.secret-key", () -> "test");
    }

    @Autowired
    ProductCatalog catalog;

    @Test
    void theWholeCatalogueSurvivedTheMigrationFromTypeScript() {
        assertThat(catalog.list(null)).hasSize(40);
        assertThat(catalog.list("All")).hasSize(40);
        assertThat(catalog.list(null).stream().mapToLong(p -> p.images().size()).sum()).isEqualTo(80);
    }

    @Test
    void keepsTheCuratedOrderRatherThanIdOrAlphabetical() {
        List<ProductCatalog.ProductView> all = catalog.list(null);
        assertThat(all.get(0).name()).isEqualTo("76th Birthday Cake");
        assertThat(all).extracting(ProductCatalog.ProductView::name).doesNotHaveDuplicates();
    }

    @Test
    void filtersByCategoryServerSide() {
        List<ProductCatalog.ProductView> cakes = catalog.list("Cakes");
        assertThat(cakes).isNotEmpty();
        assertThat(cakes).allSatisfy(p -> assertThat(p.category()).isEqualTo("Cakes"));
        assertThat(cakes).hasSizeLessThan(40);
    }

    @Test
    void anUnknownCategoryIsEmptyRatherThanEverything() {
        // Returning the full catalogue for a bad filter would quietly lie about what's in it.
        assertThat(catalog.list("Sourdough")).isEmpty();
    }

    @Test
    void offersTheFilterButtonsInTheOrderTheSiteAlwaysUsed() {
        assertThat(catalog.categories())
                .containsExactly("All", "Cookies", "Cakes", "Rolls", "Pie", "Brownies", "Pastries");
    }

    @Test
    void buildsAbsoluteImageUrlsAndEncodesSpaces() {
        List<String> images = catalog.list(null).stream().flatMap(p -> p.images().stream()).toList();
        assertThat(images).allSatisfy(url ->
                assertThat(url).startsWith("https://s3.example.test/itsthevine/images/"));
        // A raw space would not fetch; '+' (form encoding) would 404 against the bucket.
        assertThat(images).noneMatch(url -> url.contains(" ") || url.contains("+"));
    }
}
