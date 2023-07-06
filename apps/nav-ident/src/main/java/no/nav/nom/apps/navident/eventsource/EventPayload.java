package no.nav.nom.apps.navident.eventsource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.apps.navident.domain.IdentType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Builder(toBuilder = true)
@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class EventPayload {

    @Column(name = "NAVIDENT")
    String navident;

    @Column(name = "PERSONID")
    String personid;

    @Column(name = "IDENT_TYPE")
    @Enumerated(EnumType.STRING)
    IdentType identType;
}
