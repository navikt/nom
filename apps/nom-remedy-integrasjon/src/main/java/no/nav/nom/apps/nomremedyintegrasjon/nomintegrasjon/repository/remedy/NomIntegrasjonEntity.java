package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import lombok.ToString;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "NOM_INTEGRASJON")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class NomIntegrasjonEntity {

    @Id
    @Column(name = "ENTRY_ID")
    private String entryId;

    @Column(name = "ENTRY_STATUS")
    private String entryStatus;

    @Column(name = "ENTRY_OPERASJON")
    private String entryOperasjon;

    @Column(name = "ENTRY_BESTILLER")
    private String entryBestiller;

    @Column(name = "FORNAVN")
    private String fornavn;

    @Column(name = "ETTERNAVN")
    private String etternavn;

    @Column(name = "NAV_ID")
    private String navId;

    @Column(name = "FODSELSNUMMER")
    private String fodselsnummer;

    @Column(name = "SEKTOR")
    private Integer sektor;

    @Column(name = "ENHETS_ID")
    private String enhetsId;

    @Column(name = "FYSISK_BRUKERSTED_ID")
    private String fysiskBrukerstedId;

    @Column(name = "TILTREDELSESDATO")
    private Long tiltredelsesdato;

    @Column(name = "UTLOPSDATO")
    private Long utlopsdato;

    @Column(name = "MOBIL")
    private String mobil;

    @Column(name = "DIREKTE_TELEFON")
    private String direkteTelefon;

    @Column(name = "MASSEFLYTTING")
    private String masseflytting;

}
