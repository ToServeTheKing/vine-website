package com.itsthevine.web;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * The shop itself: where it is, how to reach it, and when it is open.
 *
 * <p>These facts were prose in three templates. They are now stated once, in Java, because they are read
 * by more than the eye: the footer and the contact page print them, the header works out from them
 * whether the shop is open right now, and {@code StructuredData} hands the same opening times to a search
 * engine — which is how a bakery ends up with its hours on the results page.
 *
 * <p>Hardcoded rather than a table with an admin screen, deliberately: opening times change about once a
 * year, and a table would need a screen, a migration and a way to say "closed Christmas Eve" to be worth
 * anything. When that day comes this is the one place to lift.
 */
public final class Shop {

    /** The shop is in Princeville, Illinois; the container runs on UTC, which turns over first. */
    public static final ZoneId ZONE = ZoneId.of("America/Chicago");

    public static final String NAME = "The Vine Coffeehouse + Bakery";
    public static final String STREET = "215 E Main Street";
    public static final String CITY = "Princeville";
    public static final String STATE = "IL";
    public static final String POSTCODE = "61559";
    /** For a tel: link. */
    public static final String PHONE = "+13097010660";
    /** For a human. */
    public static final String PHONE_SPOKEN = "(309) 701-0660";
    public static final String EMAIL = "contact@itsthevine.com";

    /**
     * When the doors are open. Days that aren't here are closed days, which is what makes Sunday and
     * Monday collapse into one line by themselves.
     */
    public static final Map<DayOfWeek, Opening> WEEK = Map.of(
            DayOfWeek.TUESDAY, new Opening(LocalTime.of(7, 0), LocalTime.of(14, 0)),
            DayOfWeek.WEDNESDAY, new Opening(LocalTime.of(7, 0), LocalTime.of(14, 0)),
            DayOfWeek.THURSDAY, new Opening(LocalTime.of(7, 0), LocalTime.of(14, 0)),
            DayOfWeek.FRIDAY, new Opening(LocalTime.of(7, 0), LocalTime.of(14, 0)),
            DayOfWeek.SATURDAY, new Opening(LocalTime.of(7, 0), LocalTime.of(12, 0)));

    /**
     * The week as it is read, starting on the first open day rather than on Monday — the list on the page
     * has always begun "Tuesday – Friday", because that is when the shop's week begins.
     */
    public static final List<DayOfWeek> READING_ORDER = List.of(
            DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY);

    public record Opening(LocalTime opens, LocalTime closes) {}

    private Shop() {
    }
}
