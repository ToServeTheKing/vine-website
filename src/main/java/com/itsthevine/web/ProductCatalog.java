package com.itsthevine.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.itsthevine.web.domain.Category;
import com.itsthevine.web.domain.CategoryRepository;
import com.itsthevine.web.domain.Product;
import com.itsthevine.web.domain.ProductRepository;

/**
 * The products page's brains: which categories to offer, in what order, what's in each, and where the
 * photos are. All of this used to live in a TypeScript array shipped to the browser.
 */
@Service
public class ProductCatalog {

    /** The filter shown first — every category at once. Not a stored category. */
    public static final String ALL = "All";

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final String assetBaseUrl;

    public ProductCatalog(ProductRepository products,
                          CategoryRepository categories,
                          @Value("${site.assets.base-url:https://s3.thebennett.net/itsthevine}") String assetBaseUrl) {
        this.products = products;
        this.categories = categories;
        // A trailing slash here would produce '//images/...' — harmless on most servers, but it shows
        // up in every image URL on the page.
        this.assetBaseUrl = assetBaseUrl.replaceAll("/+$", "");
    }

    public record ProductView(Long id, String name, String category, List<String> images) {}

    /**
     * @param category a stored category, or {@link #ALL}/blank for everything
     */
    @Transactional(readOnly = true)
    public List<ProductView> list(String category) {
        List<Product> found = (!StringUtils.hasText(category) || ALL.equalsIgnoreCase(category))
                ? products.findAllByOrderByPositionAsc()
                : products.findAllByCategoryOrderByPositionAsc(category);
        return found.stream().map(this::toView).toList();
    }

    /**
     * The filter buttons, in display order, starting with "All".
     *
     * Order comes from the category table. Only categories something is actually filed under are
     * offered — an empty filter button is a dead end — and a category found on a product but missing
     * from the table still shows up (appended, alphabetically) rather than silently disappearing.
     */
    @Transactional(readOnly = true)
    public List<String> categories() {
        Set<String> present = products.findAllByOrderByPositionAsc().stream()
                .map(Product::getCategory)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> defined = categories.findAllByOrderByPositionAsc().stream()
                .map(Category::getName)
                .toList();

        List<String> ordered = new ArrayList<>();
        ordered.add(ALL);
        defined.stream().filter(present::contains).forEach(ordered::add);
        present.stream()
                .filter(c -> !defined.contains(c))
                .sorted(Comparator.naturalOrder())
                .forEach(ordered::add);
        return ordered;
    }

    /**
     * Public so the admin screens render photos through exactly the same URL building as the shop
     * front — an editor should never arrange something that looks different once it's live.
     */
    public ProductView view(Product product) {
        return toView(product);
    }

    private ProductView toView(Product p) {
        return new ProductView(p.getId(), p.getName(), p.getCategory(),
                p.getImageKeys().stream().map(this::imageUrl).toList());
    }

    /**
     * Some photo filenames contain spaces ("Cinnamon Rolls.webp"), and a raw space in a URL doesn't
     * fetch — so each path segment is encoded. {@code URLEncoder} is form-encoding, which differs
     * from path-encoding in exactly one way that matters here: it turns a space into '+'.
     */
    private String imageUrl(String key) {
        String encoded = Arrays.stream(key.replaceAll("^/+", "").split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
        return assetBaseUrl + "/images/" + encoded;
    }
}
