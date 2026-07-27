package com.itsthevine.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The pages, rendered.
 *
 * <p>These assert what a visitor and a crawler are actually served — the catalogue in the HTML rather
 * than in a JSON call the page makes later, and a real per-page {@code <title>}. That second one is the
 * whole reason {@code PageMetaController} existed; this replaces its test.
 *
 * <p>They are also the only thing that catches a broken template: a Thymeleaf expression that names a
 * model attribute wrongly fails at render time, not at compile time.
 */
@SpringBootTest(properties = {
        "platform.storage.access-key=test",
        "platform.storage.secret-key=test",
        "platform.contact.to=test@example.com",
        "platform.contact.from=noreply@example.com",
        "site.base-url=https://itsthevine.test",
        "site.assets.base-url=https://s3.example.test/itsthevine"
})
@Testcontainers
class SiteControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    WebApplicationContext context;

    @Autowired
    com.itsthevine.web.domain.ContactEnquiryRepository enquiries;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void everyPageStatesItsOwnTitleAndDescription() throws Exception {
        // One generic shell for every page was the SPA's problem, and the reason a controller used to
        // rewrite the head with regular expressions.
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>The Vine Coffeehouse + Bakery</title>")))
                .andExpect(content().string(containsString("A locally owned coffeehouse and bakery")))
                .andExpect(content().string(containsString("og:url\" content=\"https://itsthevine.test\"")));
        mvc.perform(get("/products"))
                .andExpect(content().string(containsString("<title>Our products · The Vine Coffeehouse + Bakery</title>")))
                .andExpect(content().string(containsString("og:url\" content=\"https://itsthevine.test/products\"")));
        mvc.perform(get("/catering"))
                .andExpect(content().string(containsString("<title>Goodie boxes &amp; catering · The Vine Coffeehouse + Bakery</title>")));
        mvc.perform(get("/history"))
                .andExpect(content().string(containsString("<title>Our story · The Vine Coffeehouse + Bakery</title>")));
        mvc.perform(get("/contact"))
                .andExpect(content().string(containsString("<title>Contact us · The Vine Coffeehouse + Bakery</title>")));
    }

    @Test
    void theCatalogueIsInTheHtmlRatherThanFetchedAfterwards() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk())
                // A real product, its category, and a photo URL built from the bucket config.
                .andExpect(content().string(containsString("76th Birthday Cake")))
                .andExpect(content().string(containsString("https://s3.example.test/itsthevine/images/")))
                // The filter buttons are links now, so every filtered view is a URL a crawler can follow.
                .andExpect(content().string(containsString("href=\"/products?category=Cakes\"")));
    }

    @Test
    void theCategoryFilterIsAppliedByTheServer() throws Exception {
        mvc.perform(get("/products").param("category", "Pie")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Blueberry Cream Pie")))
                .andExpect(content().string(not(containsString("76th Birthday Cake"))))
                // The chosen one is marked for a screen reader, not just coloured in.
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void theCateringTablesAreRenderedFromTheDatabase() throws Exception {
        mvc.perform(get("/catering")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Office boxes")))
                .andExpect(content().string(containsString("Weddings")))
                // Prices as the server writes them — the page never formats money.
                .andExpect(content().string(containsString("$24")))
                .andExpect(content().string(containsString("$236")))
                // A cell and a note, in the wording a customer reads rather than the spreadsheet's.
                // The office box is a total mixed in sixes, not three separate things.
                .andExpect(content().string(containsString("Any mix of mini muffins")))
                .andExpect(content().string(containsString("18 items")))
                .andExpect(content().string(containsString("About 6–8 people")))
                .andExpect(content().string(containsString("Baked in sixes")))
                // One card per size, and one enquiry link per table — not one per card, which would have
                // read "Ask about the 15–20 people".
                // The size label is in the HTML as written; the small caps are CSS.
                .andExpect(content().string(containsString("Large")))
                .andExpect(content().string(containsString("Ask about office boxes")))
                // The price grid is gone: it was the source spreadsheet, rendered.
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    void thePagesShareOneHeaderThatOffersTheCateringPage() throws Exception {
        // A page nobody can navigate to isn't finished.
        mvc.perform(get("/")).andExpect(content().string(containsString("href=\"/catering\"")));
        mvc.perform(get("/products")).andExpect(content().string(containsString("href=\"/catering\"")));
    }

    @Test
    void theNavMarksThePageYouAreOn() throws Exception {
        // Generated from the path the controller set, so a wrong model attribute would mark nothing at
        // all — and nothing at all looks exactly like a page that simply has no active link.
        mvc.perform(get("/catering"))
                .andExpect(content().string(containsString("href=\"/catering\" aria-current=\"page\"")));
        mvc.perform(get("/products"))
                .andExpect(content().string(containsString("href=\"/products\" aria-current=\"page\"")))
                .andExpect(content().string(not(containsString("href=\"/catering\" aria-current"))));
    }

    @Test
    void everyPageOffersTheKeyboardAWayPastTheNav() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("href=\"#content\"")))
                .andExpect(content().string(containsString("id=\"content\"")));
    }

    @Test
    void aCateringButtonStartsTheEnquiryOffAboutThatTable() throws Exception {
        mvc.perform(get("/contact").param("about", "Weddings"))
                .andExpect(content().string(containsString("like to ask about weddings catering")));
    }

    @Test
    void anAboutParameterThatIsntATableIsIgnored() throws Exception {
        // The value lands in a box on the page, so it is matched against the real table names rather
        // than echoed. Thymeleaf escapes it either way; a link that puts someone else's words in front
        // of a customer still shouldn't work.
        mvc.perform(get("/contact").param("about", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(not(containsString("like to ask about"))));
    }

    @Test
    void aFilledInTrapIsThankedAndThrownAway() throws Exception {
        // The bot is told the same thing a person is told — anything else is a training signal — and
        // nothing is recorded or sent. The enquiry table is what proves the second half.
        mvc.perform(post("/contact")
                        .param("name", "Bot").param("email", "bot@example.com")
                        .param("message", "Cheap watches").param("website", "http://example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your message is on its way")));
        assertThat(enquiries.count()).isZero();
    }

    @Test
    void anUnknownPageIsNotFound() throws Exception {
        // Status only: MockMvc does not run the servlet container's error dispatch, so the body of the
        // rendered error/404.html page can't be asserted here. It is checked against a running container
        // instead — the page itself is a template like any other, and the layout it uses is covered above.
        mvc.perform(get("/no-such-page")).andExpect(status().isNotFound());
    }
}
