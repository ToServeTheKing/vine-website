package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A class on a tag that carries {@code th:replace} is a class that never reaches the page.
 *
 * <p>{@code th:replace} substitutes the whole host element for the fragment — the fragment's own
 * {@code class} attribute is what gets rendered, and anything written on the calling tag is dropped.
 * Silently: Thymeleaf does not warn, the page still renders, and it looks nearly right. That is what
 * makes it worth a test rather than a code review.
 *
 * <p>It had happened seven times before anyone went looking. Two homepage section titles were written
 * with an {@code mb-12} and rendered flush against the grid below them; the contact page's two headings
 * had the same; the catering divider's {@code mt-10} and the footer's went the same way; and the
 * address rendered a shade darker than the hours beside it because its {@code text-bakery-800} was
 * dropped too. Every one of those is invisible in the source and obvious on the page.
 *
 * <p>The fix in each case is to give the fragment a parameter for the class, which is why
 * {@code visit :: hours} and {@code visit :: address} take the element's own class as well as the
 * row's or the link's. This test is the thing that stops the next one being written.
 *
 * <p>No Spring context and no database: it reads the templates off disk, so it costs nothing.
 * {@code th:insert} is deliberately not checked — that one keeps the host tag, so a class on it is
 * fine.
 */
class TemplateHygieneTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    /** Any tag, including one written across several lines. */
    private static final Pattern TAG = Pattern.compile("<[a-zA-Z][^>]*?>", Pattern.DOTALL);

    /** A plain {@code class=}, not {@code th:class=} and not {@code th:classappend=}. */
    private static final Pattern PLAIN_CLASS = Pattern.compile("(?<!th:)\\bclass\\s*=");

    @Test
    @DisplayName("no template writes a class on a tag that th:replace is about to discard")
    void noClassOnAReplacedTag() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            files.filter(p -> p.toString().endsWith(".html")).sorted().forEach(file -> {
                String source = read(file);
                Matcher tag = TAG.matcher(source);
                while (tag.find()) {
                    String element = tag.group();
                    if (element.contains("th:replace") && PLAIN_CLASS.matcher(element).find()) {
                        long line = source.substring(0, tag.start()).chars().filter(c -> c == '\n').count() + 1;
                        offenders.add("%s:%d — %s".formatted(file, line, element.replaceAll("\\s+", " ")));
                    }
                }
            });
        }

        assertThat(offenders)
                .describedAs("""
                        These tags carry both a class and a th:replace. th:replace swaps the tag out for \
                        the fragment, so the class is dropped and the page renders without it. Pass the \
                        class to the fragment as a parameter instead — see fragments/visit.html.""")
                .isEmpty();
    }

    /** Guards the guard: a test that reads no files would pass forever. */
    @Test
    @DisplayName("the templates are actually being read")
    void theTemplatesAreThere() throws IOException {
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            assertThat(files.filter(p -> p.toString().endsWith(".html")).count())
                    .describedAs("templates directory at %s", TEMPLATES.toAbsolutePath())
                    .isGreaterThanOrEqualTo(10);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + file, e);
        }
    }
}
