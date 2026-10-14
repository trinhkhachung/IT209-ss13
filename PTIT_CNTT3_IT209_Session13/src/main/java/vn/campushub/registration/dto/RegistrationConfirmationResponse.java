package vn.campushub.registration.dto;

import java.util.UUID;

public record RegistrationConfirmationResponse(UUID registrationId, String status) {
}
