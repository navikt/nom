package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "aggregate_state")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class AggregateState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "aggregate_state_id")
    private UUID aggregateStateId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "delayed_until")
    private LocalDate delayedUntil;

    @Column(name = "navident")
    private String navident;

    @Column(name = "masseflytting")
    private Boolean masseflytting;

    @Column(name = "state_type")
    private String stateType;

    @Column(name = "state_fid")
    private String stateFid;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_fid")
    private String eventFid;
}
