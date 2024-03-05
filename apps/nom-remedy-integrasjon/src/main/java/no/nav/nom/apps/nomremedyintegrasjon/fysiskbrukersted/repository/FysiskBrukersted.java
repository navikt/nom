package no.nav.nom.apps.nomremedyintegrasjon.fysiskbrukersted.repository;

import jakarta.persistence.Column;
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
    @Column(name = "REQUEST_ID")
    private String requestId;

    @Column(name = "BRUKERSTED_NR")
    private String brukerstedNr;

    @Column(name = "BRUKERSTED_NAVN")
    private String brukerstedNavn;

}
