package no.nav.nom.apps.api.tilgangsenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.OrgEnhetOverordnetResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TilgangsenhetMapper {

    private final OrgenhetService orgenhetService;
    private final OrgEnhetOverordnetResolver overordnetResolver;

    public TilgangsenhetMapper(
        OrgenhetService orgenhetService,
        OrgEnhetOverordnetResolver overordnetResolver
    ) {
        this.orgenhetService = orgenhetService;

        this.overordnetResolver = overordnetResolver;
    }

    public String byNomId(String nomId) {
        var orgenhet = orgenhetService.getById(nomId);
        while (orgenhet.isPresent()) {
            if (StringUtils.isNotBlank(orgenhet.get().getTilgangsenhetId())) {
                return orgenhet.get().getTilgangsenhetId();
            }

            orgenhet = overordnetResolver.findOverordnetOrgEnhet(orgenhet.get().getFid()).map(OrgEnhet::new);
        }

        log.debug("Fant ikke tilgansenhetId for nomId {} ", nomId);
        return null;
    }
}
