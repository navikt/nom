package no.nav.nom.apps.api.graphql.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg.AKTIVE;

@Slf4j
@Component

public class OrgTilknytningResolver {

    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;

    @Autowired
    public OrgTilknytningResolver(OrgtilknytningService orgtilknytningService, OrgenhetService orgenhetService) {
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
    }

    public Map<Ressurs, List<RessursOrgTilknytning>> getOrgTilknytninger(Collection<Ressurs> ressurser, PeriodiskUtvalgArg periodiskUtvalgArg) {
        return ressurser.stream()
            .collect(toMap(Function.identity(), r -> getOrgTilknytningerForRessurs(r, periodiskUtvalgArg)));
    }

    public List<RessursOrgTilknytning> getOrgTilknytningerForRessurs(Ressurs ressurs, PeriodiskUtvalgArg periodiskUtvalgArg) {
        return orgtilknytningService
            .getByNavIdent(ressurs.getNavIdent()).stream()
            .filter(ot -> filterByPeriodiskUtvalg(ot, periodiskUtvalgArg))
            .map(this::fromOrgtilknytningState)
            .toList();
    }

    private boolean filterByPeriodiskUtvalg(OrgtilknytningState orgtilknytningState, PeriodiskUtvalgArg periodiskUtvalg) {
        if (periodiskUtvalg == null) periodiskUtvalg = AKTIVE; // Default

        return switch (periodiskUtvalg) {
            case ALLE -> true;
            case AKTIVE -> PeriodeUtil.isCurrentPeriod(orgtilknytningState);
            case FREMTIDIGE -> PeriodeUtil.isFuturePeriod(orgtilknytningState);
            case HISTORISKE -> PeriodeUtil.isHistoricPeriod(orgtilknytningState);
        };
    }


    @Deprecated
    public Map<Ressurs, List<RessursOrgTilknytning>> getOrgTilknytning(Collection<Ressurs> ressurser) {
        return ressurser.stream()
            .collect(toMap(Function.identity(), this::getOrgTilknytningerForRessurs));
    }

    @Deprecated
    private List<RessursOrgTilknytning> getOrgTilknytningerForRessurs(Ressurs ressurs) {
        return orgtilknytningService
            .getByNavIdent(ressurs.getNavIdent()).stream()
            .map(this::fromOrgtilknytningState)
            .toList();
    }

    private RessursOrgTilknytning fromOrgtilknytningState(OrgtilknytningState orgtilknytning) {
        final var orgenhet = orgenhetService.getById(orgtilknytning.orgEnhetId()).get();
        final var orgEnhetGqlResponse = MappingUtils.toOrgEnhet(orgenhet);

        return RessursOrgTilknytning.builder()
            .organisasjonsenhet(orgEnhetGqlResponse)
            .orgEnhet(orgEnhetGqlResponse)
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom())
            .sektor(orgtilknytning.sektor())
            .tilknytningType(orgtilknytning.orgtilknytningType())
            .build();
    }
}
