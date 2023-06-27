package no.nav.nom.apps.api.graphql.ressurs;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetMapper;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningDvhService;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Component

public class OrgTilknytningResolver {

    private final OrgtilknytningService orgtilknytningStateService;
    private final OrgtilknytningDvhService orgtilknytningDvhService;
    private final IOrgenhetService orgenhetService;

    private final OrgEnhetMapper orgEnhetMapper;

    public static final String UNLEASH_ORGTILKNYTNING_BRUK_EVTSRC_STATE = "nom.orgtilknytning.bruk_evtsrc_state";
    private final Unleash unleash;

    @Autowired
    public OrgTilknytningResolver(
        OrgtilknytningService orgtilknytningStateService, OrgtilknytningDvhService orgtilknytningDvhService, IOrgenhetService orgenhetService,
        OrgEnhetMapper orgEnhetMapper, Unleash unleash
    ) {
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.orgtilknytningDvhService = orgtilknytningDvhService;
        this.orgenhetService = orgenhetService;
        this.orgEnhetMapper = orgEnhetMapper;
        this.unleash = unleash;
    }

    public Map<Ressurs, List<RessursOrgTilknytning>> getOrgTilknytning(Collection<Ressurs> ressurser) {
        return ressurser.stream()
                .collect(toMap(Function.identity(), this::getOrgTilknytningerForRessurs));
    }

    private List<RessursOrgTilknytning> getOrgTilknytningerForRessurs(Ressurs ressurs) {
        if (unleash.isEnabled(UNLEASH_ORGTILKNYTNING_BRUK_EVTSRC_STATE)) {
            return orgtilknytningStateService
                .getOrgtilknytningerByNavIdent(ressurs.getNavIdent()).stream()
                .map(this::fromOrgtilknytningState)
                .toList();
        } else {
            return orgtilknytningDvhService.getKoblingerByNavIdentOld(ressurs.getNavIdent())
                .stream()
                .map(this::fromDvhKobling)
                .toList();
        }
    }

    private RessursOrgTilknytning fromOrgtilknytningState(OrgtilknytningState orgtilknytning) {
        final var orgenhet = orgenhetService.getById(orgtilknytning.orgEnhetId()).get();
        final var orgEnhetGqlResponse = orgEnhetMapper.convertToOrgEnhet(orgenhet);

        return RessursOrgTilknytning.builder()
            .organisasjonsenhet(orgEnhetGqlResponse)
            .orgEnhet(orgEnhetGqlResponse)
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom().orElse(null))
            .build();
    }

    private RessursOrgTilknytning fromDvhKobling(Orgtilknytning orgtilknytning) {
        final var orgenhet = orgenhetService.getById(orgtilknytning.getOrgEnhetId()).get();
        final var orgEnhetGqlResponse = orgEnhetMapper.convertToOrgEnhet(orgenhet);

        return RessursOrgTilknytning.builder()
                .organisasjonsenhet(orgEnhetGqlResponse)
                .orgEnhet(orgEnhetGqlResponse)
                .gyldigFom(orgtilknytning.getGyldigFom())
                .gyldigTom(orgtilknytning.getGyldigTom().orElse(null))
                .build();
    }
}
