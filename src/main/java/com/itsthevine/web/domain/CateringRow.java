package com.itsthevine.web.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import net.thebennett.platform.data.BaseEntity;

/**
 * One line of a catering table: a baked good, and how much of it each column includes.
 *
 * <p>The values are positional — index 0 belongs to the first column — and there is always exactly
 * one per column, blanks included. {@link CateringPackage} is the only thing that can set them,
 * because it is the only thing that knows how many columns there are.
 */
@Entity
@Table(name = "catering_row")
public class CateringRow extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private CateringPackage cateringPackage;

    @Column(nullable = false, length = 200)
    private String label;

    /**
     * One entry per column, in column order. An empty string is a cell the bakery hasn't filled in —
     * the spreadsheet has several — so blanks are stored rather than dropped, which is what keeps the
     * list aligned with the columns.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "catering_row_value", joinColumns = @JoinColumn(name = "row_id"))
    @OrderColumn(name = "position")
    @Column(name = "value", nullable = false, length = 300)
    private List<String> values = new ArrayList<>();

    @Column(name = "position", nullable = false)
    private int position;

    protected CateringRow() {
        // for JPA
    }

    CateringRow(CateringPackage cateringPackage) {
        this.cateringPackage = cateringPackage;
    }

    void describe(String label) {
        this.label = Text.required(label, 200, "Every line needs a name — what is it the customer gets?");
    }

    /** Replaced wholesale; the package has already checked there is one value per column. */
    void replaceValues(List<String> replacements) {
        List<String> cleaned = replacements.stream().map(value -> Text.optional(value, 300)).toList();
        this.values.clear();
        this.values.addAll(cleaned);
    }

    void moveTo(int position) {
        this.position = position;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getValues() {
        return List.copyOf(values);
    }

    public int getPosition() {
        return position;
    }
}
