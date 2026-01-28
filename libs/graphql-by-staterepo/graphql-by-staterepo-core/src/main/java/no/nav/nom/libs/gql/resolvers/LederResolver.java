package no.nav.nom.libs.gql.resolvers;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.gql.ErrorCode;
import no.nav.nom.libs.gql.MappingUtils;
import no.nav.nom.libs.gql.exception.LederNotFoundException;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
                }
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
            .map(lederForRessurs -> {
                var maybeOrgEnhet = this.orgEnhetStateRepository.findById(lederForRessurs.orgEnhetId());
                if (maybeOrgEnhet.isEmpty()) {
                    log.error("Expected LederForRessurs with {} to point to existing orgenhet", lederForRessurs.orgEnhetId());
                    return null;
                }
                return new RessursLeder(
                    MappingUtils.toRessurs(
                        ressursStateRepository.findByNavident(lederForRessurs.lederNavident()).orElseThrow(LederNotFoundException::new)
                    ),
                    lederForRessurs.erDagligOppfolging(),
                    MappingUtils.toOrgEnhet(maybeOrgEnhet.get()),
                    lederForRessurs.startdato(),
                    lederForRessurs.sluttdato()
                );
            }).toList();
    }

    private List<LederForRessurs> getLedereForRessurs(String navident, boolean kunDOL) {
        var oter = orgtilknytningStateRepository.findByNavident(navident);
        if (kunDOL) {
            oter = oter.stream().filter(OrgtilknytningState::erDagligOppfolging).toList();
        }
        List<LederForRessurs> ledereForRessurs = new ArrayList<>();
        oter.forEach(ot -> {
            var lederForRessursPerOt = new ArrayList<LederForRessurs>();
            var lederStates = getLedereForOrgEnhetIPeriode(ot.orgEnhetId(), ot.gyldigFom(), ot.gyldigTom());
            lederStates.forEach(lederState -> {
                if (lederState.navident().equals(navident)) {
                    getLedereForOverOrgenheterSomIkkeErAngittNavident(ot.orgEnhetId(), lederState.startdato(), lederState.sluttdato(), navident).forEach(
                        overEnhetLederState -> lederForRessursPerOt.add(new LederForRessurs(
                                overEnhetLederState.navident(),
                                overEnhetLederState.orgEnhetId(),
                                DateUtil.foer(overEnhetLederState.startdato(),  ot.gyldigFom()) ? ot.gyldigFom() : overEnhetLederState.startdato(),
                                DateUtil.etter(overEnhetLederState.sluttdato(), ot.gyldigTom()) ? ot.gyldigTom() : overEnhetLederState.sluttdato(),
                                ot.erDagligOppfolging()
                            )
                        ));
                } else {
                    lederForRessursPerOt.add(new LederForRessurs(
                        lederState.navident(),
                        ot.orgEnhetId(),
                        DateUtil.foer(lederState.startdato(),  ot.gyldigFom()) ? ot.gyldigFom() : lederState.startdato(),
                        DateUtil.etter(lederState.sluttdato(), ot.gyldigTom()) ? ot.gyldigTom() : lederState.sluttdato(),
                        ot.erDagligOppfolging()
                    ));
                }
            });
            ledereForRessurs.addAll(ryddOverlappendePerioderForSammeEnhet(lederForRessursPerOt));
        });
        return ledereForRessurs.stream().sorted(lederForRessursComparator).toList();
    }

    private List<LederForRessurs> ryddOverlappendePerioderForSammeEnhet(List<LederForRessurs> ledereForRessurs) {
        List<LederForRessurs> ryddetLedereForRessurs = new ArrayList<>();
        ledereForRessurs.stream()
                .sorted(Comparator.comparing(LederForRessurs::startdato))
                .forEach(leder -> {
                    var min = ledereForRessurs.stream()
                        .filter(otherLeder ->
                            !Objects.equals(otherLeder.startdato(), leder.startdato())
                                && otherLeder.startdato().isAfter(leder.startdato())
                                && DateUtil.intervalCoversDate(otherLeder.startdato(), otherLeder.sluttdato(), leder.sluttdato())
                        )
                        .map(LederForRessurs::startdato)
                        .min(Comparator.naturalOrder());

                    if (min.isPresent() && !Objects.equals(min.get(), leder.sluttdato())) {
                        ryddetLedereForRessurs.add(new LederForRessurs(leder, min.get().minusDays(1)));
                    } else {
                        ryddetLedereForRessurs.add(leder);
                    }
                });
        return ryddetLedereForRessurs;
    }

//    private boolean isCovered(OrgEnhetLederState state, List<OrgEnhetLederState> lederStates) {
//        return lederStates.stream()
//            .anyMatch(otherState ->
//                !Objects.equals(otherState.fid(), state.fid())
//                    && PeriodeUtil.intervalCoversDate(otherState.startdato(), otherState.sluttdato(), state.startdato())
//                    && PeriodeUtil.intervalCoversDate(otherState.startdato(), otherState.sluttdato(), state.sluttdato())
//            );
//    }


    private List<OrgEnhetLederState> getLedereForOrgEnhetIPeriode(String orgEnhetId, LocalDate startdato, LocalDate sluttdato) {
        return orgEnhetLederStateRepository.findAllByOrgEnhetId(orgEnhetId).stream()
            .filter(lederState -> DateUtil.overlapsInterval(startdato, sluttdato, lederState.startdato(), lederState.sluttdato()))
            .sorted(orgEnhetLederStateComparator)
            .toList();
    }

    private List<OrgEnhetLederState> getLedereForOverOrgenheterSomIkkeErAngittNavident(String orgEnhetId, LocalDate startdato, LocalDate sluttdato, String navident) {
        List<OrgEnhetLederState> lederStates = new ArrayList<>();
        getLedereForOrgEnhetIPeriode(orgEnhetId, startdato, sluttdato).forEach(lederState -> {
            if (!lederState.navident().equals(navident)) {
                lederStates.add(
                    lederState.toBuilder()
                        .startdato(DateUtil.foer(lederState.startdato(), startdato) ? startdato : lederState.startdato())
                        .sluttdato(DateUtil.etter(lederState.sluttdato(), sluttdato) ? sluttdato : lederState.sluttdato())
                        .build()
                );
            } else {
                // Har enheten flere overordnede enheter i perioden (e.g. organiseringen har endret seg)?
                organiseringStateRepository.findById(orgEnhetId).stream()
                    .filter(organiseringState -> organiseringState.id().equals(orgEnhetId))
                    .filter(organiseringState -> DateUtil.overlapsInterval(startdato, sluttdato, organiseringState.gyldigFom(), organiseringState.gyldigTom()))
                    .forEach(organiseringState ->
                        lederStates.addAll(
                            getLedereForOverOrgenheterSomIkkeErAngittNavident(
                                organiseringState.idOver(),
                                DateUtil.foer(organiseringState.gyldigFom(), startdato) ? startdato : organiseringState.gyldigFom(),
                                DateUtil.etter(organiseringState.gyldigTom(), sluttdato) ? sluttdato : organiseringState.gyldigTom(),
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
        public LederForRessurs(LederForRessurs other, LocalDate newSluttdato) {
            this(other.lederNavident, other.orgEnhetId, other.startdato, newSluttdato, other.erDagligOppfolging);
        }
    }
}
