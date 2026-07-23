package com.itsthevine.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import net.thebennett.platform.data.BaseEntity;

/**
 * A contact-form submission, recorded before we try to deliver it.
 *
 * <p>Writing this row first means a relay outage costs a notification, not the enquiry itself —
 * {@code delivered} shows which ones still need chasing up by hand.
 */
@Entity
@Table(name = "enquiry")
public class ContactEnquiry extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private boolean delivered;

    protected ContactEnquiry() {
        // for JPA
    }

    public ContactEnquiry(String name, String email, String message) {
        this.name = name;
        this.email = email;
        this.message = message;
        this.delivered = false;
    }

    public void markDelivered() {
        this.delivered = true;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getMessage() { return message; }
    public boolean isDelivered() { return delivered; }
}
