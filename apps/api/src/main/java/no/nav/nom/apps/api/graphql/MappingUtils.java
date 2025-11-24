package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.api.graphql.AgressoInfo;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import no.nav.nom.contract.nom.state.RessursState;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;

public class MappingUtils {

    public static Ressurs toRessurs(RessursState ressursState) {
        final var sektor = singletonList(ressursState.sektor());

        var sladdetPersonIdent = ressursState.personident().replaceFirst(".{5}$","*****");
        var startdato = ressursState.startdato();
        var sluttdato = ressursState.sluttdato();
        var primaryTelefonnummer = getPrimaryTelefonnummer(ressursState);
        var telefon = getTelefonliste(ressursState);
        var tilknytningstype = isNull(ressursState.tilknytningtype()) ? null : ressursState.tilknytningtype();
        return Ressurs.builder()
            .navident(ressursState.navident())
            .navident(ressursState.navident())
            .personident(sladdetPersonIdent)
            .personIdent(sladdetPersonIdent)
            .sektor(sektor)
            .gjeldendeSektor(ressursState.sektor() != null ? ressursState.sektor() : Sektor.NAV_STATLIG)
            .identType(ressursState.identType() != null ? ressursState.identType() : IdentType.UKJENT)
            .startdato(startdato)
            .sluttdato(sluttdato)
            .fornavn(ressursState.visningsnavn().getFornavnMellomnavn())
            .etternavn(ressursState.visningsnavn().etternavn())
            .visningsNavn(ressursState.visningsnavn().getFormattedVisningsnavn())
            .visningsnavn(ressursState.visningsnavn().getFormattedVisningsnavn())
            .primaryTelefon(primaryTelefonnummer)
            .telefon(telefon)
            .gjeldendeTilknytningtype(tilknytningstype)
            .build();
    }

    // Primær telefonnummer er enten registrert statlig tjenestetelefon/kontortelefon. Importert fra Telenor via DVH
    // Hvis dette ikke finnes leveres telefonnummer registrert i NOM.
    private static String getPrimaryTelefonnummer(RessursState ressursState) {
        var nummer = ressursState.jobbtelefon() != null ? ressursState.jobbtelefon().nummer() : ressursState.primaryTelefonnummer();
        return tlfFormat(nummer);
    }

    // Denne inneholder både telefonnummer for tjeneste/kontortelefon fra Telenor/DVH
    // Og telefonnummer registrert i NOM. Telefonnummer registrert i NOM får typen PRIVAT_TELEFON selv om det evt er en kommunalt eid tjenestetelefon
    private static List<Telefon> getTelefonliste(RessursState ressursState) {
        var privattelefon = ressursState.primaryTelefonnummer();
        var jobbtelefon = ressursState.jobbtelefon();
        var telefoner = new ArrayList<Telefon>();
        if (jobbtelefon != null) {
            telefoner.add(new Telefon(tlfFormat(jobbtelefon.nummer()), jobbtelefon.type(), null ));
        }
        if (privattelefon != null) {
            telefoner.add(new Telefon(tlfFormat(privattelefon), TelefonType.PRIVAT_TELEFON, null));
        }
        return telefoner;
    }

    private static String tlfFormat(String nummer) {
        if (nummer == null) return null;

        var trimmed = nummer.replaceAll("\\s", "");
        return switch (trimmed.length()) {
            case 10 -> trimmed.startsWith("47") ? trimmed.substring(2) : trimmed;
            case 11 -> trimmed.startsWith("+47") ? trimmed.substring(3) : trimmed;
            default -> trimmed;
        };
    }


    public static OrgEnhet toOrgEnhet(IOrgenhet orgEnhet){
        return OrgEnhet.builder()
                .id(orgEnhet.getId())
                .agressoId(orgEnhet.getAgressoId())
                .orgNiv(orgEnhet.getOrgNiv())
                .navn(orgEnhet.getNavn())
                .agressoOrgenhetType(orgEnhet.getAgressoOrgenhetType())
                .gyldigFom(orgEnhet.getGyldigFom())
                .gyldigTom(orgEnhet.getGyldigTom())
                .nomNivaa(orgEnhet.getNomNivaa())
                .orgEnhetsType(getOrgEnhetsType(orgEnhet.getOrgEnhetsType()))
                .agressoInfo(AgressoInfo.builder()
                    .agressoId(orgEnhet.getAgressoId())
                    .agressoOrgNiv(orgEnhet.getOrgNiv())
                    .agressoOrgenhetType(orgEnhet.getAgressoOrgenhetType())
                    .build()
                )
                .build();
    }

    private static OrgEnhetsType getOrgEnhetsType(OrgEnhetsType orgEnhetsType){
        if (!isNull(orgEnhetsType) && orgEnhetsType == OrgEnhetsType.DIR) {
            return OrgEnhetsType.DIREKTORAT;
        }
        return orgEnhetsType;
    }
}
