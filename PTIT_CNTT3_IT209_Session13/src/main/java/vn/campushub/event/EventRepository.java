package vn.campushub.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("""
            select event from Event event
            where event.startsAt >= :now
              and (:query = '' or lower(event.title) like concat('%', lower(:query), '%')
                   or lower(event.description) like concat('%', lower(:query), '%'))
              and (:category = '' or lower(event.category) = lower(:category))
            order by event.startsAt asc
            """)
    List<Event> searchUpcoming(@Param("now") OffsetDateTime now,
                               @Param("query") String query,
                               @Param("category") String category);

    @Query("select event from Event event where event.id = :id and event.startsAt >= :now")
    Optional<Event> findUpcomingById(@Param("id") UUID id, @Param("now") OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from Event event where event.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") UUID id);
}
