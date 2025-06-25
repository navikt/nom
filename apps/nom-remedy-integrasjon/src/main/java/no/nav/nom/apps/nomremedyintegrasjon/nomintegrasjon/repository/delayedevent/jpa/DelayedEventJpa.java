package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.jpa;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.contract.nom.eventsource.Event;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "delayed_event")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class DelayedEventJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "delayed_event_id")
    private UUID delayedEventId;

    @Column(name = "delayed_until")
    private LocalDate delayedUntil;

    @Column(name = "event")
    @Type(JsonBinaryType.class)
    private Event event;
}
