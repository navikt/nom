package no.nav.nom.apps.navident.eventsource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.apps.navident.domain.IdentType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

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
