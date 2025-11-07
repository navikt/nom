package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.api.graphql.AgressoInfo;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.RessursState;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;

public class MappingUtils {

    public static Ressurs toRessurs(RessursState ressursState) {
        final var sektor = singletonList(ressursState.sektor());

        var sladdetPersonIdent = ressursState.personident().replaceFirst(".{5}$","*****");
        var startdato = ressursState.startdato();
        var sluttdato = ressursState.sluttdato();
        var primaryTelefonnummer = ressursState.primaryTelefonnummer();
        var tilknytningstype = isNull(ressursState.tilknytningtype()) ? null : ressursState.tilknytningtype();
        return Ressurs.builder()
                .navident(ressursState.navident())
                .navIdent(ressursState.navident())
                .personident(sladdetPersonIdent)
                .personIdent(sladdetPersonIdent)
                .sektor(sektor)
                .gjeldendeSektor(ressursState.sektor() != null ? ressursState.sektor() : Sektor.NAV_STATLIG)
                .startdato(startdato)
                .sluttdato(sluttdato)
                .fornavn(ressursState.visningsnavn().getFornavnMellomnavn())
                .etternavn(ressursState.visningsnavn().etternavn())
                .visningsNavn(ressursState.visningsnavn().getFormattedVisningsnavn())
                .visningsnavn(ressursState.visningsnavn().getFormattedVisningsnavn())
                .primaryTelefon(primaryTelefonnummer)
                .gjeldendeTilknytningstype(tilknytningstype)
                .build();
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
                .orgEnhetsType(getType(orgEnhet.getOrgEnhetsType()))
                .agressoInfo(AgressoInfo.builder()
                    .agressoId(orgEnhet.getAgressoId())
                    .agressoOrgNiv(orgEnhet.getOrgNiv())
                    .agressoOrgenhetType(orgEnhet.getAgressoOrgenhetType())
                    .build()
                )
                .build();
    }

    private static OrgEnhetsType getType(OrgEnhetsType orgEnhetsType){
        if (!isNull(orgEnhetsType) && orgEnhetsType == OrgEnhetsType.DIR) {
            return OrgEnhetsType.DIREKTORAT;
        }
        return orgEnhetsType;
    }
}
