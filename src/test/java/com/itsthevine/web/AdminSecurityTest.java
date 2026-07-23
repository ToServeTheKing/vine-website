package com.itsthevine.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * What an anonymous visitor can and cannot reach, with security on as production runs it.
 *
 * <p>The admin can create, edit, reorder and delete the whole menu, and the shop is otherwise public —
 * so the boundary between them is the thing most worth a test. Runs with {@code SECURITY_MODE=OIDC}
 * because the default of NONE leaves everything open and would prove nothing.
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
class AdminSecurityTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        // .apply(springSecurity()) is not optional: webAppContextSetup alone leaves the filter chain
        // out, so every protected path returns 200 and the test would assert nothing.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void everyAdminApiIsClosedToAnonymousVisitors() throws Exception {
        // csrf() on the writes, so these assert AUTHORIZATION (401), not a missing token.
        mvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/categories")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/products").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/categories").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theAdminPageRedirectsABrowserToLogin() throws Exception {
        // Protecting /admin server-side is what makes sign-in work: a browser opening it is bounced to
        // Authentik and comes back signed in. The redirect only fires for a request that prefers HTML —
        // the platform answers */* (a fetch/XHR) with a bare 401 so the SPA can handle it — so this
        // must send a browser's Accept header to see the 302. (Verified against a running container.)
        mvc.perform(get("/admin").header("Accept", "text/html,application/xhtml+xml"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void theShopStaysPublic() throws Exception {
        // Locking the admin must not lock the menu.
        mvc.perform(get("/api/products")).andExpect(status().isOk());
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    void theContactFormStillNeedsItsCsrfToken() throws Exception {
        // Enabling the security starter enables CSRF for the PUBLIC contact form too. Without the token
        // it 403s; the SPA reads the XSRF-TOKEN cookie and sends X-XSRF-TOKEN.
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"email\":\"ada@example.com\",\"message\":\"hi\"}"))
                .andExpect(status().isForbidden());
        // With the token it reaches the controller (400 on the deliberately bad email below).
        mvc.perform(post("/api/contact").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"email\":\"nope\",\"message\":\"hi\"}"))
                .andExpect(status().isBadRequest());
    }
}
