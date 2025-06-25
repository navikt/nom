package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.r2dbc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Table("delayed_event")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class DelayedEventR2dbc {

    @Id
    @Column("delayed_event_id")
    private UUID delayedEventId;

    @Column("delayed_until")
    private LocalDate delayedUntil;

    @Column("event")
    private Event event;
}
