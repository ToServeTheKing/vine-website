package com.itsthevine.web;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

/**
 * The shop, described for machines: a schema.org Bakery in the page head.
 *
 * <p>This is how a search engine learns that itsthevine.com is a bakery in Princeville with a phone number
 * and Saturday hours — the difference between a blue link and a listing that shows "Open ⋅ closes 2pm".
 * For a shop whose customers find it by searching its town, that is worth more than anything else on this
 * page.
 *
 * <p>Built from {@link Shop} and rendered once in the head, so the hours a crawler reads cannot drift from
 * the ones a visitor reads. Serialised with Jackson rather than string-built: every value here is a
 * constant of ours today, but the one day somebody interpolates a name into it, hand-built JSON in a
 * {@code <script>} is an injection.
 */
@Service
public class StructuredData {

    private final ObjectMapper json;
    private final SitePhotos photos;
    private final String baseUrl;

    public StructuredData(ObjectMapper json, SitePhotos photos,
                          @Value("${site.base-url:https://itsthevine.com}") String baseUrl) {
        this.json = json;
        this.photos = photos;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public String bakery() {
        Map<String, Object> shop = new LinkedHashMap<>();
        shop.put("@context", "https://schema.org");
        shop.put("@type", "Bakery");
        shop.put("name", Shop.NAME);
        shop.put("url", baseUrl);
        shop.put("image", photos.of("gallery/Outside.webp"));
        shop.put("telephone", Shop.PHONE);
        shop.put("email", Shop.EMAIL);
        // "$" rather than a number: schema.org wants a band, and ours is one coffee to a wedding cake.
        shop.put("priceRange", "$");
        shop.put("servesCuisine", List.of("Bakery", "Coffee", "Sandwiches"));
        shop.put("address", Map.of(
                "@type", "PostalAddress",
                "streetAddress", Shop.STREET,
                "addressLocality", Shop.CITY,
                "addressRegion", Shop.STATE,
                "postalCode", Shop.POSTCODE,
                "addressCountry", "US"));
        shop.put("openingHoursSpecification", openingHours());

        // Jackson 3 (tools.jackson, what Boot 4 ships) throws unchecked, and there is nothing to catch
        // anyway: the input is a map of our own constants.
        return json.writeValueAsString(shop);
    }

    /**
     * Days that keep the same times are given as one specification with several days on it, which is both
     * shorter and what the vocabulary is for.
     */
    private List<Map<String, Object>> openingHours() {
        Map<Shop.Opening, List<String>> byHours = new TreeMap<>(
                (a, b) -> a.opens().equals(b.opens()) ? a.closes().compareTo(b.closes()) : a.opens().compareTo(b.opens()));
        for (DayOfWeek day : Shop.READING_ORDER) {
            Shop.Opening opening = Shop.WEEK.get(day);
            if (opening != null) {
                byHours.computeIfAbsent(opening, o -> new java.util.ArrayList<>())
                        .add(day.getDisplayName(TextStyle.FULL, Locale.US));
            }
        }

        return byHours.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "@type", "OpeningHoursSpecification",
                        "dayOfWeek", entry.getValue(),
                        "opens", entry.getKey().opens().toString(),
                        "closes", entry.getKey().closes().toString()))
                .toList();
    }
}
