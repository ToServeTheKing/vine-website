package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.itsthevine.web.domain.ContactEnquiry;
import com.itsthevine.web.domain.ContactEnquiryRepository;

@SpringBootTest
@Testcontainers
class ContactControllerTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Activates the contact starter without pointing it at anything real.
        registry.add("platform.contact.to", () -> "shop@example.com");
        registry.add("platform.contact.from", () -> "noreply@example.com");
    }

    /** Nothing in a test run may reach a real relay. */
    @MockitoBean
    JavaMailSender mailSender;

    // Boot 4's starter-test no longer ships @AutoConfigureMockMvc, so build MockMvc from the context
    // directly — it's plain spring-test and needs no extra module.
    @Autowired
    WebApplicationContext context;

    @Autowired
    ContactEnquiryRepository enquiries;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        enquiries.deleteAll();
    }

    private static String body(String name, String email, String message) {
        return """
               {"name":"%s","email":"%s","message":"%s"}
               """.formatted(name, email, message);
    }

    @Test
    void acceptsAnEnquiryEmailsItAndRecordsItAsDelivered() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ada", "ada@example.com", "Do you do wedding cakes?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(mailSender).send(any(SimpleMailMessage.class));

        assertThat(enquiries.findAll()).singleElement().satisfies(e -> {
            assertThat(e.getName()).isEqualTo("Ada");
            assertThat(e.getEmail()).isEqualTo("ada@example.com");
            assertThat(e.getMessage()).isEqualTo("Do you do wedding cakes?");
            assertThat(e.isDelivered()).isTrue();
            assertThat(e.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void rejectsJunkWithoutStoringItOrEmailing() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ada", "not-an-address", "hello")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("That email address does not look right."));

        verifyNoInteractions(mailSender);
        assertThat(enquiries.findAll()).isEmpty();
    }

    @Test
    void keepsTheEnquiryWhenTheRelayIsDown() throws Exception {
        // The whole reason the row is written before delivery: a broken relay must not lose business.
        doThrow(new MailSendException("relay down")).when(mailSender).send(any(SimpleMailMessage.class));

        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ada", "ada@example.com", "Cinnamon rolls for 30?")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Could not send the message."));

        assertThat(enquiries.findAll())
                .singleElement()
                .extracting(ContactEnquiry::isDelivered, ContactEnquiry::getMessage)
                .containsExactly(false, "Cinnamon rolls for 30?");
    }

    @Test
    void trimsBeforeStoring() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content(body("  Ada  ", "  ada@example.com  ", "  hello  ")))
                .andExpect(status().isOk());

        assertThat(enquiries.findAll()).singleElement().satisfies(e -> {
            assertThat(e.getName()).isEqualTo("Ada");
            assertThat(e.getEmail()).isEqualTo("ada@example.com");
        });
    }
}
