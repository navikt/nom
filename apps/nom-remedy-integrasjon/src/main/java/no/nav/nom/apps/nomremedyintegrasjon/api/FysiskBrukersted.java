package no.nav.nom.apps.nomremedyintegrasjon.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Getter
public class FysiskBrukersted {

    @Id
    private String requestId;

    private String brukerstedNr;

    private String brukerstedNavn;
}
