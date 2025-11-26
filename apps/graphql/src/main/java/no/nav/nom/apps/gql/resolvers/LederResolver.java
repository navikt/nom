package no.nav.nom.apps.gql.resolvers;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.gql.ErrorCode;
import no.nav.nom.apps.gql.MappingUtils;
import no.nav.nom.apps.gql.exception.LederNotFoundException;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class LederResolver {

    private final static Logger log = LoggerFactory.getLogger(LederResolver.class);

    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrganiseringStateRepository  organiseringStateRepository;

    public LederResolver(OrgEnhetLederStateRepository orgEnhetLederStateRepository, RessursStateRepository ressursStateRepository,
                         OrgEnhetStateRepository orgEnhetStateRepository, OrgtilknytningStateRepository orgtilknytningStateRepository,
                         OrganiseringStateRepository organiseringStateRepository
    ) {
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
    }

    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, PeriodiskUtvalgArg periodiskUtvalg) {
        var relevantLederStates = orgEnhetLederStateRepository.findAllByOrgEnhetId(orgEnhet.getId()).stream()
            .sorted(orgEnhetLederStateComparator)
            .filter(leder -> Util.filterByPeriodiskUtvalg(leder, periodiskUtvalg));

        return relevantLederStates.map(lederState -> getLederRessurs(lederState).map(r -> new OrgEnhetsLeder(r, lederState.startdato(), lederState.sluttdato())))
            .flatMap(Optional::stream)
            .toList();

    }

    public List<LederOrgEnhet> lederFor(Ressurs ressurs, PeriodiskUtvalgArg periodiskUtvalg) {
        return orgEnhetLederStateRepository.findAllByNavident(ressurs.getNavident())
            .stream()
            .filter(lederState -> Util.filterByPeriodiskUtvalg(lederState, periodiskUtvalg))
            .map(orgEnhetLederState -> {
                var maybeOrgEnhet = this.orgEnhetStateRepository.findById(orgEnhetLederState.orgEnhetId());
                if(maybeOrgEnhet.isEmpty()) {
                    // todo review behavior
                    log.error("Expected lederState with fid " + orgEnhetLederState.fid() + " to point to existing orgenhet ");
                    return null;
                };
                return new LederOrgEnhet(
                    MappingUtils.toOrgEnhet(maybeOrgEnhet.get()),
                    orgEnhetLederState.startdato(),
                    orgEnhetLederState.sluttdato()
                );
            })
            .filter(Objects::nonNull)
            .toList();
    }

    public List<RessursLeder> ledere(String navIdent, PeriodiskUtvalgArg periodiskUtvalg) {
        return getLedereForRessurs(navIdent, false).stream()
            .filter(lederForRessurs -> Util.filterByPeriodiskUtvalg(lederForRessurs, periodiskUtvalg))
            .map(lederForRessurs ->
                new RessursLeder(
                    MappingUtils.toRessurs(
                        ressursStateRepository.findByNavident(lederForRessurs.lederNavident()).orElseThrow(LederNotFoundException::new)
                    ),
                    lederForRessurs.erDagligOppfolging(),
                    lederForRessurs.startdato(),
                    lederForRessurs.sluttdato()
                )
            ).toList();
    }




    // ***


    private List<LederForRessurs> getLedereForRessurs(String navident, boolean kunDOL) {
        var oter = orgtilknytningStateRepository.findByNavident(navident);
        if (kunDOL) {
            oter = oter.stream().filter(OrgtilknytningState::erDagligOppfolging).toList();
        }
        List<LederForRessurs> lederForRessurs = new ArrayList<>();
        oter.forEach(ot -> {
            var lederStates = getLedereForOrgEnhetIPeriode(ot.orgEnhetId(), ot.gyldigFom(), ot.gyldigTom());
            lederStates.forEach(lederState -> {
                if (lederState.navident().equals(navident)) {
                    getLedereForOverOrgenheterSomIkkeErAngittNavident(ot.orgEnhetId(), lederState.startdato(), lederState.sluttdato(), navident).forEach(
                        overEnhetLederState -> lederForRessurs.add(new LederForRessurs(
                                overEnhetLederState.navident(),
                                overEnhetLederState.orgEnhetId(),
                                DateUtils.foer(overEnhetLederState.startdato(), lederState.startdato()) ?lederState.startdato() : overEnhetLederState.startdato(),
                                DateUtils.etter(overEnhetLederState.sluttdato(), lederState.sluttdato()) ? lederState.sluttdato() : overEnhetLederState.sluttdato(),
                                ot.erDagligOppfolging()
                            )
                        ));
                } else {
                    lederForRessurs.add(new LederForRessurs(
                        lederState.navident(),
                        ot.orgEnhetId(),
                        DateUtils.foer(lederState.startdato(), ot.gyldigFom()) ? ot.gyldigFom() : lederState.startdato(),
                        DateUtils.etter(lederState.sluttdato(), ot.gyldigTom()) ? ot.gyldigTom() : lederState.sluttdato(),
                        ot.erDagligOppfolging()
                    ));
                }
            });
        });
        return lederForRessurs.stream().sorted(lederForRessursComparator).toList();
    }

    private List<OrgEnhetLederState> getLedereForOrgEnhetIPeriode(String orgEnhetId, LocalDate startdato, LocalDate sluttdato) {
        return orgEnhetLederStateRepository.findAllByOrgEnhetId(orgEnhetId).stream()
            .filter(lederState -> DateUtils.overlapsInterval(startdato, sluttdato, lederState.startdato(), lederState.sluttdato()))
            .sorted(orgEnhetLederStateComparator)
            .toList();
    }

    private List<OrgEnhetLederState> getLedereForOverOrgenheterSomIkkeErAngittNavident(String orgEnhetId, LocalDate startdato, LocalDate sluttdato, String navident) {
        List<OrgEnhetLederState> lederStates = new ArrayList<>();
        getLedereForOrgEnhetIPeriode(orgEnhetId, startdato, sluttdato).forEach(lederState -> {
            if (!lederState.navident().equals(navident)) {
                lederStates.add(
                    lederState.toBuilder()
                        .startdato(DateUtils.foer(lederState.startdato(), startdato) ? startdato : lederState.startdato())
                        .sluttdato(DateUtils.etter(lederState.sluttdato(), sluttdato) ? sluttdato : lederState.sluttdato())
                        .build()
                );
            } else {
                // Har enheten flere overordnede enheter i perioden (e.g. organiseringen har endret seg)?
                organiseringStateRepository.findById(orgEnhetId).stream()
                    .filter(organiseringState -> organiseringState.id().equals(orgEnhetId))
                    .filter(organiseringState -> DateUtils.overlapsInterval(startdato, sluttdato, organiseringState))
                    .forEach(organiseringState ->
                        lederStates.addAll(
                            getLedereForOverOrgenheterSomIkkeErAngittNavident(
                                organiseringState.idOver(),
                                DateUtils.foer(organiseringState.gyldigFom(), startdato) ? startdato : organiseringState.gyldigFom(),
                                DateUtils.etter(organiseringState.gyldigTom(), sluttdato) ? sluttdato : organiseringState.gyldigTom(),
                                navident
                            )
                        )
                    );
            }
        });
        return lederStates.stream().sorted(orgEnhetLederStateComparator).toList();
    }

    // Comparator to sort leaders first by start date, then by end date (nulls last), then by org unit ID
    private final Comparator<LederForRessurs> lederForRessursComparator =
        Comparator.comparing(LederForRessurs::startdato)
            .thenComparing(LederForRessurs::sluttdato, Comparator.nullsLast(LocalDate::compareTo))
            .thenComparing(LederForRessurs::orgEnhetId);

    // Comparator to sort leaders first by start date, then by end date (nulls last), then by org unit ID
    private final Comparator<OrgEnhetLederState> orgEnhetLederStateComparator =
        Comparator.comparing(OrgEnhetLederState::startdato)
            .thenComparing(OrgEnhetLederState::sluttdato, Comparator.nullsLast(LocalDate::compareTo))
            .thenComparing(OrgEnhetLederState::orgEnhetId);

    // ****


    private Optional<Ressurs> getLederRessurs(OrgEnhetLederState lederState) {
        return ressursStateRepository.findByNavident(lederState.navident(), MappingUtils::toRessurs);
    }

    public static GraphQLError handleLederNotFoundException(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof LederNotFoundException) {
            return ErrorCode.NOT_FOUND.construct(environment, "Kunne ikke slå opp leder!");
        }
        return null;
    }


    record LederForRessurs(
        String lederNavident,
        String orgEnhetId,
        LocalDate startdato,
        LocalDate sluttdato,
        boolean erDagligOppfolging
    ) {
        public boolean isActive() {
            return (startdato == null || !startdato.isAfter(LocalDate.now())) &&
                (sluttdato == null || !sluttdato.isBefore(LocalDate.now()));
        }
    }

    // todo, unngå duplisering av koden
    //    stort sett tatt fra PeriodeUtil klassen i api
    private static class DateUtils{
        static final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);

        private static boolean foer(LocalDate dato, LocalDate sjekkMotDato) {
            dato = dato == null ? NO_END_DATE : dato;
            return dato.isBefore(sjekkMotDato == null ? NO_END_DATE : sjekkMotDato);
        }

        static boolean etter(LocalDate dato, LocalDate sjekkMotDato) {
            dato = dato == null ? NO_END_DATE : dato;
            return dato.isAfter(sjekkMotDato == null ? NO_END_DATE : sjekkMotDato);
        }

        static boolean overlapsInterval(LocalDate start, LocalDate end, OrganiseringState organiseringState) {
            end = end == null ? NO_END_DATE : end;
            start = start == null ? LocalDate.MIN : start;
            return organiseringState.gyldigFom().isBefore(dayAfter(end)) &&
                (organiseringState.gyldigTom() == null || organiseringState.gyldigTom().isAfter(dayBefore(start)));
        }

        private static LocalDate dayAfter(LocalDate localDate) { return localDate.plusDays(1); }

        private static LocalDate dayBefore(LocalDate localDate) { return localDate.minusDays(1); }

        public static boolean overlapsInterval(LocalDate startDateA, LocalDate endDateA, LocalDate startDateB, LocalDate endDateB) {
            endDateA = endDateA == null ? NO_END_DATE : endDateA;
            endDateB = endDateB == null ? NO_END_DATE : endDateB;
            startDateA = startDateA == null ? LocalDate.MIN : startDateA;
            startDateB = startDateB == null ? LocalDate.MIN : startDateB;

            return startDateA.isBefore(dayAfter(endDateB)) && endDateA.isAfter(dayBefore(startDateB));
        }
    }


}
