package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.itsthevine.web.domain.ProductRepository;

/**
 * The admin catalogue operations. Whether they're reachable without a login is covered separately by
 * {@link AdminSecurityTest} — this is about what they do once you're in.
 */
@SpringBootTest(properties = {
        "platform.contact.to=test@example.com",
        "platform.contact.from=noreply@example.com",
        "platform.storage.access-key=test",
        "platform.storage.secret-key=test",
        "site.assets.base-url=https://s3.example.test/itsthevine"
})
@Testcontainers
class AdminControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    AdminController admin;

    @Autowired
    ProductCatalog catalog;

    @Autowired
    ProductRepository products;

    private static AdminController.ProductForm form(String name, String category, List<String> keys) {
        return new AdminController.ProductForm(name, category, null, keys);
    }

    @Test
    void createsAProductAndItAppearsOnThePublicSite() {
        int before = catalog.list(null).size();

        var created = admin.create(form("Test Loaf", "Rolls", List.of("products/test-loaf.webp")));

        assertThat(created.id()).isNotNull();
        assertThat(catalog.list(null)).hasSize(before + 1);
        assertThat(catalog.list("Rolls"))
                .extracting(ProductCatalog.ProductView::name)
                .contains("Test Loaf");

        admin.delete(created.id());
    }

    @Test
    void aNewProductGoesToTheEndRatherThanDisplacingOne() {
        // Position defaults matter: reusing an existing one would reorder the curated catalogue.
        int maxBefore = admin.list().stream().mapToInt(AdminController.AdminProduct::position).max().orElse(0);

        var created = admin.create(form("末 Loaf", "Rolls", List.of("products/x.webp")));

        assertThat(created.position()).isGreaterThan(maxBefore);
        admin.delete(created.id());
    }

    @Test
    void editingReplacesTheFieldsAndKeepsTheOrderOfPhotos() {
        var created = admin.create(form("Before", "Cakes", List.of("products/a.webp", "products/b.webp")));

        var updated = admin.update(created.id(),
                new AdminController.ProductForm("After", "Pie", 3,
                        List.of("products/b.webp", "products/a.webp", "products/c.webp")));

        assertThat(updated.name()).isEqualTo("After");
        assertThat(updated.category()).isEqualTo("Pie");
        assertThat(updated.position()).isEqualTo(3);
        assertThat(updated.imageKeys())
                .containsExactly("products/b.webp", "products/a.webp", "products/c.webp");

        admin.delete(created.id());
    }

    @Test
    void aProductWithoutAPhotoIsRejected() {
        // The card is a photo with a caption; without one it renders as an empty square.
        assertThatThrownBy(() -> admin.create(form("No photo", "Cakes", List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least one photo");
        assertThatThrownBy(() -> admin.create(form("No photo", "Cakes", List.of("  "))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void aProductWithoutANameOrCategoryIsRejected() {
        assertThatThrownBy(() -> admin.create(form(" ", "Cakes", List.of("products/a.webp"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("name");
        assertThatThrownBy(() -> admin.create(form("Thing", " ", List.of("products/a.webp"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("category");
    }

    @Test
    void editingSomethingThatIsGoneIs404NotACrash() {
        assertThatThrownBy(() -> admin.update(9_999_999L, form("x", "Cakes", List.of("products/a.webp"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        assertThatThrownBy(() -> admin.delete(9_999_999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void deletingRemovesItFromThePublicCatalogue() {
        var created = admin.create(form("Temporary", "Brownies", List.of("products/t.webp")));
        assertThat(catalog.list("Brownies")).extracting(ProductCatalog.ProductView::name).contains("Temporary");

        admin.delete(created.id());

        assertThat(catalog.list("Brownies")).extracting(ProductCatalog.ProductView::name)
                .doesNotContain("Temporary");
        assertThat(products.findById(created.id())).isEmpty();
    }

    @Test
    void adminListsCarryBothKeysAndUrlsSoTheEditorCanShowThumbnails() {
        var created = admin.create(form("Thumb", "Cookies", List.of("products/thumb.webp")));

        var found = admin.list().stream().filter(p -> p.id().equals(created.id())).findFirst().orElseThrow();
        assertThat(found.imageKeys()).containsExactly("products/thumb.webp");
        assertThat(found.imageUrls())
                .containsExactly("https://s3.example.test/itsthevine/images/products/thumb.webp");

        admin.delete(created.id());
    }
}
