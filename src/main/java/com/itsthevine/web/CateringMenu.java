package com.itsthevine.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsthevine.web.domain.CateringNote;
import com.itsthevine.web.domain.CateringNoteRepository;
import com.itsthevine.web.domain.CateringPackage;
import com.itsthevine.web.domain.CateringPackageRepository;

/**
 * The catering page's brains: which tables to show, in what order, what's in each, and what the
 * prices read as.
 *
 * <p>Both the public page and the admin come through here, so there is one answer to "what does this
 * table say" — an editor can never arrange something that reads differently once it's live. Prices
 * arrive as whatever the editor typed and leave as text that's ready to print; {@code Money} is the
 * only thing that decides either, because how much something costs is the shop's business and not
 * the browser's.
 */
@Service
public class CateringMenu {

    private static final int MOST_NOTES = 12;

    private final CateringPackageRepository packages;
    private final CateringNoteRepository notes;

    public CateringMenu(CateringPackageRepository packages, CateringNoteRepository notes) {
        this.packages = packages;
        this.notes = notes;
    }

    /** A column. {@code price} is ready to print ("$24"), and null when the column doesn't state one. */
    public record TierView(Long id, String label, String price) {}

    /** A line, with one entry per column, in column order. */
    public record RowView(Long id, String label, List<String> values) {}

    public record PackageView(Long id, String name, String blurb,
                              List<TierView> tiers, List<RowView> rows, List<String> notes) {}

    /** The page: every table, plus the terms that apply to all of them. */
    public record MenuView(List<PackageView> packages, List<String> notes) {}

    // What the admin screen sends back. A whole table at a time — see CateringPackage#arrange.
    // `price` is the raw text from the box ("24", "$24.50", ""); Money decides what it means.
    public record TierEdit(Long id, String label, String price) {}

    public record RowEdit(Long id, String label, List<String> values) {}

    public record PackageEdit(String name, String blurb, List<TierEdit> tiers, List<RowEdit> rows,
                              List<String> notes) {}

    /**
     * What a customer sees.
     *
     * <p>A table with no columns or no lines is left out. Adding a table and filling it in are two
     * separate acts in the admin, and the gap between them shouldn't put a bare heading on the live
     * page — an empty price table tells a customer nothing except that we're disorganised.
     */
    @Transactional(readOnly = true)
    public MenuView menu() {
        List<PackageView> published = packages.findAllByOrderByPositionAsc().stream()
                .filter(p -> !p.getTiers().isEmpty() && !p.getRows().isEmpty())
                .map(CateringMenu::toView)
                .toList();
        return new MenuView(published, pageNotes());
    }

    /** What the editor sees: the same tables, including any they haven't finished. */
    @Transactional(readOnly = true)
    public MenuView everything() {
        List<PackageView> all = packages.findAllByOrderByPositionAsc().stream()
                .map(CateringMenu::toView)
                .toList();
        return new MenuView(all, pageNotes());
    }

    /** A new, empty table at the end of the page. Columns and lines come next, from the editor. */
    @Transactional
    public PackageView add(String name) {
        int last = packages.findAllByOrderByPositionAsc().stream()
                .mapToInt(CateringPackage::getPosition).max().orElse(0);
        return toView(packages.save(new CateringPackage(name, last + 1)));
    }

    /**
     * The whole table as the editor left it. Rejected in full or saved in full: the aggregate checks
     * every column and line before it touches anything, and the transaction covers the rest.
     */
    @Transactional
    public PackageView save(Long id, PackageEdit edit) {
        CateringPackage table = find(id);
        table.describe(edit.name(), edit.blurb());
        table.replaceNotes(clean(edit.notes()));
        table.arrange(
                orEmpty(edit.tiers()).stream()
                        .map(t -> new CateringPackage.Heading(t.id(), t.label(), t.price()))
                        .toList(),
                orEmpty(edit.rows()).stream()
                        .map(r -> new CateringPackage.Line(r.id(), r.label(), orEmpty(r.values())))
                        .toList());
        return toView(packages.save(table));
    }

    @Transactional
    public void remove(Long id) {
        packages.delete(find(id));
    }

    /** The ids in the order they should appear; anything omitted keeps its relative place after them. */
    @Transactional
    public List<PackageView> reorder(List<Long> ids) {
        List<CateringPackage> all = packages.findAllByOrderByPositionAsc();
        List<CateringPackage> arranged = new ArrayList<>();
        for (Long id : orEmpty(ids)) {
            all.stream().filter(p -> p.getId().equals(id)).findFirst().ifPresent(arranged::add);
        }
        all.stream().filter(p -> !arranged.contains(p)).forEach(arranged::add);

        int position = 1;
        for (CateringPackage table : arranged) {
            table.moveTo(position++);
        }
        packages.saveAll(arranged);
        return arranged.stream().map(CateringMenu::toView).toList();
    }

    /**
     * The page's own footnotes, replaced by the list the editor is looking at — so removing one is an
     * omission, exactly as it is everywhere else in this admin.
     *
     * <p>The notes that came back are reused in place rather than deleted and re-inserted, so editing
     * a typo doesn't quietly restamp when the terms were written.
     */
    @Transactional
    public List<String> replaceNotes(List<String> bodies) {
        List<String> wanted = clean(bodies);
        if (wanted.size() > MOST_NOTES) {
            throw new IllegalArgumentException(
                    "That's a lot of small print — " + MOST_NOTES + " notes at most.");
        }

        List<CateringNote> existing = notes.findAllByOrderByPositionAsc();
        List<CateringNote> keeping = new ArrayList<>();
        for (int i = 0; i < wanted.size(); i++) {
            CateringNote note = i < existing.size() ? existing.get(i) : new CateringNote(wanted.get(i), i + 1);
            note.say(wanted.get(i));
            note.moveTo(i + 1);
            keeping.add(note);
        }
        if (existing.size() > wanted.size()) {
            notes.deleteAll(existing.subList(wanted.size(), existing.size()));
        }
        notes.saveAll(keeping);
        return keeping.stream().map(CateringNote::getBody).toList();
    }

    private List<String> pageNotes() {
        return notes.findAllByOrderByPositionAsc().stream().map(CateringNote::getBody).toList();
    }

    private CateringPackage find(Long id) {
        return packages.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("That table no longer exists."));
    }

    private static PackageView toView(CateringPackage table) {
        List<TierView> tiers = table.getTiers().stream()
                .map(t -> new TierView(t.getId(), t.getLabel(), t.getPrice()))
                .toList();
        List<RowView> rows = table.getRows().stream()
                .map(r -> new RowView(r.getId(), r.getLabel(), r.getValues()))
                .toList();
        return new PackageView(table.getId(), table.getName(), table.getBlurb(), tiers, rows, table.getNotes());
    }

    /** Blank lines are how a textarea says "nothing here"; they are not notes. */
    private static List<String> clean(List<String> bodies) {
        return orEmpty(bodies).stream()
                .map(body -> body == null ? "" : body.trim())
                .filter(body -> !body.isEmpty())
                .toList();
    }

    /** A missing JSON array and an empty one mean the same thing to an editor. */
    private static <T> List<T> orEmpty(List<T> items) {
        return items == null ? List.of() : items;
    }
}
