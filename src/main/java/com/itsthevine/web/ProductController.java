package com.itsthevine.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductCatalog catalog;

    public ProductController(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    /** @param category filter; omit (or pass "All") for the whole catalogue */
    @GetMapping("/api/products")
    public List<ProductCatalog.ProductView> products(
            @RequestParam(required = false) String category) {
        return catalog.list(category);
    }

    @GetMapping("/api/categories")
    public List<String> categories() {
        return catalog.categories();
    }
}
