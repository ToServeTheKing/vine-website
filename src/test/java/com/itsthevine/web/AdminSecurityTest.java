package com.itsthevine.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * What an anonymous visitor can and cannot reach.
 *
 * <p>This is the test that matters most on this branch: the admin API can create, edit and delete the
 * menu, and the whole site is otherwise public. Running with {@code platform.security.mode=OIDC}, as
 * production does — the default of NONE would leave everything open and prove nothing.
 */
@SpringBootTest(properties = {
        "platform.security.mode=OIDC",
        // Endpoints stated outright rather than an issuer-uri: an issuer-uri makes Spring fetch the
        // discovery document at startup, which needs the network and a real identity provider.
        "spring.security.oauth2.client.provider.authentik.authorization-uri=https://sso.example.test/authorize",
        "spring.security.oauth2.client.provider.authentik.token-uri=https://sso.example.test/token",
        "spring.security.oauth2.client.provider.authentik.jwk-set-uri=https://sso.example.test/jwks",
        "spring.security.oauth2.client.provider.authentik.user-info-uri=https://sso.example.test/userinfo",
        "spring.security.oauth2.client.provider.authentik.user-name-attribute=preferred_username",
        "spring.security.oauth2.client.registration.authentik.scope=openid,profile,email",
        "spring.security.oauth2.client.registration.authentik.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.authentik.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "spring.security.oauth2.client.registration.authentik.client-id=test",
        "spring.security.oauth2.client.registration.authentik.client-secret=test",
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
        // .apply(springSecurity()) is not optional here: webAppContextSetup alone leaves the security
        // filter chain out, so every protected path returns 200 and the test proves nothing.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void everyAdminEndpointIsClosedToAnonymousVisitors() throws Exception {
        // 401 rather than a redirect: the platform's security starter answers /api/** with a status so
        // the SPA can handle it, instead of bouncing an XHR to the identity provider.
        mvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/enquiries")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"category\":\"Cakes\",\"imageKeys\":[\"a\"]}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/images/presign-upload?filename=x.jpg").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theShopStaysPublic() throws Exception {
        // The whole point of authenticated-paths: locking the admin API must not lock the menu.
        mvc.perform(get("/api/products")).andExpect(status().isOk());
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(post("/api/contact").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"email\":\"nope\",\"message\":\"hi\"}"))
                .andExpect(status().isBadRequest());   // reached the controller, rejected on content
    }

    @Test
    void theContactFormNeedsItsCsrfToken() throws Exception {
        // Turning on the security starter turns on CSRF, which applies to the PUBLIC contact form too.
        // Without the token the form silently 403s — the SPA reads the XSRF-TOKEN cookie and sends
        // X-XSRF-TOKEN for exactly this reason.
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"email\":\"ada@example.com\",\"message\":\"hi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meIsPublicAndSaysNobodyIsSignedIn() throws Exception {
        // If this required a login, every anonymous visitor would be bounced to Authentik on page load.
        mvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.admin").value(false));
    }
}
