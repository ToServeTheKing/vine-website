package com.itsthevine.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * What this app must do because it is on the platform.
 *
 * <p>It used to extend {@code PlatformWebContract} from platform-starter-test and inherit these five
 * assertions verbatim. It can't any more, and the reason is worth stating: the shared contract asserts
 * that an unknown extension-less path forwards to {@code /index.html}, because it was written when every
 * app on the platform was a React SPA. There is no SPA here at all now — no shell to forward to — so
 * that assertion describes an app this no longer is. The contract's own test methods are package-private,
 * so it cannot be overridden from here.
 *
 * <p>The other four are restated below unchanged, so this app still fails the build on the regression
 * the contract exists for (an {@code /api} typo answering with a page and a 200).
 *
 * <p><b>Platform follow-up:</b> {@code PlatformWebContract} should decide which of the two routing
 * behaviours to assert by reading {@code platform.web.spa.enabled} — then one contract would cover both
 * kinds of app and this file could go back to inheriting it.
 */
@SpringBootTest(properties = {
        // The storage starter activates on its default endpoint, so an S3 client is built even in
        // tests and fails on blank keys.
        "platform.storage.access-key=test",
        "platform.storage.secret-key=test",
        "platform.contact.to=test@example.com",
        "platform.contact.from=noreply@example.com"
})
@Testcontainers
class PlatformContractTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("an /api path that matches no controller returns 404, not a page")
    void unknownApiPathIsNotFound() throws Exception {
        mvc.perform(get("/api/a-path-no-controller-serves")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a nested unknown /api path returns 404 too")
    void unknownNestedApiPathIsNotFound() throws Exception {
        mvc.perform(get("/api/deeper/still/not/real")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("health reports UP")
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("liveness and readiness probes are exposed")
    void probesAreExposed() throws Exception {
        // Docker's HEALTHCHECK and any future orchestrator depend on these existing.
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("routing is server-side: an unknown path is a 404, and so is /admin with no identity provider")
    void routingIsServerSide() throws Exception {
        mvc.perform(get("/some/client/side/route")).andExpect(status().isNotFound());
        // No SECURITY_MODE here, so AdminController does not exist — "no Authentik configured" means "no
        // admin", and it 404s like any other unknown path rather than exposing catalogue writes.
        // AdminSecurityTest covers the other half: with OIDC on, /admin exists and needs a login.
        mvc.perform(get("/admin")).andExpect(status().isNotFound());
    }
}
