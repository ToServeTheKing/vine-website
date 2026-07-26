package com.itsthevine.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import net.thebennett.platform.data.BaseEntity;

/**
 * One column of a catering table: a size, and what it costs.
 *
 * <p>Only {@link CateringPackage} can change one. A tier means nothing apart from the table it sits
 * in — its price is read against the row values beside it — so the package is the only thing allowed
 * to rearrange them, which is what keeps a row's values and its columns the same length.
 */
@Entity
@Table(name = "catering_tier")
public class CateringTier extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private CateringPackage cateringPackage;

    @Column(nullable = false, length = 120)
    private String label;

    /** Whole cents, and nullable — see {@link Money}. */
    @Column(name = "price_cents")
    private Integer priceCents;

    /**
     * Roughly how many people this column feeds — "About 6–8 people".
     *
     * <p>Free text, and optional. A column named "Small" says nothing about how many it feeds, which is
     * the question anyone ordering for an office actually has; a column named "125 people" has already
     * answered it and leaves this blank rather than saying it twice.
     */
    @Column(length = 80)
    private String serves;

    @Column(name = "position", nullable = false)
    private int position;

    protected CateringTier() {
        // for JPA
    }

    CateringTier(CateringPackage cateringPackage) {
        this.cateringPackage = cateringPackage;
    }

    /**
     * @param price  as the editor typed it; empty for a column that doesn't state one
     * @param serves roughly how many people, or empty
     */
    void describe(String label, String price, String serves) {
        this.label = Text.required(label, 120,
                "Every column needs a heading — a size like \"Large\", or who it feeds like \"15–20 people\".");
        this.priceCents = Money.cents(price);
        String trimmed = Text.optional(serves, 80);
        this.serves = trimmed.isEmpty() ? null : trimmed;
    }

    void moveTo(int position) {
        this.position = position;
    }

    public String getLabel() {
        return label;
    }

    public Integer getPriceCents() {
        return priceCents;
    }

    /** What the column's price reads as — "$24", or null when it doesn't state one. */
    public String getPrice() {
        return Money.format(priceCents);
    }

    public String getServes() {
        return serves;
    }

    public int getPosition() {
        return position;
    }
}
