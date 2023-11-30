package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "NOM_INTEGRASJON")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
public class NomIntegrasjonEntity {

    @Id
    private String entryId;

    private String entryStatus;

    private String entryOperasjon;

    private String entryBestiller;

    private String fornavn;

    private String etternavn;

    private String navId;

    private String fodselsnummer;

    private Integer sektor;

    private String enhetsId;

    private String fysiskBrukerstedId;

    private Long tiltredelsesdato;

    private Long utlopsdato;

}
