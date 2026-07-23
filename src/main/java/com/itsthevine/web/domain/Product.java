package com.itsthevine.web.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import net.thebennett.platform.data.BaseEntity;

/** Something the bakery makes, with the photos that show it off. */
@Entity
@Table(name = "product")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 60)
    private String category;

    /** Display order on the products page; the catalogue is curated, not alphabetical. */
    @Column(name = "position", nullable = false)
    private int position;

    /**
     * Object keys, not URLs — where the bucket lives is deployment configuration, so the absolute
     * URL is built at the edge of the app ({@code ProductCatalog}) rather than baked into the data.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_image", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position")
    @Column(name = "image_key", nullable = false, length = 300)
    private List<String> imageKeys = new ArrayList<>();

    protected Product() {
        // for JPA
    }

    public Product(String name, String category, int position, List<String> imageKeys) {
        this.name = name;
        this.category = category;
        this.position = position;
        this.imageKeys = new ArrayList<>(imageKeys);
    }

    /** Rename and/or refile the item; both are free text the editor typed. */
    public void describe(String name, String category) {
        this.name = name;
        this.category = category;
    }

    /** Where this sits on the products page. Reordering renumbers the whole catalogue. */
    public void moveTo(int position) {
        this.position = position;
    }

    /**
     * Replace the photo list wholesale. {@code @OrderColumn} makes Hibernate rewrite the tail of the
     * collection on any insert or removal anyway, so there's nothing to gain from finer-grained
     * mutators — and one path in means the stored order always matches what the editor arranged.
     */
    public void replacePhotos(List<String> keys) {
        this.imageKeys.clear();
        this.imageKeys.addAll(keys);
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getPosition() { return position; }
    public List<String> getImageKeys() { return imageKeys; }
}
