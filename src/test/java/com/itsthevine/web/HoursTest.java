package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

/**
 * The opening times, as a list and as an answer.
 *
 * <p>No Spring here on purpose: this is a calculation over a constant, and it should be testable at the
 * speed of a calculation. Every "now" is passed in, so the Sunday-afternoon case doesn't need a Sunday.
 */
class HoursTest {

    private final Hours hours = new Hours();

    @Test
    void collapsesTheWeekTheWayASignWouldWriteIt() {
        assertThat(hours.week()).extracting(Hours.Span::days, Hours.Span::hours)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Tuesday – Friday", "7:00am – 2:00pm"),
                        org.assertj.core.groups.Tuple.tuple("Saturday", "7:00am – 12:00pm"),
                        org.assertj.core.groups.Tuple.tuple("Sunday – Monday", "Closed"));
    }

    @Test
    void theWeekStartsOnTheDayTheShopsWeekStarts() {
        // Not Monday: the shop is shut on Monday, and a list that opens with a closed day reads as an
        // apology. This is the order the printed list has always used.
        assertThat(hours.week().getFirst().days()).startsWith("Tuesday");
        assertThat(hours.week().getLast().closed()).isTrue();
    }

    @Test
    void saysHowLongIsLeftWhenTheShopIsOpen() {
        Hours.Status wednesdayMorning = hours.at(chicago("2026-07-29T09:15"));
        assertThat(wednesdayMorning.open()).isTrue();
        assertThat(wednesdayMorning.summary()).isEqualTo("Open until 2:00pm");
    }

    @Test
    void countsTheMinuteOfOpeningAsOpenAndTheMinuteOfClosingAsShut() {
        // The two edges anyone would get wrong, and the ones a customer stands at the door for.
        assertThat(hours.at(chicago("2026-07-29T07:00")).open()).isTrue();
        assertThat(hours.at(chicago("2026-07-29T13:59")).open()).isTrue();
        assertThat(hours.at(chicago("2026-07-29T14:00")).open()).isFalse();
        assertThat(hours.at(chicago("2026-07-29T06:59")).open()).isFalse();
    }

    @Test
    void tellsYouWhenItOpensAgainRatherThanJustSayingClosed() {
        // Saturday afternoon, shut since noon: the next open day is Tuesday, three days away.
        Hours.Status saturdayAfternoon = hours.at(chicago("2026-08-01T15:00"));
        assertThat(saturdayAfternoon.open()).isFalse();
        assertThat(saturdayAfternoon.summary()).isEqualTo("Closed · opens Tuesday at 7:00am");

        // Monday: tomorrow reads better than naming the day.
        assertThat(hours.at(chicago("2026-08-03T10:00")).summary())
                .isEqualTo("Closed · opens tomorrow at 7:00am");

        // Before opening on a day it does open, the day itself is redundant.
        assertThat(hours.at(chicago("2026-07-29T06:30")).summary()).isEqualTo("Opens at 7:00am");
    }

    private static ZonedDateTime chicago(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(Shop.ZONE);
    }
}
