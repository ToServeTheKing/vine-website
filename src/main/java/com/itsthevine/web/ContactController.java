package com.itsthevine.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itsthevine.web.domain.ContactEnquiry;
import com.itsthevine.web.domain.ContactEnquiryRepository;

import net.thebennett.platform.contact.ContactException;
import net.thebennett.platform.contact.ContactService;
import net.thebennett.platform.contact.Enquiry;

/**
 * The contact form. Keeps the response shape the old Next route used ({@code {ok:true}} /
 * {@code {error:"..."}}), because the error text is shown to the visitor as-is.
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contact;
    private final ContactEnquiryRepository enquiries;

    public ContactController(ContactService contact, ContactEnquiryRepository enquiries) {
        this.contact = contact;
        this.enquiries = enquiries;
    }

    public record Submission(String name, String email, String message) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Submission body) {
        Enquiry enquiry = Enquiry.of(body.name(), body.email(), body.message());

        // Validate first so junk never reaches the table, then record it BEFORE attempting delivery:
        // if the relay is down we still have the enquiry, flagged undelivered.
        contact.validate(enquiry);
        ContactEnquiry recorded = enquiries.save(
                new ContactEnquiry(enquiry.name(), enquiry.email(), enquiry.message()));

        contact.submit(enquiry);

        recorded.markDelivered();
        enquiries.save(recorded);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * The visitor sees this text, so it must stay the wording the service chose — never a stack trace
     * or a generic 500.
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(ContactException.class)
    public ResponseEntity<Map<String, Object>> handle(ContactException ex) {
        HttpStatus status = ex.isClientError() ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }
}
