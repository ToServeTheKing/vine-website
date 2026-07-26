package com.itsthevine.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import net.thebennett.platform.data.BaseEntity;

/**
 * A footnote for the catering page as a whole — the terms that apply whichever table you were
 * reading, like "we're happy to make changes, the price may change with them".
 *
 * <p>Its own table rather than a note on a package, because these outlive any one table: delete the
 * wedding package and the page still has terms.
 */
@Entity
@Table(name = "catering_note")
public class CateringNote extends BaseEntity {

    @Column(nullable = false, length = 600)
    private String body;

    @Column(name = "position", nullable = false)
    private int position;

    protected CateringNote() {
        // for JPA
    }

    public CateringNote(String body, int position) {
        say(body);
        this.position = position;
    }

    public final void say(String body) {
        this.body = Text.required(body, 600, "A note with nothing in it — delete it rather than blanking it.");
    }

    public void moveTo(int position) {
        this.position = position;
    }

    public String getBody() {
        return body;
    }

    public int getPosition() {
        return position;
    }
}
