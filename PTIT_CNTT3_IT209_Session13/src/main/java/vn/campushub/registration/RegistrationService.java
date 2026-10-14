package vn.campushub.registration;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import vn.campushub.event.Event;
import vn.campushub.event.EventNotFoundException;
import vn.campushub.event.EventRepository;
import vn.campushub.event.dto.EventDetailResponse;
import vn.campushub.event.dto.EventSummaryResponse;
import vn.campushub.registration.dto.RegistrationConfirmationResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public RegistrationService(EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public RegistrationConfirmationResponse register(UUID eventId, String fullName, String email) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (!event.isRegistrationOpen() || !event.getStartsAt().isAfter(OffsetDateTime.now())) {
            throw new RegistrationConflictException("REGISTRATION_CLOSED", "Sự kiện đã đóng đăng ký.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (registrationRepository.existsByEvent_IdAndEmailNormalized(eventId, normalizedEmail)) {
            throw duplicateRegistration();
        }

        long registeredCount = registrationRepository.countByEvent_Id(eventId);
        if (registeredCount >= event.getCapacity()) {
            throw new RegistrationConflictException("EVENT_FULL", "Sự kiện đã hết chỗ.");
        }

        Registration registration = new Registration(event, fullName.trim(), normalizedEmail);
        try {
            Registration saved = registrationRepository.saveAndFlush(registration);
            return new RegistrationConfirmationResponse(saved.getId(), "CONFIRMED");
        } catch (DataIntegrityViolationException exception) {
            throw duplicateRegistration();
        }
    }

    private RegistrationConflictException duplicateRegistration() {
        return new RegistrationConflictException("REGISTRATION_DUPLICATE", "Email này đã đăng ký sự kiện.");
    }
}
