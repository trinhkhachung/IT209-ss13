package vn.campushub.event.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventSummaryResponse(
        UUID id,
        String title,
        String category,
        String description,
        String location,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        int capacity,
        long registeredCount,
        int remainingSeats,
        boolean registrationOpen) {
}
