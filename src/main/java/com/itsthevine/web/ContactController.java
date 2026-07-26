package com.itsthevine.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.thebennett.platform.contact.ContactException;

/**
 * The contact form as JSON. Keeps the response shape the old Next route used ({@code {ok:true}} /
 * {@code {error:"..."}}), because the error text is shown to the visitor as-is.
 *
 * <p>The page's own form posts to {@code /contact} and renders a page rather than reading this. Both
 * go through {@link Enquiries}, so there is one order of operations for taking an enquiry.
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final Enquiries enquiries;

    public ContactController(Enquiries enquiries) {
        this.enquiries = enquiries;
    }

    public record Submission(String name, String email, String message) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Submission body) {
        enquiries.receive(body.name(), body.email(), body.message());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * The visitor sees this text, so it must stay the wording the service chose — never a stack trace
     * or a generic 500.
     */
    @ExceptionHandler(ContactException.class)
    public ResponseEntity<Map<String, Object>> handle(ContactException ex) {
        HttpStatus status = ex.isClientError() ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }
}
