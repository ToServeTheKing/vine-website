package com.itsthevine.web;

import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * The handful of things every page's chrome needs, added to the model once instead of by each handler.
 *
 * <p>Not scoped to {@link SiteController}: the error pages wear the same chrome, and Boot renders those
 * through its own controller. The JSON controllers get these attributes too and ignore them, which
 * costs nothing — a {@code @ResponseBody} method never looks at the model.
 */
@ControllerAdvice
public class SiteModel {

    private final SitePhotos photos;
    private final Hours hours;
    private final StructuredData structuredData;
    private final String baseUrl;
    private final String build;

    public SiteModel(SitePhotos photos, Hours hours, StructuredData structuredData,
                     @Value("${site.base-url:https://itsthevine.com}") String baseUrl,
                     @Value("${site.build:dev}") String build) {
        this.photos = photos;
        this.hours = hours;
        this.structuredData = structuredData;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.build = build;
    }

    /** The opening times as the page prints them — see Hours#week. */
    @ModelAttribute("hours")
    public java.util.List<Hours.Span> hours() {
        return hours.week();
    }

    /** Whether the shop is open at this moment, for the line in the header. */
    @ModelAttribute("openNow")
    public Hours.Status openNow() {
        return hours.now();
    }

    // The shop's own facts, so no template states an address or a phone number itself. Four attributes
    // rather than one object because a template reading `${shopStreet}` needs no explaining.

    @ModelAttribute("shopStreet")
    public String shopStreet() {
        return Shop.STREET;
    }

    @ModelAttribute("shopTown")
    public String shopTown() {
        return Shop.CITY + ", " + Shop.STATE + " " + Shop.POSTCODE;
    }

    @ModelAttribute("shopPhone")
    public String shopPhone() {
        return Shop.PHONE;
    }

    @ModelAttribute("shopPhoneSpoken")
    public String shopPhoneSpoken() {
        return Shop.PHONE_SPOKEN;
    }

    @ModelAttribute("shopEmail")
    public String shopEmail() {
        return Shop.EMAIL;
    }

    /** schema.org JSON-LD for the head. Written by Jackson; see StructuredData. */
    @ModelAttribute("structuredData")
    public String structuredData() {
        return structuredData.bakery();
    }

    /** What a shared link should show. */
    @ModelAttribute("shareImage")
    public String shareImage() {
        return photos.of("gallery/Outside.webp");
    }

    /** Lets a template ask for a photo by key: {@code ${photos.of('gallery/Outside.webp')}}. */
    @ModelAttribute("photos")
    public SitePhotos photos() {
        return photos;
    }

    /** Absolute URLs for og:url, which only means anything to a scraper if it's absolute. */
    @ModelAttribute("baseUrl")
    public String baseUrl() {
        return baseUrl;
    }

    @ModelAttribute("assetOrigin")
    public String assetOrigin() {
        return photos.origin();
    }

    /**
     * Hung on the stylesheet URL as {@code ?v=…}.
     *
     * <p>The SPA's bundles had content hashes in their filenames; one hand-written stylesheet does not,
     * so without this a visitor keeps whatever CSS they cached before the deploy — new markup, old
     * rules. The deployment passes the commit sha; a dev run says "dev".
     */
    @ModelAttribute("build")
    public String build() {
        return build;
    }

    /** The footer's copyright year. */
    @ModelAttribute("year")
    public int year() {
        return ZonedDateTime.now(Shop.ZONE).getYear();
    }
}
