package com.itsthevine.web;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

/**
 * The opening times, read two ways: as the list on the page, and as the answer to "are they open now?"
 *
 * <p>Both used to be impossible. The hours were three lines of markup, so the only thing the site could do
 * with them was print them — a visitor at half past two on a Sunday had to work out for themselves that
 * the shop was shut. They are data now ({@link Shop#WEEK}), and this turns them into both.
 */
@Service
public class Hours {

    /** One line of the printed list: "Tuesday – Friday", "7:00am – 2:00pm". Closed days say so. */
    public record Span(String days, String hours, boolean closed) {}

    /** "Open until 2:00pm", or "Closed · opens Tuesday at 7:00am". */
    public record Status(boolean open, String summary) {}

    /**
     * The week, with consecutive days that keep the same times collapsed into one line — which is what a
     * shop sign does, and what the three hand-written lines used to do by hand.
     */
    public List<Span> week() {
        List<Span> spans = new ArrayList<>();
        int i = 0;
        while (i < Shop.READING_ORDER.size()) {
            DayOfWeek first = Shop.READING_ORDER.get(i);
            Shop.Opening opening = Shop.WEEK.get(first);

            int j = i;
            while (j + 1 < Shop.READING_ORDER.size()
                    && sameHours(opening, Shop.WEEK.get(Shop.READING_ORDER.get(j + 1)))) {
                j++;
            }
            DayOfWeek last = Shop.READING_ORDER.get(j);

            String days = first == last ? name(first) : name(first) + " – " + name(last);
            spans.add(opening == null
                    ? new Span(days, "Closed", true)
                    : new Span(days, time(opening.opens()) + " – " + time(opening.closes()), false));
            i = j + 1;
        }
        return spans;
    }

    public Status now() {
        return at(ZonedDateTime.now(Shop.ZONE));
    }

    /**
     * @param when the moment to answer for; taken as an argument so a test can ask about a Sunday
     *             afternoon without waiting for one
     */
    public Status at(ZonedDateTime when) {
        LocalTime time = when.toLocalTime();
        Shop.Opening today = Shop.WEEK.get(when.getDayOfWeek());

        if (today != null && !time.isBefore(today.opens()) && time.isBefore(today.closes())) {
            return new Status(true, "Open until " + time(today.closes()));
        }
        if (today != null && time.isBefore(today.opens())) {
            return new Status(false, "Opens at " + time(today.opens()));
        }

        // Closed for the day, so the next answer is on a later day. Seven steps at most, and the shop is
        // open five of them — the loop cannot run out.
        for (int ahead = 1; ahead <= 7; ahead++) {
            DayOfWeek day = when.getDayOfWeek().plus(ahead);
            Shop.Opening next = Shop.WEEK.get(day);
            if (next != null) {
                String when_ = ahead == 1 ? "tomorrow" : name(day);
                return new Status(false, "Closed · opens " + when_ + " at " + time(next.opens()));
            }
        }
        return new Status(false, "Closed");
    }

    private static boolean sameHours(Shop.Opening a, Shop.Opening b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String name(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.US);
    }

    /** "7:00am", "12:00pm" — the way the shop's own sign writes it, not "07:00". */
    private static String time(LocalTime at) {
        int hour = at.getHour() % 12 == 0 ? 12 : at.getHour() % 12;
        return "%d:%02d%s".formatted(hour, at.getMinute(), at.getHour() < 12 ? "am" : "pm");
    }
}
