package com.itsthevine.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import net.thebennett.platform.data.BaseEntity;

/**
 * A filter button on the products page, and the order it sits in.
 *
 * Products still record their category by name, so this table is reference data rather than the
 * owner of the relationship — it exists to say which categories the bakery offers and in what order
 * to show them, both of which used to be a constant in the code.
 */
@Entity
@Table(name = "category")
public class Category extends BaseEntity {

    @Column(nullable = false, length = 60, unique = true)
    private String name;

    /** Display order of the filter buttons, after "All". */
    @Column(name = "position", nullable = false)
    private int position;

    protected Category() {
        // for JPA
    }

    public Category(String name, int position) {
        this.name = name;
        this.position = position;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void moveTo(int position) {
        this.position = position;
    }

    public String getName() { return name; }
    public int getPosition() { return position; }
}
