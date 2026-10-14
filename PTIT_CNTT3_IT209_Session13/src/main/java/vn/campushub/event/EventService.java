package vn.campushub.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import vn.campushub.event.dto.EventDetailResponse;
import vn.campushub.event.dto.EventSummaryResponse;
import vn.campushub.registration.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public EventService(EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    public List<EventSummaryResponse> searchUpcoming(String query, String category) {
        String normalizedQuery = normalize(query);
        String normalizedCategory = normalize(category);
        return eventRepository.searchUpcoming(OffsetDateTime.now(), normalizedQuery, normalizedCategory)
                .stream()
                .map(event -> toSummary(event, registrationRepository.countByEvent_Id(event.getId())))
                .toList();
    }

    public EventDetailResponse getUpcoming(UUID id) {
        Event event = eventRepository.findUpcomingById(id, OffsetDateTime.now())
                .orElseThrow(() -> new EventNotFoundException(id));
        EventSummaryResponse response = toSummary(event, registrationRepository.countByEvent_Id(event.getId()));
        return new EventDetailResponse(
                response.id(), response.title(), response.category(), response.description(),
                response.location(), response.startsAt(), response.endsAt(), response.capacity(),
                response.registeredCount(), response.remainingSeats(), response.registrationOpen());
    }

    private EventSummaryResponse toSummary(Event event, long registeredCount) {
        return new EventSummaryResponse(
                event.getId(), event.getTitle(), event.getCategory(), event.getDescription(),
                event.getLocation(), event.getStartsAt(), event.getEndsAt(), event.getCapacity(),
                registeredCount, Math.max(0, event.getCapacity() - (int) registeredCount), event.isRegistrationOpen());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
