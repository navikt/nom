package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.exception.LederNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.List;

import static no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg.AKTIVE;

@Component("OrgEnhetsLeder")
public class LederResolver {
    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetLederService orgEnhetLederService;

    public LederResolver(RessursStateRepository ressursStateRepository,
                         OrgEnhetLederService orgEnhetLederService
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetLederService = orgEnhetLederService;
    }

    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, PeriodiskUtvalgArg periodiskUtvalg) {
        return orgEnhetLederService.getLedereForOrgEnhet(orgEnhet.getId()).stream()
            .filter(leder -> filterByPeriodiskUtvalg(leder, periodiskUtvalg))
            .map(leder -> new OrgEnhetsLeder(
                getRessurs(leder),
                leder.startdato(), leder.sluttdato())
            )
            .toList();
    }

    private boolean filterByPeriodiskUtvalg(OrgEnhetLederState leder, PeriodiskUtvalgArg periodiskUtvalg) {
        if (periodiskUtvalg == null) periodiskUtvalg = AKTIVE; // Default

        return switch (periodiskUtvalg) {
            case ALLE -> true;
            case AKTIVE -> PeriodeUtil.isCurrentPeriod(leder);
            case FREMTIDIGE -> PeriodeUtil.isFuturePeriod(leder);
            case HISTORISKE -> PeriodeUtil.isHistoricPeriod(leder);
        };
    }

    private Ressurs getRessurs(OrgEnhetLederState leder) {
        return ressursStateRepository.findByNavident(leder.navident(), MappingUtils::toRessurs).orElseThrow(LederNotFoundException::new);
    }

    public static GraphQLError handleLederNotFoundException(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof LederNotFoundException) {
            return ErrorCode.NOT_FOUND.construct(environment, "Kunne ikke slå opp leder!");
        }
        return null;
    }
}
