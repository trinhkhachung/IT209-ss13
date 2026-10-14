package vn.campushub.event;

import java.util.List;
import java.util.UUID;

import vn.campushub.event.dto.EventDetailResponse;
import vn.campushub.event.dto.EventSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventSummaryResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category) {
        return eventService.searchUpcoming(query, category);
    }

    @GetMapping("/{id}")
    public EventDetailResponse detail(@PathVariable UUID id) {
        return eventService.getUpcoming(id);
    }
}
