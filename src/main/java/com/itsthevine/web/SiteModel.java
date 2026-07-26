package com.itsthevine.web;

import java.time.ZoneId;
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

    /** The shop is in Princeville, Illinois; the container runs on UTC, which turns over first. */
    private static final ZoneId SHOP_TIME = ZoneId.of("America/Chicago");

    private final SitePhotos photos;
    private final String baseUrl;
    private final String build;

    public SiteModel(SitePhotos photos,
                     @Value("${site.base-url:https://itsthevine.com}") String baseUrl,
                     @Value("${site.build:dev}") String build) {
        this.photos = photos;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.build = build;
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
        return ZonedDateTime.now(SHOP_TIME).getYear();
    }
}
