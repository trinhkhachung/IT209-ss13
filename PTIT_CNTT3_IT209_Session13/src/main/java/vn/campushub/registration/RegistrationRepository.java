package vn.campushub.registration;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    long countByEvent_Id(UUID eventId);

    boolean existsByEvent_IdAndEmailNormalized(UUID eventId, String emailNormalized);
}
