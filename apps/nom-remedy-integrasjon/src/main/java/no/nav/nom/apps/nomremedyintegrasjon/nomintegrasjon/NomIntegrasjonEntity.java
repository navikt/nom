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

    String fornavn;

    String etternavn;

    String navId;

    String fodselsnummer;

    Integer sektor;

    String enhetsId;

    String fysiskBrukerstedId;

    Long tiltredelsesdato;

    Long utlopsdato;

}
