package com.itsthevine.web;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The catalogue, editable by the person who bakes it — as pages and form posts.
 *
 * <p>Every write is POST, then a redirect back to the page it came from. That is not ceremony: it means
 * the browser's back button and reload do what they look like they do, a double-tap can't repeat an
 * upload, and there is no client-side state to lose. The message the editor reads afterwards travels as
 * a flash attribute.
 *
 * <p>The whole controller is conditional on OIDC being switched on, the same as the JSON admin it
 * replaced. That is deliberate belt-and-braces: the platform's permit-all filter chain is what runs when
 * {@code platform.security.mode} is unset, so if these pages existed unconditionally a deployment that
 * forgot to configure Authentik would be publishing catalogue writes to the open internet. Gated this
 * way, "no auth configured" means "no admin" — the paths 404 like any other unknown URL.
 */
@Controller
@RequestMapping("/admin")
@ConditionalOnProperty(prefix = "platform.security", name = "mode", havingValue = "OIDC")
public class AdminController {

    private final Catalogue catalogue;

    public AdminController(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping
    public String catalogue(Model model) {
        model.addAttribute("items", catalogue.items());
        model.addAttribute("filters", catalogue.filters());
        return "admin/catalogue";
    }

    // --- items ---------------------------------------------------------------

    @PostMapping("/items")
    public String add(@RequestParam String name,
                      @RequestParam String category,
                      @RequestParam(name = "photos", required = false) List<MultipartFile> photos,
                      RedirectAttributes flash) {
        return run(flash, () -> {
            catalogue.addItem(name, category, photos);
            flash.addFlashAttribute("done", "Added " + name.trim() + " to the top of the page.");
        });
    }

    /**
     * Everything you can do to one item, from one form.
     *
     * <p>One form per item rather than one per button: the page carries forty items, and a separate form
     * for each control meant 467 forms and 464 CSRF tokens — 317 KB of HTML for a screen that is opened
     * on a phone, in a bakery. {@code name="do"} says which button was pressed and its value carries the
     * argument, exactly as the catering table editor does.
     *
     * <p>Only {@code save} looks at the name and category boxes. The other actions deliberately ignore
     * them, so pressing "move down" halfway through retyping a name doesn't save the half-typed name.
     *
     * @param action {@code save}, {@code move:-1}, {@code move:1}, {@code delete}, or
     *               {@code photo:<key>:<-1|1|0>} — earlier, later, or remove
     */
    @PostMapping("/items/{id}")
    public String item(@PathVariable Long id,
                       @RequestParam(name = "do", defaultValue = "save") String action,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String category,
                       RedirectAttributes flash) {
        String[] parts = action.split(":");
        return run(flash, () -> {
            switch (parts[0]) {
                case "save" -> {
                    catalogue.describeItem(id, name, category);
                    flash.addFlashAttribute("done", "Saved " + name.trim() + ".");
                }
                case "move" -> catalogue.moveItem(id, Integer.parseInt(parts[1]));
                case "delete" -> {
                    catalogue.removeItem(id);
                    flash.addFlashAttribute("done", "Removed from the products page.");
                }
                case "photo" -> {
                    // The key is the middle field; it can contain slashes and dots but never a colon.
                    String key = parts[1];
                    int move = Integer.parseInt(parts[2]);
                    if (move == 0) {
                        catalogue.removePhoto(id, key);
                    } else {
                        catalogue.movePhoto(id, key, move);
                    }
                }
                // A stale page, or a hand-edited form. Do nothing rather than guess.
                default -> { }
            }
        });
    }

    @PostMapping("/items/{id}/photos")
    public String addPhotos(@PathVariable Long id,
                            @RequestParam(name = "photos", required = false) List<MultipartFile> photos,
                            RedirectAttributes flash) {
        return run(flash, () -> {
            catalogue.addPhotos(id, photos);
            flash.addFlashAttribute("done", "Photos added.");
        });
    }

    // --- filters -------------------------------------------------------------

    @PostMapping("/categories")
    public String addFilter(@RequestParam String name, RedirectAttributes flash) {
        return run(flash, () -> {
            catalogue.addFilter(name);
            flash.addFlashAttribute("done", "Added the " + name.trim() + " category.");
        });
    }

    /**
     * Everything you can do to one filter, from one form — same shape as an item.
     *
     * @param action {@code save}, {@code move:-1}, {@code move:1} or {@code delete}
     */
    @PostMapping("/categories/{id}")
    public String filter(@PathVariable Long id,
                         @RequestParam(name = "do", defaultValue = "save") String action,
                         @RequestParam(required = false) String name,
                         RedirectAttributes flash) {
        String[] parts = action.split(":");
        return run(flash, () -> {
            switch (parts[0]) {
                case "save" -> {
                    catalogue.renameFilter(id, name);
                    flash.addFlashAttribute("done",
                            "Renamed to " + name.trim() + ", and everything filed under it moved too.");
                }
                case "move" -> catalogue.moveFilter(id, Integer.parseInt(parts[1]));
                case "delete" -> {
                    catalogue.removeFilter(id);
                    flash.addFlashAttribute("done", "Category deleted.");
                }
                default -> { }
            }
        });
    }

    /**
     * Runs one edit and comes back to the page.
     *
     * <p>The two exception types are the vocabulary the domain already speaks — {@code
     * IllegalArgumentException} for "that isn't a usable value", {@code IllegalStateException} for "not
     * while things are like this" — and both carry a sentence written for the editor to read. The
     * platform's exception handler turns them into JSON for the API; here they belong on the page.
     */
    private String run(RedirectAttributes flash, Runnable edit) {
        try {
            edit.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("problem", e.getMessage());
        }
        return "redirect:/admin";
    }

    /**
     * A photo bigger than the configured limit, said in a sentence instead of a stack trace.
     *
     * <p>This is the same {@code problem} flash the refusals above use, so the editor reads it in the
     * same place on the same page. Before it existed, an over-sized file escaped as the container's own
     * parsing error: a 500, and then a second failure forwarding to {@code /error}, because that forward
     * re-parsed the same too-large request. Reaching this handler at all depends on {@code
     * spring.servlet.multipart.resolve-lazily} — parsed eagerly, the throw happens before any handler
     * method is chosen and there is nothing here to catch it.
     *
     * <p>The flash map is written directly rather than through {@code RedirectAttributes}, which is not
     * an argument Spring supplies to an {@code @ExceptionHandler}.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String photoTooBig(HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("problem",
                "That photo is too large. Anything up to 15 MB is fine — a photo straight off a phone "
                        + "normally is — and we resize it here, so there is no need to shrink it first.");
        return "redirect:/admin";
    }
}
