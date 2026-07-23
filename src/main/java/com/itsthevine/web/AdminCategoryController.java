package com.itsthevine.web;

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
import org.springframework.web.bind.annotation.RestController;

import com.itsthevine.web.domain.Category;
import com.itsthevine.web.domain.CategoryRepository;
import com.itsthevine.web.domain.Product;
import com.itsthevine.web.domain.ProductRepository;

/**
 * The filter buttons, editable. Gated on OIDC for the same reason as the product admin: with no
 * identity provider configured these endpoints shouldn't exist at all.
 */
@RestController
@RequestMapping("/api/admin/categories")
@ConditionalOnProperty(prefix = "platform.security", name = "mode", havingValue = "OIDC")
public class AdminCategoryController {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public AdminCategoryController(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    /** {@code used} tells the editor whether deleting it would strand anything. */
    public record AdminView(Long id, String name, int position, long used) {}

    public record Name(String name) {}

    public record Order(List<Long> ids) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<AdminView> list() {
        List<Product> all = products.findAllByOrderByPositionAsc();
        return categories.findAllByOrderByPositionAsc().stream()
                .map(c -> new AdminView(c.getId(), c.getName(), c.getPosition(), count(all, c.getName())))
                .toList();
    }

    @PostMapping
    @Transactional
    public AdminView create(@RequestBody Name body) {
        String name = required(body.name());
        categories.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new IllegalStateException("There's already a " + existing.getName() + " category.");
        });
        int last = categories.findAllByOrderByPositionAsc().stream()
                .mapToInt(Category::getPosition).max().orElse(0);
        Category saved = categories.save(new Category(name, last + 1));
        return new AdminView(saved.getId(), saved.getName(), saved.getPosition(), 0);
    }

    /**
     * Renaming carries the products with it. They store the category by name, so without this the
     * rename would orphan everything filed under the old one — it would drop off the filter and
     * reappear at the end as an unlisted category.
     */
    @PutMapping("/{id}")
    @Transactional
    public AdminView rename(@PathVariable Long id, @RequestBody Name body) {
        Category category = find(id);
        String name = required(body.name());
        categories.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("There's already a " + other.getName() + " category.");
                });

        String previous = category.getName();
        category.rename(name);
        categories.save(category);

        List<Product> filed = products.findAllByCategoryOrderByPositionAsc(previous);
        filed.forEach(p -> p.describe(p.getName(), name));
        products.saveAll(filed);

        return new AdminView(category.getId(), category.getName(), category.getPosition(), filed.size());
    }

    @PutMapping("/order")
    @Transactional
    public List<AdminView> reorder(@RequestBody Order order) {
        List<Category> all = categories.findAllByOrderByPositionAsc();
        List<Category> arranged = new ArrayList<>();
        for (Long id : order.ids()) {
            all.stream().filter(c -> c.getId().equals(id)).findFirst().ifPresent(arranged::add);
        }
        all.stream().filter(c -> !arranged.contains(c)).forEach(arranged::add);

        int position = 1;
        for (Category category : arranged) {
            category.moveTo(position++);
        }
        categories.saveAll(arranged);

        List<Product> everything = products.findAllByOrderByPositionAsc();
        return arranged.stream()
                .map(c -> new AdminView(c.getId(), c.getName(), c.getPosition(), count(everything, c.getName())))
                .toList();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Category category = find(id);
        long used = count(products.findAllByOrderByPositionAsc(), category.getName());
        if (used > 0) {
            // Refuse rather than cascade: deleting the button shouldn't quietly decide what happens to
            // the items behind it.
            throw new IllegalStateException(
                    used + " item" + (used == 1 ? " is" : "s are") + " still filed under "
                            + category.getName() + ". Move them first.");
        }
        categories.delete(category);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private Category find(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("That category no longer exists."));
    }

    private static long count(List<Product> all, String category) {
        return all.stream().filter(p -> p.getCategory().equalsIgnoreCase(category)).count();
    }

    private static String required(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Please give the category a name.");
        }
        return trimmed;
    }
}
