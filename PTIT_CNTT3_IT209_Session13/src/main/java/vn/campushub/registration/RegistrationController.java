package vn.campushub.registration;

import java.util.UUID;

import vn.campushub.registration.dto.CreateRegistrationRequest;
import vn.campushub.registration.dto.RegistrationConfirmationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{id}/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationConfirmationResponse register(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRegistrationRequest request) {
        return registrationService.register(id, request.fullName(), request.email());
    }
}
