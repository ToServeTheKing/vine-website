package com.itsthevine.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.thebennett.platform.contact.ContactException;

/**
 * The site: every page a customer sees, rendered on the server with Thymeleaf.
 *
 * <p>This replaced a React SPA and, with it, {@code PageMetaController} — a class whose whole job was
 * to splice per-page {@code <title>} and OG tags into the SPA's one shell with regular expressions,
 * because crawlers and link-preview scrapers got nothing useful otherwise. A server-rendered page has
 * a head of its own, so that machinery is gone rather than ported.
 *
 * <p>Every page states its own title and description here, in Java, next to the route that serves it.
 * The templates only lay them out.
 */
@Controller
public class SiteController {

    private static final String NAME = "The Vine Coffeehouse + Bakery";

    private final ProductCatalog catalog;
    private final CateringMenu catering;
    private final Enquiries enquiries;
    private final SitePhotos photos;

    public SiteController(ProductCatalog catalog, CateringMenu catering, Enquiries enquiries, SitePhotos photos) {
        this.catalog = catalog;
        this.catering = catering;
        this.enquiries = enquiries;
        this.photos = photos;
    }

    @GetMapping("/")
    public String home(Model model) {
        meta(model, "/", NAME,
                "A locally owned coffeehouse and bakery in downtown Princeville, Illinois. We bake "
                + "pastries, custom cakes, cookies, and cinnamon rolls, and serve sandwiches, paninis, "
                + "and coffee.");
        // The hero photo is the largest thing on the page and the first thing you see; telling the
        // browser about it in the head starts it a round trip sooner.
        model.addAttribute("preload", photos.of("gallery/Outside.webp"));
        return "home";
    }

    /**
     * @param category a stored category, or absent/"All" for the whole catalogue. A query parameter
     *                 rather than a click handler: the filtered page is now a real URL you can send
     *                 someone, and the filter is applied by the same code that answers /api/products.
     */
    @GetMapping("/products")
    public String products(@RequestParam(required = false) String category, Model model) {
        String selected = category == null || category.isBlank() ? ProductCatalog.ALL : category;
        meta(model, "/products", "Our products · " + NAME,
                "Cinnamon rolls, caramel rolls, scones, cookie bars, macarons, brownies, pies, and "
                + "made-to-order cakes and decorated cookies from The Vine in Princeville, Illinois.");
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("selected", selected);
        model.addAttribute("products", catalog.list(selected));
        return "products";
    }

    @GetMapping("/catering")
    public String catering(Model model) {
        meta(model, "/catering", "Goodie boxes & catering · " + NAME,
                "Goodie boxes for the office, party packages, and wedding cakes and desserts from The "
                + "Vine in Princeville, Illinois — what each size includes, and what it costs.");
        model.addAttribute("menu", catering.menu());
        return "catering";
    }

    @GetMapping("/history")
    public String history(Model model) {
        meta(model, "/history", "Our story · " + NAME,
                "Morissa Bennett opened The Vine in 2024 at 215 E Main Street in downtown Princeville, "
                + "Illinois. We bake in our own kitchen on Main Street.");
        return "history";
    }

    /**
     * @param about which catering table they came from, if they arrived by one of that page's buttons.
     *              The message box starts with the question already half-asked — the alternative is a
     *              blank box and an enquiry that says "how much?" with no way to tell what about.
     */
    @GetMapping("/contact")
    public String contact(@RequestParam(required = false) String about, Model model) {
        contactMeta(model);
        // Matched against the real table names rather than echoed: this text ends up in a box on the
        // page, and a query parameter is whatever a link says it is. Thymeleaf would escape it, but a
        // link that puts words of someone else's choosing in front of a customer is still not a link we
        // want to work.
        catering.menu().packages().stream()
                .map(CateringMenu.PackageView::name)
                .filter(name -> name.equalsIgnoreCase(about))
                .findFirst()
                .ifPresent(name -> model.addAttribute("message",
                        "I'd like to ask about " + name.toLowerCase() + " catering — "));
        return "contact";
    }

    /**
     * The form posts here and gets a page back — no JavaScript involved in sending an enquiry.
     *
     * <p>It renders rather than redirects on both outcomes, deliberately. A failed send has to come
     * back with what the visitor typed still in the boxes: they wrote it once, and the failure is ours
     * (a refused relay), not theirs. On success the fields are cleared and the message replaces them.
     */
    @PostMapping("/contact")
    public String submit(@RequestParam String name,
                         @RequestParam String email,
                         @RequestParam String message,
                         Model model) {
        contactMeta(model);
        try {
            enquiries.receive(name, email, message);
            model.addAttribute("sent", true);
        } catch (ContactException e) {
            // The service wrote this sentence for the visitor to read; don't replace it with a status
            // code or a stack trace.
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("message", message);
        }
        return "contact";
    }

    /**
     * The admin is still a React screen, and this is the one route that serves it.
     *
     * <p>The platform's SPA fallback used to do this for every extension-less path, which is why it's
     * switched off in application.yaml: with the site server-rendered, forwarding an unknown URL to a
     * JavaScript shell would answer a typo with a blank page and a 200 instead of the site's own 404.
     */
    @GetMapping("/admin")
    public String admin() {
        return "forward:/index.html";
    }

    private void contactMeta(Model model) {
        meta(model, "/contact", "Contact us · " + NAME,
                "Get in touch with The Vine Coffeehouse + Bakery, 215 E Main Street, Princeville, "
                + "Illinois. Call (309) 701-0660 or send us a message.");
    }

    /** @param path the route, so the head can build an absolute og:url for the scrapers */
    private static void meta(Model model, String path, String title, String description) {
        model.addAttribute("path", path);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
    }
}
