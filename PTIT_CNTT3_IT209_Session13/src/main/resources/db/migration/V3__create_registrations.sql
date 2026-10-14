CREATE TABLE registrations (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    email_normalized VARCHAR(254) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT registrations_unique_event_email UNIQUE (event_id, email_normalized)
);

CREATE INDEX idx_registrations_event_id ON registrations (event_id);
