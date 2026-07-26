package com.itsthevine.web;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PostMapping("/items/{id}")
    public String describe(@PathVariable Long id,
                           @RequestParam String name,
                           @RequestParam String category,
                           RedirectAttributes flash) {
        return run(flash, () -> {
            catalogue.describeItem(id, name, category);
            flash.addFlashAttribute("done", "Saved " + name.trim() + ".");
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

    /**
     * @param move {@code -1} or {@code 1} to shuffle the photo along, {@code 0} to remove it. One
     *             endpoint for the three buttons under a photo, because they are the same edit — which
     *             photos, in which order — and the server is what decides the resulting list.
     */
    @PostMapping("/items/{id}/photos/arrange")
    public String arrangePhoto(@PathVariable Long id,
                               @RequestParam String key,
                               @RequestParam int move,
                               RedirectAttributes flash) {
        return run(flash, () -> {
            if (move == 0) {
                catalogue.removePhoto(id, key);
            } else {
                catalogue.movePhoto(id, key, move);
            }
        });
    }

    @PostMapping("/items/{id}/move")
    public String move(@PathVariable Long id, @RequestParam int by, RedirectAttributes flash) {
        return run(flash, () -> catalogue.moveItem(id, by));
    }

    @PostMapping("/items/{id}/delete")
    public String remove(@PathVariable Long id, RedirectAttributes flash) {
        return run(flash, () -> {
            catalogue.removeItem(id);
            flash.addFlashAttribute("done", "Removed from the products page.");
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

    @PostMapping("/categories/{id}")
    public String renameFilter(@PathVariable Long id, @RequestParam String name, RedirectAttributes flash) {
        return run(flash, () -> {
            catalogue.renameFilter(id, name);
            flash.addFlashAttribute("done", "Renamed to " + name.trim() + ", and everything filed under it moved too.");
        });
    }

    @PostMapping("/categories/{id}/move")
    public String moveFilter(@PathVariable Long id, @RequestParam int by, RedirectAttributes flash) {
        return run(flash, () -> catalogue.moveFilter(id, by));
    }

    @PostMapping("/categories/{id}/delete")
    public String removeFilter(@PathVariable Long id, RedirectAttributes flash) {
        return run(flash, () -> {
            catalogue.removeFilter(id);
            flash.addFlashAttribute("done", "Category deleted.");
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
}
