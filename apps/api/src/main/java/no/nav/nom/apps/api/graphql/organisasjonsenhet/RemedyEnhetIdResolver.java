package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import org.springframework.stereotype.Component;

@Component
public class RemedyEnhetIdResolver {

    private final RemedyEnhetMapperService remedyEnhetMapperService;
    public static final String UNLEASH_SHOW_REMEDY_ENHET_ID = "nom.orgenhet.vis_remedy_enhet_id";
    private final Unleash unleash;

    public RemedyEnhetIdResolver(RemedyEnhetMapperService remedyEnhetMapperService, Unleash unleash) {
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.unleash = unleash;
    }

    public String remedyEnhetId(OrgEnhet orgenhet) {
        return unleash.isEnabled(UNLEASH_SHOW_REMEDY_ENHET_ID)
            ? remedyEnhetMapperService.findByNomId(orgenhet.getId()).map(RemedyEnhet::remedyEnhetId).orElse(null)
            : null;
    }
}
