package vn.campushub.event;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(UUID eventId) {
        super("Không tìm thấy sự kiện " + eventId + ".");
    }
}
