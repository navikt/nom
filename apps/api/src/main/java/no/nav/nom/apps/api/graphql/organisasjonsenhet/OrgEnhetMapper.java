package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import org.springframework.stereotype.Component;

@Component
public class OrgEnhetMapper {

    private final RemedyEnhetMapperService remedyEnhetMapperService;

    public OrgEnhetMapper(RemedyEnhetMapperService remedyEnhetMapperService) {
        this.remedyEnhetMapperService = remedyEnhetMapperService;
    }
    public OrgEnhet convertToOrgEnhet(IOrgenhet orgenhet) {
        var remedyId = remedyEnhetMapperService.findByNomId(orgenhet.getId())
            .map(RemedyEnhet::remedyEnhetId).orElse(null);

        return OrgEnhet.builder()
            .id(orgenhet.getId())
            .agressoId(orgenhet.getAgressoId())
            .orgNiv(orgenhet.getOrgNiv())
            .navn(orgenhet.getNavn())
            .agressoOrgenhetType(orgenhet.getAgressoOrgenhetType())
            .gyldigFom(orgenhet.getGyldigFom())
            .gyldigTom(orgenhet.getGyldigTom().orElse(null))
            .orgEnhetsKategori(orgenhet.getNomNivaa().orElse(null))
            .nomNivaa(orgenhet.getNomNivaa().orElse(null))
            .orgEnhetsType(orgenhet.getOrgEnhetsType().orElse(null))
            .remedyEnhetId(remedyId)
            .build();
    }
}
