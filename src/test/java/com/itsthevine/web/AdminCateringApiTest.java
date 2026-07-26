package com.itsthevine.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
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
 * The catering admin over HTTP, signed in: the paths, the JSON field names and the shape of a refusal.
 *
 * <p>{@code CateringMenuTest} covers what the tables mean; this covers the surface the admin screen
 * actually calls. Both matter — a table can be modelled perfectly and still be unreachable because a
 * URL has a typo in it, and the screen reads the sentence out of a ProblemDetail rather than showing
 * a status code.
 */
@SpringBootTest(properties = {
        "SECURITY_MODE=OIDC",
        // Stated outright rather than via issuer-uri, which would fetch a discovery document at
        // startup — that needs the network and a real identity provider. (As in AdminSecurityTest.)
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
// Rolled back per test, so each one starts from the seeded page. MockMvc runs the controller on this
// thread, which is what lets the test's transaction wrap the whole request.
@Transactional
class AdminCateringApiTest {

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

    @Test
    void handsTheEditorEveryTableWithItsColumnsPricedAndItsLinesFilledIn() throws Exception {
        mvc.perform(get("/api/admin/catering").with(user("morissa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packages.length()").value(3))
                .andExpect(jsonPath("$.packages[0].name").value("Office"))
                // Written out, not a number the browser would have to format.
                .andExpect(jsonPath("$.packages[0].tiers[0].price").value("$24"))
                .andExpect(jsonPath("$.packages[0].rows[0].label").value("Mini muffins"))
                .andExpect(jsonPath("$.packages[0].rows[0].values[1]").value("18 items"))
                .andExpect(jsonPath("$.notes.length()").value(2));
    }

    @Test
    void savesAWholeTableAtOnce() throws Exception {
        String office = """
                {"name":"Office boxes","blurb":"For meetings.",
                 "tiers":[{"id":null,"label":"Dozen","price":"$18.50"}],
                 "rows":[{"id":null,"label":"Mini muffins","values":["12 items"]}],
                 "notes":["Two days' notice, please."]}
                """;

        mvc.perform(put("/api/admin/catering/packages/1").with(user("morissa")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(office))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office boxes"))
                // The column and line were new; they come back with ids so the next save edits them
                // rather than adding more.
                .andExpect(jsonPath("$.tiers[0].id").isNumber())
                .andExpect(jsonPath("$.tiers[0].price").value("$18.50"))
                .andExpect(jsonPath("$.rows[0].id").isNumber())
                .andExpect(jsonPath("$.notes[0]").value("Two days' notice, please."));
    }

    @Test
    void refusesAnArrangementThatWouldMisprintThePricesAndSaysWhy() throws Exception {
        // Two columns, three entries on the line: exactly the mistake that shifts a box's contents.
        String crooked = """
                {"name":"Parties",
                 "tiers":[{"id":null,"label":"Small","price":"54"},{"id":null,"label":"Large","price":"98"}],
                 "rows":[{"id":null,"label":"Cake","values":["6 in","8 in","10 in"]}],
                 "notes":[]}
                """;

        mvc.perform(put("/api/admin/catering/packages/2").with(user("morissa")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(crooked))
                .andExpect(status().isBadRequest())
                // `detail` is the field the SPA shows the editor.
                .andExpect(jsonPath("$.detail")
                        .value(containsString("3 entries but the table has 2 columns")));
    }

    @Test
    void reordersTheTablesFromItsOwnPathRatherThanReadingOrderAsAnId() throws Exception {
        // /packages/order and /packages/{id} are both PUT; Spring's literal-beats-template rule is
        // what keeps "order" from arriving as a table id.
        mvc.perform(put("/api/admin/catering/packages/order").with(user("morissa")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[3,1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Weddings"))
                .andExpect(jsonPath("$[2].name").value("Parties"));
    }

    @Test
    void replacesThePageNotesWithTheListItWasGiven() throws Exception {
        mvc.perform(put("/api/admin/catering/notes").with(user("morissa")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"Prices may change.\",\"   \"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("Prices may change."));
    }
}
