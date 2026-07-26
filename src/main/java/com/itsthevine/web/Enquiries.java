package com.itsthevine.web;

import org.springframework.stereotype.Service;

import com.itsthevine.web.domain.ContactEnquiry;
import com.itsthevine.web.domain.ContactEnquiryRepository;

import net.thebennett.platform.contact.ContactService;
import net.thebennett.platform.contact.Enquiry;

/**
 * Taking an enquiry: check it, write it down, then try to deliver it.
 *
 * <p>The order matters and is the reason this is a service rather than three lines in a controller.
 * Validation first, so junk never reaches the table; the enquiry is recorded BEFORE delivery is
 * attempted, so a relay outage costs a notification rather than somebody's order; and
 * {@code delivered} is only set once the relay has actually taken it, which is what makes the
 * undelivered ones findable later.
 *
 * <p>Two things submit enquiries — the page's own form and {@code /api/contact} — and they must not
 * drift into two different orderings of those steps.
 */
@Service
public class Enquiries {

    private final ContactService contact;
    private final ContactEnquiryRepository enquiries;

    public Enquiries(ContactService contact, ContactEnquiryRepository enquiries) {
        this.contact = contact;
        this.enquiries = enquiries;
    }

    /**
     * @throws net.thebennett.platform.contact.ContactException if it's not a usable enquiry, or the
     *         relay refused it — the message is written for the visitor to read
     */
    public void receive(String name, String email, String message) {
        Enquiry enquiry = Enquiry.of(name, email, message);

        contact.validate(enquiry);
        ContactEnquiry recorded = enquiries.save(
                new ContactEnquiry(enquiry.name(), enquiry.email(), enquiry.message()));

        contact.submit(enquiry);

        recorded.markDelivered();
        enquiries.save(recorded);
    }
}
