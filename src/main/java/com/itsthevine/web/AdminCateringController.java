package com.itsthevine.web;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The catering price tables, editable as forms.
 *
 * <p>A table is edited and saved whole, which is the same rule the React screen followed and for the
 * same reason: a column heading, its price and the entries beneath it only mean anything together, so
 * they have to be added, moved and removed together. {@code CateringPackage#arrange} refuses an
 * arrangement whose lines and columns disagree.
 *
 * <p>The interesting part is doing that without JavaScript. One form holds the whole table, and its
 * buttons all submit it — {@code name="do"} says which one was pressed. "Add a column" therefore arrives
 * with every cell the editor has typed so far, adds the column to what arrived, and re-renders; nothing
 * typed is lost, and nothing is written until Save. The alternative — a link that adds a column
 * server-side — would have to either discard the unsaved edits or write a half-built table to the live
 * page.
 */
@Controller
@RequestMapping("/admin/catering")
@ConditionalOnProperty(prefix = "platform.security", name = "mode", havingValue = "OIDC")
public class AdminCateringController {

    private final CateringMenu catering;

    public AdminCateringController(CateringMenu catering) {
        this.catering = catering;
    }

    /**
     * One table, as the form posts it back.
     *
     * <p>A form-backing object rather than a pile of {@code @RequestParam} lists, because the cells are a
     * grid: Spring binds {@code lines[2].values[1]} into exactly the right place, whereas flat repeated
     * parameters would rely on the browser's submission order to keep the grid square.
     */
    public static class TableForm {

        private String name = "";
        private String blurb = "";
        private List<ColumnForm> columns = new ArrayList<>();
        private List<LineForm> lines = new ArrayList<>();
        private List<String> notes = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBlurb() { return blurb; }
        public void setBlurb(String blurb) { this.blurb = blurb; }

        public List<ColumnForm> getColumns() { return columns; }
        public void setColumns(List<ColumnForm> columns) { this.columns = columns; }

        public List<LineForm> getLines() { return lines; }
        public void setLines(List<LineForm> lines) { this.lines = lines; }

        public List<String> getNotes() { return notes; }
        public void setNotes(List<String> notes) { this.notes = notes; }
    }

    public static class ColumnForm {

        /** Null for a column the editor has just added and not yet saved. */
        private Long id;
        private String label = "";
        private String price = "";
        private String serves = "";

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }

        public String getServes() { return serves; }
        public void setServes(String serves) { this.serves = serves; }
    }

    public static class LineForm {

        private Long id;
        private String label = "";
        private List<String> values = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public List<String> getValues() { return values; }
        public void setValues(List<String> values) { this.values = values; }
    }

    /** The tables, listed: rename or fill one in on its own page, and set the page's own notes here. */
    @GetMapping
    public String tables(Model model) {
        model.addAttribute("menu", catering.everything());
        return "admin/catering";
    }

    /**
     * One table's editor.
     *
     * <p>A page per table rather than every table on one screen: a table is saved whole, so the thing
     * being edited and the thing being saved should be the same thing you can see.
     */
    @GetMapping("/tables/{id}")
    public String edit(@PathVariable Long id, Model model) {
        CateringMenu.PackageView table = catering.everything().packages().stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElse(null);
        if (table == null) {
            return "redirect:/admin/catering";
        }
        model.addAttribute("table", formOf(table));
        model.addAttribute("tableId", id);
        return "admin/table";
    }

    @PostMapping("/tables")
    public String add(@RequestParam String name, RedirectAttributes flash) {
        try {
            catering.add(name);
            flash.addFlashAttribute("done", "Added the " + name.trim() + " table. Give it a column and a line.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("problem", e.getMessage());
        }
        return "redirect:/admin/catering";
    }

    /**
     * Save, or restructure and come back.
     *
     * @param action which button was pressed: {@code save}, {@code add-column}, {@code add-line}, or one
     *               of {@code remove-column}/{@code move-column}/{@code remove-line}/{@code move-line}
     *               with the position after a colon ({@code move-column:2:-1}). A button can only send
     *               its own name and value, so the value carries the argument.
     */
    @PostMapping("/tables/{id}")
    public String save(@PathVariable Long id,
                       @ModelAttribute("table") TableForm form,
                       @RequestParam(name = "do", defaultValue = "save") String action,
                       Model model,
                       RedirectAttributes flash) {
        if (!action.equals("save")) {
            restructure(form, action);
            // Deliberately NOT a redirect: this is a draft, not a saved state. Re-rendering the form the
            // editor is looking at keeps every cell they have typed; writing it now would put a column
            // headed "" on the live page, and the table refuses that anyway.
            model.addAttribute("tableId", id);
            model.addAttribute("unsaved", true);
            return "admin/table";
        }

        try {
            catering.save(id, new CateringMenu.PackageEdit(
                    form.getName(),
                    form.getBlurb(),
                    form.getColumns().stream()
                            .map(c -> new CateringMenu.TierEdit(c.getId(), c.getLabel(), c.getPrice(), c.getServes()))
                            .toList(),
                    form.getLines().stream()
                            .map(l -> new CateringMenu.RowEdit(l.getId(), l.getLabel(), l.getValues()))
                            .toList(),
                    form.getNotes()));
            flash.addFlashAttribute("done", "Saved the " + form.getName().trim() + " table.");
            return "redirect:/admin/catering";
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Back to the form with what they typed, and the reason. A redirect here would throw away the
            // work and leave them guessing which cell the message was about.
            model.addAttribute("tableId", id);
            model.addAttribute("unsaved", true);
            model.addAttribute("problem", e.getMessage());
            return "admin/table";
        }
    }

    @PostMapping("/tables/{id}/move")
    public String move(@PathVariable Long id, @RequestParam int by) {
        List<Long> ids = new ArrayList<>(catering.everything().packages().stream()
                .map(CateringMenu.PackageView::id).toList());
        int at = ids.indexOf(id);
        int to = at + by;
        if (at >= 0 && to >= 0 && to < ids.size()) {
            swap(ids, at, to);
            catering.reorder(ids);
        }
        return "redirect:/admin/catering";
    }

    @PostMapping("/tables/{id}/delete")
    public String remove(@PathVariable Long id, RedirectAttributes flash) {
        try {
            catering.remove(id);
            flash.addFlashAttribute("done", "Table deleted.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("problem", e.getMessage());
        }
        return "redirect:/admin/catering";
    }

    @PostMapping("/notes")
    public String notes(@RequestParam(name = "notes", required = false) List<String> notes,
                        RedirectAttributes flash) {
        try {
            catering.replaceNotes(notes == null ? List.of() : notes);
            flash.addFlashAttribute("done", "Saved the notes for the page.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("problem", e.getMessage());
        }
        return "redirect:/admin/catering";
    }

    /**
     * Applies a structural button to the draft that arrived.
     *
     * <p>Adding a column adds an empty entry to every line, and removing one takes its entries with it,
     * which is the invariant the aggregate insists on. Doing it here rather than in the browser is the
     * whole point: there is one implementation of "a table has as many entries per line as it has
     * columns", and it is in Java.
     */
    private static void restructure(TableForm form, String action) {
        String[] parts = action.split(":");
        String what = parts[0];
        int at = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;
        int by = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        switch (what) {
            case "add-column" -> {
                form.getColumns().add(new ColumnForm());
                form.getLines().forEach(line -> line.getValues().add(""));
            }
            case "remove-column" -> {
                if (inRange(at, form.getColumns().size())) {
                    form.getColumns().remove(at);
                    form.getLines().forEach(line -> {
                        if (inRange(at, line.getValues().size())) {
                            line.getValues().remove(at);
                        }
                    });
                }
            }
            case "move-column" -> {
                int to = at + by;
                if (inRange(at, form.getColumns().size()) && inRange(to, form.getColumns().size())) {
                    swap(form.getColumns(), at, to);
                    form.getLines().forEach(line -> swap(line.getValues(), at, to));
                }
            }
            case "add-line" -> {
                LineForm line = new LineForm();
                form.getColumns().forEach(column -> line.getValues().add(""));
                form.getLines().add(line);
            }
            case "remove-line" -> {
                if (inRange(at, form.getLines().size())) {
                    form.getLines().remove(at);
                }
            }
            case "move-line" -> {
                int to = at + by;
                if (inRange(at, form.getLines().size()) && inRange(to, form.getLines().size())) {
                    swap(form.getLines(), at, to);
                }
            }
            case "add-note" -> form.getNotes().add("");
            case "remove-note" -> {
                if (inRange(at, form.getNotes().size())) {
                    form.getNotes().remove(at);
                }
            }
            // An unknown action is a stale page or a hand-edited form: leave the draft exactly as it is
            // rather than guessing at an edit nobody asked for.
            default -> { }
        }
    }

    /** The stored table, as a form to edit. */
    private static TableForm formOf(CateringMenu.PackageView table) {
        TableForm form = new TableForm();
        form.setName(table.name());
        form.setBlurb(table.blurb() == null ? "" : table.blurb());
        form.setColumns(table.tiers().stream().map(tier -> {
            ColumnForm column = new ColumnForm();
            column.setId(tier.id());
            column.setLabel(tier.label());
            // The price comes back written out ("$24") and goes out again as whatever is left in the box;
            // Money reads either.
            column.setPrice(tier.price() == null ? "" : tier.price());
            column.setServes(tier.serves() == null ? "" : tier.serves());
            return column;
        }).collect(Collectors.toCollection(ArrayList::new)));
        form.setLines(table.rows().stream().map(row -> {
            LineForm line = new LineForm();
            line.setId(row.id());
            line.setLabel(row.label());
            line.setValues(new ArrayList<>(row.values()));
            return line;
        }).collect(Collectors.toCollection(ArrayList::new)));
        form.setNotes(new ArrayList<>(table.notes()));
        return form;
    }

    private static boolean inRange(int at, int size) {
        return at >= 0 && at < size;
    }

    private static <T> void swap(List<T> items, int a, int b) {
        T held = items.get(a);
        items.set(a, items.get(b));
        items.set(b, held);
    }
}
