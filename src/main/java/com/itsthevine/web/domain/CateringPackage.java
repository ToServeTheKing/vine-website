package com.itsthevine.web.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import net.thebennett.platform.data.BaseEntity;

/**
 * One price table on the catering page — "Office", "Parties", "Weddings".
 *
 * <p>The bakery keeps these as a spreadsheet, so a spreadsheet is what this models: {@link
 * CateringTier}s are the columns (a size, and what it costs) and {@link CateringRow}s are the lines
 * (a baked good, and how much of it each column includes). A line holds one value per column, in
 * column order.
 *
 * <p>That alignment is the whole reason this is an aggregate rather than three tables edited
 * separately. A column and the values under it only mean anything together — drop the middle column
 * on its own and every remaining value shifts one place left, so the Large box silently starts
 * advertising the Medium box's contents at the Large price. Only this class can rearrange a table,
 * and it will not accept an arrangement whose lines and columns disagree.
 */
@Entity
@Table(name = "catering_package")
public class CateringPackage extends BaseEntity {

    /** Wider than this doesn't fit a phone, and these tables are read on phones. */
    private static final int MOST_COLUMNS = 8;
    private static final int MOST_LINES = 40;
    private static final int MOST_NOTES = 12;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 400)
    private String blurb;

    @Column(name = "position", nullable = false)
    private int position;

    /** The rules under this table: minimums, what can't be mixed, how delivery is charged. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "catering_package_note", joinColumns = @JoinColumn(name = "package_id"))
    @OrderColumn(name = "position")
    @Column(name = "body", nullable = false, length = 600)
    private List<String> notes = new ArrayList<>();

    @OneToMany(mappedBy = "cateringPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<CateringTier> tiers = new ArrayList<>();

    @OneToMany(mappedBy = "cateringPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<CateringRow> rows = new ArrayList<>();

    protected CateringPackage() {
        // for JPA
    }

    public CateringPackage(String name, int position) {
        describe(name, null);
        this.position = position;
    }

    /**
     * A column as the editor left it — its heading and its price. A null {@code id} is one they just
     * added. (Named for the heading rather than the column so as not to shadow {@code @Column}.)
     */
    public record Heading(Long id, String label, String price, String serves) {}

    /** A line as the editor left it, with one value per column — blanks included. */
    public record Line(Long id, String label, List<String> values) {}

    public final void describe(String name, String blurb) {
        this.name = Text.required(name, 120, "Please give the table a name, like \"Weddings\".");
        String trimmed = Text.optional(blurb, 400);
        this.blurb = trimmed.isEmpty() ? null : trimmed;
    }

    public void moveTo(int position) {
        this.position = position;
    }

    public void replaceNotes(List<String> replacements) {
        if (replacements.size() > MOST_NOTES) {
            throw new IllegalArgumentException(
                    "That's a lot of small print — " + MOST_NOTES + " notes per table at most.");
        }
        List<String> cleaned = replacements.stream()
                .map(note -> Text.required(note, 600, "One of the notes is empty — delete it rather than blanking it."))
                .toList();
        this.notes.clear();
        this.notes.addAll(cleaned);
    }

    /**
     * Make the table exactly this: these columns in this order, and these lines, each carrying one
     * value per column.
     *
     * <p>The whole table arrives at once because that is the only way the editor can move a column and
     * take its values with it. Anything they left out is deleted, anything carrying an id keeps its
     * identity, and positions are renumbered from the order they arrived in rather than trusted from
     * the request — so what's stored is what they were looking at when they hit save.
     */
    public void arrange(List<Heading> columns, List<Line> lines) {
        if (columns.size() > MOST_COLUMNS) {
            throw new IllegalArgumentException(
                    "A table can have at most " + MOST_COLUMNS + " columns and still be readable on a phone.");
        }
        if (lines.size() > MOST_LINES) {
            throw new IllegalArgumentException("A table can have at most " + MOST_LINES + " lines.");
        }
        for (Line line : lines) {
            if (line.values().size() != columns.size()) {
                // Not a message an editor should ever see: the screen sends whole tables. Worth saying
                // out loud anyway, because the alternative is a table that quietly means something else.
                throw new IllegalArgumentException("\"" + line.label() + "\" has " + line.values().size()
                        + " entries but the table has " + columns.size()
                        + " columns. Reload the page and try that again.");
            }
        }

        List<CateringTier> arrangedTiers = new ArrayList<>();
        int columnNumber = 1;
        for (Heading column : columns) {
            CateringTier tier = column.id() == null ? new CateringTier(this) : tier(column.id());
            tier.describe(column.label(), column.price(), column.serves());
            tier.moveTo(columnNumber++);
            arrangedTiers.add(tier);
        }

        List<CateringRow> arrangedRows = new ArrayList<>();
        int lineNumber = 1;
        for (Line line : lines) {
            CateringRow row = line.id() == null ? new CateringRow(this) : row(line.id());
            row.describe(line.label());
            row.replaceValues(line.values());
            row.moveTo(lineNumber++);
            arrangedRows.add(row);
        }

        // Both collections are rewritten only after every column and line has been accepted, so a
        // rejected edit leaves the table exactly as it was.
        settle(tiers, arrangedTiers, Comparator.comparingInt(CateringTier::getPosition));
        settle(rows, arrangedRows, Comparator.comparingInt(CateringRow::getPosition));
    }

    /**
     * Keep what was arranged, drop what wasn't. Removal from the collection is what deletes the row —
     * these are {@code orphanRemoval} associations — so the omitted ones need no further handling.
     */
    private static <T> void settle(List<T> stored, List<T> arranged, Comparator<T> byPosition) {
        stored.removeIf(item -> !holds(arranged, item));
        arranged.stream().filter(item -> !holds(stored, item)).forEach(stored::add);
        stored.sort(byPosition);
    }

    /**
     * Identity, deliberately: entities here inherit no {@code equals}, and two freshly built columns
     * with the same heading are two different columns.
     */
    private static boolean holds(List<?> items, Object item) {
        return items.stream().anyMatch(candidate -> candidate == item);
    }

    private CateringTier tier(Long id) {
        return tiers.stream().filter(t -> id.equals(t.getId())).findFirst().orElseThrow(this::changedUnderneath);
    }

    private CateringRow row(Long id) {
        return rows.stream().filter(r -> id.equals(r.getId())).findFirst().orElseThrow(this::changedUnderneath);
    }

    private IllegalArgumentException changedUnderneath() {
        return new IllegalArgumentException(
                "Part of the " + name + " table isn't there any more. Reload the page to see it as it is now.");
    }

    public String getName() {
        return name;
    }

    public String getBlurb() {
        return blurb;
    }

    public int getPosition() {
        return position;
    }

    public List<String> getNotes() {
        return List.copyOf(notes);
    }

    public List<CateringTier> getTiers() {
        return List.copyOf(tiers);
    }

    public List<CateringRow> getRows() {
        return List.copyOf(rows);
    }
}
