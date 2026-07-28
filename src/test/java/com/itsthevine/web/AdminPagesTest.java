package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.unit.DataSize;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The admin, signed in and driven the way a browser drives it: form posts and redirects.
 *
 * <p>These replace the JSON admin's tests. The screen used to be React talking to {@code
 * /api/admin/**}; it is now Thymeleaf forms, so what is worth asserting is that a form arrives bound
 * correctly, that an edit lands, that a refusal comes back readable rather than as a stack trace, and
 * that a structural button changes the draft without writing it.
 *
 * <p>Runs with {@code SECURITY_MODE=OIDC}, because that is the only condition under which the admin
 * exists at all.
 */
@SpringBootTest(properties = {
        "SECURITY_MODE=OIDC",
        // Endpoints stated outright rather than an issuer-uri, which would make Spring fetch the
        // discovery document at startup — that needs the network and a real identity provider.
        "spring.security.oauth2.client.provider.authentik.authorization-uri=https://sso.example.test/authorize",
        "spring.security.oauth2.client.provider.authentik.token-uri=https://sso.example.test/token",
        "spring.security.oauth2.client.provider.authentik.jwk-set-uri=https://sso.example.test/jwks",
        "spring.security.oauth2.client.provider.authentik.user-info-uri=https://sso.example.test/userinfo",
        "spring.security.oauth2.client.provider.authentik.user-name-attribute=preferred_username",
        "spring.security.oauth2.client.registration.authentik.client-id=test",
        "spring.security.oauth2.client.registration.authentik.client-secret=test",
        "spring.security.oauth2.client.registration.authentik.scope=openid,profile,email",
        "spring.security.oauth2.client.registration.authentik.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.authentik.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "platform.contact.to=test@example.com",
        "platform.contact.from=noreply@example.com",
        "platform.storage.access-key=test",
        "platform.storage.secret-key=test"
})
@Testcontainers
// Rolled back per test, so each starts from the seeded catalogue. MockMvc runs the controller on this
// thread, which is what lets the test's transaction wrap the whole request.
@Transactional
class AdminPagesTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        // .apply(springSecurity()) is not optional: webAppContextSetup alone leaves the filter chain out.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    /**
     * The bug this guards was an ABSENCE: nothing configured multipart, so Boot's 1 MB default applied and
     * every photo off a phone was rejected by the container before {@code ProductPhotoService} — the class
     * whose whole job is resizing phone photos — could see it. A default is exactly the kind of thing that
     * comes back silently, so the numbers are asserted rather than trusted.
     */
    @Test
    void photosOffAPhoneFitInsideTheUploadLimits() {
        MultipartProperties multipart = context.getBean(MultipartProperties.class);

        assertThat(multipart.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(15));
        assertThat(multipart.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(60));
        // Without this the throw happens inside the container's parameter parsing, where no
        // @ExceptionHandler can reach it — which is what made an over-sized photo a 500.
        assertThat(multipart.isResolveLazily()).isTrue();
        // The default is 1 MB. If this ever passes, the fix has been undone.
        assertThat(multipart.getMaxFileSize()).isNotEqualTo(DataSize.ofMegabytes(1));
    }

    @Test
    void theCatalogueScreenShowsWhatIsOnThePageWithItsPhotos() throws Exception {
        mvc.perform(get("/admin").with(user("morissa")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("76th Birthday Cake")))
                // The photo URL and the key beside it: the key is what the arrange buttons name it by.
                .andExpect(content().string(containsString("products/76th_birthday_cake.webp")))
                .andExpect(content().string(containsString("On the page (40)")))
                // The filter list, with the count that decides whether Delete is offered.
                .andExpect(content().string(containsString("Cookies")));
    }

    @Test
    void renamingAnItemLandsAndSaysSo() throws Exception {
        mvc.perform(post("/admin/items/1").with(user("morissa")).with(csrf())
                        .param("do", "save")
                        .param("name", "76th Birthday Cake (chocolate)")
                        .param("category", "Cakes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("done", containsString("Saved")));

        mvc.perform(get("/admin").with(user("morissa")))
                .andExpect(content().string(containsString("76th Birthday Cake (chocolate)")));
    }

    @Test
    void aRefusalComesBackAsASentenceTheEditorCanActOn() throws Exception {
        // Cookies has items filed under it, and deleting the button shouldn't decide what happens to them.
        mvc.perform(post("/admin/categories/1").with(user("morissa")).with(csrf()).param("do", "delete"))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("problem", containsString("still filed under Cookies")));
    }

    @Test
    void movingAnItemUpFromTheTopIsNotAnError() throws Exception {
        // The button is disabled in the page, but a stale page could still post this.
        mvc.perform(post("/admin/items/1").with(user("morissa")).with(csrf()).param("do", "move:-1"))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    void theTableEditorRendersTheStoredTableAsAForm() throws Exception {
        mvc.perform(get("/admin/catering/tables/1").with(user("morissa")))
                .andExpect(status().isOk())
                // Indexed names are what let Spring bind the grid back into the right cells.
                .andExpect(content().string(containsString("name=\"columns[0].label\"")))
                .andExpect(content().string(containsString("name=\"lines[0].values[1]\"")))
                .andExpect(content().string(containsString("value=\"18 items\"")))
                // The price round-trips as text: it came out "$24" and goes back the same way.
                .andExpect(content().string(containsString("value=\"$24\"")));
    }

    @Test
    void savingTheTableWritesEveryCell() throws Exception {
        mvc.perform(post("/admin/catering/tables/1").with(user("morissa")).with(csrf())
                        .param("do", "save")
                        .param("name", "Office boxes")
                        .param("blurb", "For meetings.")
                        .param("columns[0].id", "1").param("columns[0].label", "Small").param("columns[0].price", "$26")
                        .param("lines[0].id", "1").param("lines[0].label", "Mini muffins")
                        .param("lines[0].values[0]", "14 items")
                        .param("notes[0]", "Two days' notice, please."))
                .andExpect(redirectedUrl("/admin/catering"))
                .andExpect(flash().attribute("done", containsString("Saved the Office boxes table")));

        mvc.perform(get("/api/catering"))
                .andExpect(content().string(containsString("Office boxes")))
                .andExpect(content().string(containsString("$26")))
                .andExpect(content().string(containsString("14 items")));
    }

    @Test
    void addingAColumnKeepsWhatWasTypedAndWritesNothing() throws Exception {
        mvc.perform(post("/admin/catering/tables/1").with(user("morissa")).with(csrf())
                        .param("do", "add-column")
                        .param("name", "Office")
                        .param("columns[0].id", "1").param("columns[0].label", "Small").param("columns[0].price", "$24")
                        .param("lines[0].id", "1").param("lines[0].label", "Mini muffins")
                        // A cell edited but not yet saved: it has to survive the round trip.
                        .param("lines[0].values[0]", "13 items"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"13 items\"")))
                // The new column exists in the form, and every line grew an entry to match it.
                .andExpect(content().string(containsString("name=\"columns[1].label\"")))
                .andExpect(content().string(containsString("name=\"lines[0].values[1]\"")))
                .andExpect(content().string(containsString("Not saved yet")));

        // And nothing was written: the live page still says what it said.
        mvc.perform(get("/api/catering"))
                .andExpect(content().string(containsString("12 items")));
    }

    @Test
    void removingAColumnTakesItsCellsOutOfEveryLine() throws Exception {
        mvc.perform(post("/admin/catering/tables/1").with(user("morissa")).with(csrf())
                        .param("do", "remove-column:0")
                        .param("name", "Office")
                        .param("columns[0].id", "1").param("columns[0].label", "Small").param("columns[0].price", "$24")
                        .param("columns[1].id", "2").param("columns[1].label", "Medium").param("columns[1].price", "$32")
                        .param("lines[0].id", "1").param("lines[0].label", "Mini muffins")
                        .param("lines[0].values[0]", "12 items")
                        .param("lines[0].values[1]", "18 items"))
                .andExpect(status().isOk())
                // What is left is the Medium column and, under it, Medium's entry — not Small's.
                .andExpect(content().string(containsString("value=\"Medium\"")))
                .andExpect(content().string(containsString("value=\"18 items\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"12 items\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"columns[1].label\""))));
    }

    @Test
    void aPriceThatIsntAPriceComesBackWithTheWorkStillInTheForm() throws Exception {
        mvc.perform(post("/admin/catering/tables/1").with(user("morissa")).with(csrf())
                        .param("do", "save")
                        .param("name", "Office")
                        .param("columns[0].id", "1").param("columns[0].label", "Small").param("columns[0].price", "ask us")
                        .param("lines[0].id", "1").param("lines[0].label", "Mini muffins")
                        .param("lines[0].values[0]", "12 items"))
                // Not a redirect: a redirect would throw the work away and leave them guessing.
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("isn&#39;t a price")))
                .andExpect(content().string(containsString("value=\"ask us\"")));
    }

    @Test
    void thePageNotesAreReplacedByWhatTheFormSubmits() throws Exception {
        mvc.perform(post("/admin/catering/notes").with(user("morissa")).with(csrf())
                        .param("notes", "Prices may change.")
                        .param("notes", "   ")
                        .param("notes", "Two weeks' notice for a wedding."))
                .andExpect(redirectedUrl("/admin/catering"));

        mvc.perform(get("/api/catering"))
                .andExpect(content().string(containsString("Two weeks' notice for a wedding.")))
                // The blank box was a deletion, not a note: two notes came back, not three.
                .andExpect(content().string(containsString("Prices may change.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("\"   \""))));
    }
}
