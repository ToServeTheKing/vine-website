package com.itsthevine.web;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catering tables, editable from the site — prices move, and moving them shouldn't need a deploy.
 *
 * <p>Conditional on OIDC for the same reason as the product and category admins: with no identity
 * provider configured the platform runs its permit-all chain, so an unconditional controller here
 * would publish price writes to the open internet on any deployment that forgot to wire Authentik.
 * Gated this way, "no auth configured" means these endpoints simply don't exist.
 *
 * <p>A table is saved whole rather than field by field. That isn't a shortcut — the columns and the
 * values under them only mean anything together, so they have to arrive together (see
 * {@code CateringPackage#arrange}).
 */
@RestController
@RequestMapping("/api/admin/catering")
@ConditionalOnProperty(prefix = "platform.security", name = "mode", havingValue = "OIDC")
public class AdminCateringController {

    private final CateringMenu menu;

    public AdminCateringController(CateringMenu menu) {
        this.menu = menu;
    }

    public record Name(String name) {}

    public record Order(List<Long> ids) {}

    @GetMapping
    public CateringMenu.MenuView all() {
        return menu.everything();
    }

    @PostMapping("/packages")
    public CateringMenu.PackageView add(@RequestBody Name body) {
        return menu.add(body.name());
    }

    @PutMapping("/packages/{id}")
    public CateringMenu.PackageView save(@PathVariable Long id,
                                         @RequestBody CateringMenu.PackageEdit edit) {
        return menu.save(id, edit);
    }

    /** Mapped above {@code /packages/{id}} by Spring's literal-beats-template rule, as with products. */
    @PutMapping("/packages/order")
    public List<CateringMenu.PackageView> reorder(@RequestBody Order order) {
        return menu.reorder(order.ids());
    }

    @DeleteMapping("/packages/{id}")
    public ResponseEntity<Map<String, Object>> remove(@PathVariable Long id) {
        menu.remove(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** The page's own footnotes: the full list the editor is looking at. */
    @PutMapping("/notes")
    public List<String> notes(@RequestBody List<String> bodies) {
        return menu.replaceNotes(bodies);
    }
}
