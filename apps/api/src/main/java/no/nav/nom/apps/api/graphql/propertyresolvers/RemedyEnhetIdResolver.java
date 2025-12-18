package no.nav.nom.apps.api.graphql.propertyresolvers;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import org.springframework.stereotype.Component;

@Component
public class RemedyEnhetIdResolver {

    private final RemedyEnhetMapperService remedyEnhetMapperService;

    public RemedyEnhetIdResolver(RemedyEnhetMapperService remedyEnhetMapperService) {
        this.remedyEnhetMapperService = remedyEnhetMapperService;
    }

    public String remedyEnhetId(OrgEnhet orgenhet) {
        return remedyEnhetMapperService.findByNomId(orgenhet.getId()).map(RemedyEnhet::remedyEnhetId).orElse(null);
    }

    public String remedyEnhetId(IOrgenhet orgenhet) {
        return remedyEnhetMapperService.findByNomId(orgenhet.getId()).map(RemedyEnhet::remedyEnhetId).orElse(null);
    }

    public String remedyEnhetId(String nomId) {
        return remedyEnhetMapperService.findByNomId(nomId).map(RemedyEnhet::remedyEnhetId).orElse(null);
    }
}
