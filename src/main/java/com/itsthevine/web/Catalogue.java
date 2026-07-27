package com.itsthevine.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itsthevine.web.domain.Category;
import com.itsthevine.web.domain.CategoryRepository;
import com.itsthevine.web.domain.Product;
import com.itsthevine.web.domain.ProductRepository;

/**
 * Editing the catalogue: what's on the products page, in what order, under which filter, with which
 * photos.
 *
 * <p>This is the logic that used to sit in {@code AdminProductController} and
 * {@code AdminCategoryController} when the admin was a React screen talking JSON. The screen is now
 * server-rendered forms, and a form can only POST — so "move this up" arrives as an action rather than
 * as the whole re-ordered list the browser had arranged. The reordering therefore happens here, which is
 * where it should always have been: the browser was only ever telling us what it had already worked out.
 */
@Service
public class Catalogue {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductPhotoService photos;
    private final SitePhotos urls;

    public Catalogue(ProductRepository products, CategoryRepository categories,
                     ProductPhotoService photos, SitePhotos urls) {
        this.products = products;
        this.categories = categories;
        this.photos = photos;
        this.urls = urls;
    }

    /**
     * A photo, as the editor needs it: the URL to look at and the key to name it by. Same pairing the
     * React screen kept as two parallel arrays — a template can just walk the pairs.
     */
    public record Photo(String key, String url) {}

    public record Item(Long id, String name, String category, List<Photo> photos) {}

    /** {@code used} tells the editor whether deleting a filter would strand anything. */
    public record Filter(Long id, String name, long used) {}

    @Transactional(readOnly = true)
    public List<Item> items() {
        return products.findAllByOrderByPositionAsc().stream().map(this::toItem).toList();
    }

    @Transactional(readOnly = true)
    public List<Filter> filters() {
        List<Product> all = products.findAllByOrderByPositionAsc();
        return categories.findAllByOrderByPositionAsc().stream()
                .map(c -> new Filter(c.getId(), c.getName(), count(all, c.getName())))
                .toList();
    }

    // --- items ---------------------------------------------------------------

    /**
     * New items go to the front — the newest work is what's worth showing first, and it saves the editor
     * a reorder after every upload.
     */
    @Transactional
    public void addItem(String name, String category, List<MultipartFile> files) {
        String cleanName = required(name, "Please give it a name.");
        String cleanCategory = required(category, "Please choose a category.");
        List<MultipartFile> chosen = real(files);
        if (chosen.isEmpty()) {
            throw new IllegalArgumentException("Please add at least one photo.");
        }

        List<String> keys = new ArrayList<>();
        for (MultipartFile file : chosen) {
            keys.add(photos.store(bytes(file), file.getOriginalFilename(), cleanName));
        }

        Product saved = products.save(new Product(cleanName, cleanCategory, 0, keys));
        List<Product> arranged = new ArrayList<>();
        arranged.add(saved);
        products.findAllByOrderByPositionAsc().stream()
                .filter(p -> !p.getId().equals(saved.getId()))
                .forEach(arranged::add);
        renumber(arranged);
    }

    @Transactional
    public void describeItem(Long id, String name, String category) {
        Product product = item(id);
        product.describe(required(name, "Please give it a name."),
                required(category, "Please choose a category."));
        products.save(product);
    }

    @Transactional
    public void addPhotos(Long id, List<MultipartFile> files) {
        Product product = item(id);
        List<MultipartFile> chosen = real(files);
        if (chosen.isEmpty()) {
            throw new IllegalArgumentException("Please choose a photo to add.");
        }
        List<String> keys = new ArrayList<>(product.getImageKeys());
        for (MultipartFile file : chosen) {
            keys.add(photos.store(bytes(file), file.getOriginalFilename(), product.getName()));
        }
        product.replacePhotos(keys);
        products.save(product);
    }

    /** Order matters: the first photo is the one the products page leads with. */
    @Transactional
    public void movePhoto(Long id, String key, int delta) {
        Product product = item(id);
        List<String> keys = new ArrayList<>(product.getImageKeys());
        int at = keys.indexOf(key);
        if (at < 0) {
            throw new IllegalArgumentException("That photo isn't on this item any more. Reload the page.");
        }
        int to = at + delta;
        if (to < 0 || to >= keys.size()) {
            // Already at an end. Nothing to do, and nothing to complain about — the button that asked
            // for this is disabled in the page anyway.
            return;
        }
        swap(keys, at, to);
        product.replacePhotos(keys);
        products.save(product);
    }

    @Transactional
    public void removePhoto(Long id, String key) {
        Product product = item(id);
        List<String> keys = new ArrayList<>(product.getImageKeys());
        if (!keys.remove(key)) {
            throw new IllegalArgumentException("That photo isn't on this item any more. Reload the page.");
        }
        if (keys.isEmpty()) {
            // The card would have nothing to show. Deleting the item is a different, deliberate act.
            throw new IllegalArgumentException("An item needs at least one photo.");
        }
        product.replacePhotos(keys);
        products.save(product);
    }

    @Transactional
    public void removeItem(Long id) {
        products.delete(item(id));
    }

    @Transactional
    public void moveItem(Long id, int delta) {
        List<Product> all = products.findAllByOrderByPositionAsc();
        int at = at(all.stream().map(Product::getId).toList(), id, "That item no longer exists.");
        int to = at + delta;
        if (to < 0 || to >= all.size()) {
            return;
        }
        swap(all, at, to);
        renumber(all);
    }

    // --- filters -------------------------------------------------------------

    @Transactional
    public void addFilter(String name) {
        String clean = required(name, "Please give the category a name.");
        categories.findByNameIgnoreCase(clean).ifPresent(existing -> {
            throw new IllegalStateException("There's already a " + existing.getName() + " category.");
        });
        int last = categories.findAllByOrderByPositionAsc().stream()
                .mapToInt(Category::getPosition).max().orElse(0);
        categories.save(new Category(clean, last + 1));
    }

    /**
     * Renaming carries the products with it. They store the category by name, so without this the rename
     * would orphan everything filed under the old one — it would drop off the filter and reappear at the
     * end as an unlisted category.
     */
    @Transactional
    public void renameFilter(Long id, String name) {
        Category category = filter(id);
        String clean = required(name, "Please give the category a name.");
        categories.findByNameIgnoreCase(clean)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("There's already a " + other.getName() + " category.");
                });

        String previous = category.getName();
        category.rename(clean);
        categories.save(category);

        List<Product> filed = products.findAllByCategoryOrderByPositionAsc(previous);
        filed.forEach(p -> p.describe(p.getName(), clean));
        products.saveAll(filed);
    }

    @Transactional
    public void moveFilter(Long id, int delta) {
        List<Category> all = categories.findAllByOrderByPositionAsc();
        int at = at(all.stream().map(Category::getId).toList(), id, "That category no longer exists.");
        int to = at + delta;
        if (to < 0 || to >= all.size()) {
            return;
        }
        swap(all, at, to);
        int position = 1;
        for (Category category : all) {
            category.moveTo(position++);
        }
        categories.saveAll(all);
    }

    @Transactional
    public void removeFilter(Long id) {
        Category category = filter(id);
        long used = count(products.findAllByOrderByPositionAsc(), category.getName());
        if (used > 0) {
            // Refuse rather than cascade: deleting the button shouldn't quietly decide what happens to
            // the items behind it.
            throw new IllegalStateException(used + " item" + (used == 1 ? " is" : "s are")
                    + " still filed under " + category.getName() + ". Move them first.");
        }
        categories.delete(category);
    }

    // --- plumbing ------------------------------------------------------------

    /** Reuses the catalogue's URL building so the admin and the shop front agree about a photo. */
    private Item toItem(Product product) {
        List<Photo> pictures = product.getImageKeys().stream()
                .map(key -> new Photo(key, urls.of(key)))
                .toList();
        return new Item(product.getId(), product.getName(), product.getCategory(), pictures);
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

    private Product item(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("That item no longer exists."));
    }

    private Category filter(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("That category no longer exists."));
    }

    private static int at(List<Long> ids, Long id, String gone) {
        int at = ids.indexOf(id);
        if (at < 0) {
            throw new IllegalArgumentException(gone);
        }
        return at;
    }

    private static <T> void swap(List<T> items, int a, int b) {
        T held = items.get(a);
        items.set(a, items.get(b));
        items.set(b, held);
    }

    private static long count(List<Product> all, String category) {
        return all.stream().filter(p -> p.getCategory().equalsIgnoreCase(category)).count();
    }

    /** An empty file input still posts a part, with no filename and no bytes. */
    private static List<MultipartFile> real(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> !f.isEmpty()).toList();
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
}
