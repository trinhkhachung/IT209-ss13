package vn.campushub.registration;

import java.time.OffsetDateTime;
import java.util.UUID;

import vn.campushub.event.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email_normalized", nullable = false, length = 254)
    private String emailNormalized;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Registration() {
    }

    public Registration(Event event, String fullName, String emailNormalized) {
        this.id = UUID.randomUUID();
        this.event = event;
        this.fullName = fullName;
        this.emailNormalized = emailNormalized;
    }

    public UUID getId() {
        return id;
    }
}
