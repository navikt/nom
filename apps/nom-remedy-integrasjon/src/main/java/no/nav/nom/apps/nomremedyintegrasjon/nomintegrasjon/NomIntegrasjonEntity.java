package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "NOM_INTEGRASJON")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
public class NomIntegrasjonEntity {

    @Id
    String entryId;

    String entryStatus;

    String entryOperasjon;

    String entryBestiller;

    String tempChar1;

    String tempChar2;

    String tempInteger;

    String fornavn;

    String etternavn;

    String navId;

    String fodselsnummer;

    Integer sektor;

    String enhetsId;

    String enhetsnavn;

    String fysiskBrukerstedId;

    String fysiskBrukerstedNavn;

    Long tiltredelsesdato;

    Long utlopsdato;

    String mobil;

}
