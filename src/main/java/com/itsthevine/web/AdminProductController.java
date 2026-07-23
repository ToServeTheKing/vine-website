package com.itsthevine.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.itsthevine.web.domain.Product;
import com.itsthevine.web.domain.ProductRepository;

/**
 * Editing the catalogue from the site, so a new cake is a photo and a name rather than a migration.
 *
 * The whole controller is conditional on OIDC being switched on. That is deliberate belt-and-braces:
 * the platform's permit-all filter chain is what runs when {@code platform.security.mode} is unset,
 * so if these endpoints existed unconditionally a deployment that forgot to configure Authentik
 * would be publishing catalogue writes to the open internet. Gated this way, "no auth configured"
 * means "no admin endpoints" — they 404 like any other unknown path, which is also what the platform
 * web contract expects of {@code /api/**}.
 */
@RestController
@RequestMapping("/api/admin/products")
@ConditionalOnProperty(prefix = "platform.security", name = "mode", havingValue = "OIDC")
public class AdminProductController {

    private final ProductRepository products;
    private final ProductPhotoService photos;
    private final ProductCatalog catalog;

    public AdminProductController(ProductRepository products, ProductPhotoService photos, ProductCatalog catalog) {
        this.products = products;
        this.photos = photos;
        this.catalog = catalog;
    }

    /**
     * What the editor sees: the catalogue in display order.
     *
     * {@code images} and {@code keys} are the same photos in the same order — the URLs to show and the
     * identifiers to arrange by. The public view only needs the former, but an editor rearranging
     * photos has to name them back to us, and the URL is a rendering of the key rather than the key
     * itself.
     */
    public record AdminView(Long id, String name, String category, int position,
                            List<String> images, List<String> keys) {}

    public record Details(String name, String category) {}

    public record Order(List<Long> ids) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<AdminView> list() {
        return products.findAllByOrderByPositionAsc().stream().map(this::toView).toList();
    }

    /**
     * New items go to the front — the newest work is what's worth showing first, and it saves the
     * editor a reorder after every upload.
     */
    @PostMapping
    @Transactional
    public AdminView create(@RequestParam String name,
                            @RequestParam String category,
                            @RequestParam("photos") List<MultipartFile> files) {
        String cleanName = required(name, "Please give it a name.");
        String cleanCategory = required(category, "Please choose a category.");
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Please add at least one photo.");
        }

        List<String> keys = new ArrayList<>();
        for (MultipartFile file : files) {
            keys.add(photos.store(bytes(file), file.getOriginalFilename(), cleanName));
        }

        Product saved = products.save(new Product(cleanName, cleanCategory, 0, keys));
        renumberWithFirst(saved);
        return toView(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public AdminView describe(@PathVariable Long id, @RequestBody Details details) {
        Product product = find(id);
        product.describe(required(details.name(), "Please give it a name."),
                required(details.category(), "Please choose a category."));
        return toView(products.save(product));
    }

    @PostMapping("/{id}/photos")
    @Transactional
    public AdminView addPhotos(@PathVariable Long id, @RequestParam("photos") List<MultipartFile> files) {
        Product product = find(id);
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Please choose a photo to add.");
        }
        List<String> keys = new ArrayList<>(product.getImageKeys());
        for (MultipartFile file : files) {
            keys.add(photos.store(bytes(file), file.getOriginalFilename(), product.getName()));
        }
        product.replacePhotos(keys);
        return toView(products.save(product));
    }

    /**
     * Reordering and removal both arrive as the full list the editor arranged, so the stored order is
     * whatever they last saw rather than the result of replaying moves.
     */
    @PutMapping("/{id}/photos")
    @Transactional
    public AdminView arrangePhotos(@PathVariable Long id, @RequestBody List<String> keys) {
        Product product = find(id);
        List<String> existing = product.getImageKeys();
        List<String> arranged = keys.stream().filter(existing::contains).distinct().toList();
        if (arranged.isEmpty()) {
            throw new IllegalArgumentException("An item needs at least one photo.");
        }
        product.replacePhotos(arranged);
        return toView(products.save(product));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        products.delete(find(id));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** The ids in the order they should appear; anything omitted keeps its relative place after them. */
    @PutMapping("/order")
    @Transactional
    public List<AdminView> reorder(@RequestBody Order order) {
        List<Product> all = products.findAllByOrderByPositionAsc();
        List<Product> arranged = new ArrayList<>();
        for (Long id : order.ids()) {
            all.stream().filter(p -> p.getId().equals(id)).findFirst().ifPresent(arranged::add);
        }
        all.stream().filter(p -> !arranged.contains(p)).forEach(arranged::add);
        renumber(arranged);
        return arranged.stream().map(this::toView).toList();
    }

    private void renumberWithFirst(Product first) {
        List<Product> arranged = new ArrayList<>();
        arranged.add(first);
        products.findAllByOrderByPositionAsc().stream()
                .filter(p -> !p.getId().equals(first.getId()))
                .forEach(arranged::add);
        renumber(arranged);
    }

    /**
     * {@code product.position} has no unique constraint, so ordering is a full renumber rather than a
     * swap — forty rows, once in a while, from one editor.
     */
    private void renumber(List<Product> arranged) {
        int position = 1;
        for (Product product : arranged) {
            product.moveTo(position++);
        }
        products.saveAll(arranged);
    }

    private Product find(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("That item no longer exists."));
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the uploaded photo.", e);
        }
    }

    private static String required(String value, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    /** Reuses the catalogue's URL building so admin and public pages can never disagree about a photo. */
    private AdminView toView(Product product) {
        ProductCatalog.ProductView view = catalog.view(product);
        return new AdminView(view.id(), view.name(), view.category(), product.getPosition(),
                view.images(), List.copyOf(product.getImageKeys()));
    }
}
