package com.itsthevine.web;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import net.thebennett.platform.test.PlatformWebContract;

/** Everything in {@link PlatformWebContract} — what this app must do because it is on the platform. */
@SpringBootTest(properties = {
        "platform.contact.to=test@example.com",
        "platform.contact.from=noreply@example.com"
})
@Testcontainers
class PlatformContractTest extends PlatformWebContract {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));
}
