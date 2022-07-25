package no.nav.nom.apps.navident.eventsource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "SOURCE_EVENT")
@EntityListeners(AuditingEntityListener.class)
public class SourceEvent {

    @Id
    @Column(name = "ID")
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGenerator")
    @SequenceGenerator(name = "idGenerator", sequenceName = "EVENT_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "EVENT_TYPE")
    @Enumerated(EnumType.STRING)
    EventType event;

    @Column(name = "EVENT_TS")
    @CreatedDate
    LocalDateTime eventTS;

    @Column(name = "BESTILLER")
    @CreatedBy
    String bestiller;

    @Embedded
    EventPayload payload;

    public String maskedToString() {
        SourceEvent masked = this.toBuilder().build(); // Is not a deep copy
        masked.payload = payload.toBuilder().build();
        masked.payload.personid = mask(payload.personid);

        return masked.toString();
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}
