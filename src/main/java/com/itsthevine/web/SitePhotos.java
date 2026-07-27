package com.itsthevine.web;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Where a photo lives: {@code of("gallery/Outside.webp")} → the absolute bucket URL.
 *
 * <p>Photos are in a public MinIO bucket rather than the image — 50 MB of JPEGs has no business inside
 * a container we redeploy on every commit — so the bucket's address is deployment configuration and the
 * absolute URL is built here rather than stored on anything. Both the catalogue and the templates use
 * this, so an editor can never arrange a photo that resolves differently once it's live.
 */
@Service
public class SitePhotos {

    private final String baseUrl;

    public SitePhotos(@Value("${site.assets.base-url:https://s3.thebennett.net/itsthevine}") String baseUrl) {
        // A trailing slash here would produce '//images/...' — harmless on most servers, but it shows
        // up in every image URL on the page.
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    /**
     * Some photo filenames contain spaces ("Cinnamon Rolls.webp"), and a raw space in a URL doesn't
     * fetch — so each path segment is encoded. {@code URLEncoder} is form-encoding, which differs from
     * path-encoding in exactly one way that matters here: it turns a space into '+'.
     */
    public String of(String key) {
        String encoded = Arrays.stream(key.replaceAll("^/+", "").split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
        return baseUrl + "/images/" + encoded;
    }

    /**
     * Just the scheme and host, for the {@code preconnect} in the page head: opening that connection
     * during the head saves the hero image a round trip.
     */
    public String origin() {
        URI uri = URI.create(baseUrl);
        return uri.getScheme() + "://" + uri.getAuthority();
    }
}
