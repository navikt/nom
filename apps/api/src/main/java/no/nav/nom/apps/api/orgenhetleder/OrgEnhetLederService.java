package no.nav.nom.apps.api.orgenhetleder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederSluttdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.foer;
import static no.nav.nom.libs.utils.common.util.DateUtil.isActive;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgEnhetLederService {

    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final OrgEnhetLederCommandService orgEnhetLederCommandService;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrganiseringStateRepository organiseringStateRepository;

    public void persisterLedereForOrgenheter(List<DvhLeder> filteredDvhLedere) {
        Origination origination = new Origination("nom-api", "nom-api");
        InputMetadata inputMetadata = new InputMetadata("nom-api BatchJob", "persisterLedereForOrgenheter", null, origination);

        if (!filteredDvhLedere.isEmpty()) {
            filteredDvhLedere.forEach(dvhLedere -> {
                try {
                    var orgEnhetId = orgenhetService.getEnhetByAgressoIdNivaAndName(dvhLedere.getOrgEnhetNr(), dvhLedere.getOrgNiv(), dvhLedere.getOrgenhetNavn())
                        .orElseThrow(() -> OrgEnhetNotFoundException.agressoIdNivaaNavn(
                            dvhLedere.getOrgEnhetNr(),
                            dvhLedere.getOrgNiv(),
                            dvhLedere.getOrgenhetNavn()
                        )).getId();

                    var orgEnhetLedere = orgEnhetLederStateRepository.findAllByOrgEnhetId(orgEnhetId);
                    inputMetadata.toBuilder().inputInterfaceMethodArguments(dvhLedere).build();

                    if (orgEnhetLedere.isEmpty()) {
                        log.info("Fant ingen ledere for orgenhet, lagrer {} for orgenhet {}", dvhLedere.getNavIdent(), orgEnhetId);
                        var fid = orgEnhetLederCommandService.opprettOrgEnhetLeder(new OpprettOrgEnhetLeder(
                            dvhLedere.getNavIdent(),
                            orgEnhetId,
                            inputMetadata
                        ));
                        orgEnhetLederCommandService.oppdaterOrgEnhetLederStartdato(fid, new OppdaterOrgEnhetLederStartdato(LocalDate.now(), inputMetadata));
                    } else {
                        var aktivLedere = orgEnhetLedere.stream()
                            .filter(orgEnhetLederState -> isActive(orgEnhetLederState.startdato(), orgEnhetLederState.sluttdato()))
                            .collect(Collectors.toList());

                        log.info("Fant {} ledere for orgenheten {}", aktivLedere.size(), orgEnhetId);

                        var oneOfCurrentLeadersMatchDvhLeader = aktivLedere.stream()
                            .filter(orgEnhetLederState -> orgEnhetLederState.navident().equals(dvhLedere.getNavIdent()))
                            .findFirst();

                        if (oneOfCurrentLeadersMatchDvhLeader.isPresent()) {
                            log.info("Fant matchende aktiv leder i DVH og NOM: {}", oneOfCurrentLeadersMatchDvhLeader.get());
                            aktivLedere.remove(oneOfCurrentLeadersMatchDvhLeader.get());
                        } else {
                            log.info("Fant ingen matchende leder i NOM, lagrer {}", dvhLedere.getNavIdent());
                            var newFid = orgEnhetLederCommandService.opprettOrgEnhetLeder(new OpprettOrgEnhetLeder(
                                dvhLedere.getNavIdent(),
                                orgEnhetId,
                                inputMetadata
                            ));
                            orgEnhetLederCommandService.oppdaterOrgEnhetLederStartdato(
                                newFid,
                                new OppdaterOrgEnhetLederStartdato(LocalDate.now(), inputMetadata)
                            );
                        }
                        aktivLedere.forEach(orgEnhetLederState -> {
                            log.info("Avslutter {} som leder i OrgEnhet {}", orgEnhetLederState.navident(), orgEnhetLederState.orgEnhetId());
                            var fid = orgEnhetLederState.fid();
                            if (isNull(orgEnhetLederState.startdato())) {
                                orgEnhetLederCommandService.oppdaterOrgEnhetLederStartdato(
                                    fid,
                                    new OppdaterOrgEnhetLederStartdato(LocalDate.now().minusDays(2), inputMetadata)
                                );
                            }
                            var sluttdato = orgEnhetLederState.startdato() != null && !orgEnhetLederState.startdato().isAfter(LocalDate.now()) ? LocalDate.now() : LocalDate.now().minusDays(1);
                            orgEnhetLederCommandService.oppdaterOrgEnhetLederSluttdato(
                                fid,
                                new OppdaterOrgEnhetLederSluttdato(sluttdato, inputMetadata)
                            );
                        });
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            });

            Map<String, List<DvhLeder>> dvhLedereForNavident = filteredDvhLedere.stream().collect(Collectors.groupingBy(DvhLeder::getNavIdent));
            Map<String, Map<String, OrgEnhetLederState>> orgEnhetLederStates = getAktiveOrgEnhetLederStateMappedByNavidentAndOrgEnhetId();

            log.info("Starter å rydde i orgEnhetLederState");
            orgEnhetLederStates.forEach((navident, orgEnhetLederStateByNavident) -> {
                log.info("{} er leder i {} enheter", navident, orgEnhetLederStateByNavident.size());
                var dvhLederByNavident = dvhLedereForNavident.get(navident);
                if (isNull(dvhLederByNavident) || dvhLederByNavident.isEmpty()) {
                    log.info("{} er ikke leder i Unit4, alle aktive stater blir avsluttet hos oss", navident);
                    orgEnhetLederStateByNavident.values().forEach(orgEnhetLederState -> orgEnhetLederCommandService.oppdaterOrgEnhetLederSluttdato(
                        orgEnhetLederState.fid(),
                        new OppdaterOrgEnhetLederSluttdato(LocalDate.now().minusDays(1), inputMetadata)
                    ));
                    return;
                }

                var copyOforgEnhetLederStateByNavident = new HashMap<>(orgEnhetLederStateByNavident);

                dvhLederByNavident.forEach(dvhLeder -> {
                    var orgEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
                        dvhLeder.getOrgEnhetNr(),
                        dvhLeder.getOrgNiv(),
                        dvhLeder.getOrgenhetNavn()
                    );
                    orgEnhet.ifPresent(iOrgenhet -> copyOforgEnhetLederStateByNavident.remove(iOrgenhet.getId()));
                });

                copyOforgEnhetLederStateByNavident.forEach((orgEnhetId, orgEnhetLederState) -> {
                    log.info("{} er ikke leder for {} lenger, staten blir avsluttet", navident, orgEnhetId);
                    orgEnhetLederCommandService.oppdaterOrgEnhetLederSluttdato(
                        orgEnhetLederState.fid(),
                        new OppdaterOrgEnhetLederSluttdato(LocalDate.now().minusDays(1), inputMetadata)
                    );
                });
            });
        } else {
            log.warn("Ingen ledere funnet fra DVH");
        }
    }

    public List<LederForRessurs> getLedereForRessurs(String navident, boolean kunDOL) {
        var oter = orgtilknytningService.getByNavIdent(navident);
        if (kunDOL) {
            oter = oter.stream().filter(OrgtilknytningState::erDagligOppfolging).toList();
        }
        List<LederForRessurs> lederForRessurs = new ArrayList<>();
        oter.forEach(ot -> {
            var lederStates = getLedereForOrgEnhetIPeriode(ot.orgEnhetId(), ot.gyldigFom(), ot.gyldigTom());
            lederStates.forEach(lederState -> {
                if (lederState.navident().equals(navident)) {
                    var periodeStart = foer(lederState.startdato(), ot.gyldigFom()) ? ot.gyldigFom() : lederState.startdato();
                    var periodeSlutt = etter(lederState.sluttdato(), ot.gyldigTom()) ? ot.gyldigTom() : lederState.sluttdato();

                    getLedereForOverOrgenheterSomIkkeErAngittNavident(ot.orgEnhetId(), periodeStart, periodeSlutt, navident).forEach(
                        overEnhetLederState -> lederForRessurs.add(new LederForRessurs(
                            overEnhetLederState.navident(),
                            overEnhetLederState.orgEnhetId(),
                            foer(overEnhetLederState.startdato(), periodeStart) ? periodeStart : overEnhetLederState.startdato(),
                            etter(overEnhetLederState.sluttdato(), periodeSlutt) ? periodeSlutt : overEnhetLederState.sluttdato(),
                            ot.erDagligOppfolging()
                        )
                    ));
                } else {
                    lederForRessurs.add(new LederForRessurs(
                        lederState.navident(),
                        ot.orgEnhetId(),
                        foer(lederState.startdato(), ot.gyldigFom()) ? ot.gyldigFom() : lederState.startdato(),
                        etter(lederState.sluttdato(), ot.gyldigTom()) ? ot.gyldigTom() : lederState.sluttdato(),
                        ot.erDagligOppfolging()
                    ));
                }
            });
        });
        return lederForRessurs.stream().sorted(lederForRessursComparator).toList();
    }

    private List<OrgEnhetLederState> getLedereForOverOrgenheterSomIkkeErAngittNavident(String orgEnhetId, LocalDate startdato, LocalDate sluttdato, String navident) {
        List<OrgEnhetLederState> lederStates = new ArrayList<>();
        getLedereForOrgEnhetIPeriode(orgEnhetId, startdato, sluttdato).forEach(lederState -> {
            if (!lederState.navident().equals(navident)) {
                lederStates.add(
                    lederState.toBuilder()
                        .startdato(foer(lederState.startdato(), startdato) ? startdato : lederState.startdato())
                        .sluttdato(etter(lederState.sluttdato(), sluttdato) ? sluttdato : lederState.sluttdato())
                        .build()
                );
            } else {
                var delperiodeStart = foer(lederState.startdato(), startdato) ? startdato : lederState.startdato();
                var delperiodeSlutt = etter(lederState.sluttdato(), sluttdato) ? sluttdato : lederState.sluttdato();
                // Har enheten flere overordnede enheter i perioden (e.g. organiseringen har endret seg)?
                organiseringStateRepository.findById(orgEnhetId).stream()
                    .filter(organiseringState -> organiseringState.id().equals(orgEnhetId))
                    .filter(organiseringState -> PeriodeUtil.overlapsInterval(delperiodeStart, delperiodeSlutt, organiseringState))
                    .forEach(organiseringState ->
                        lederStates.addAll(
                            getLedereForOverOrgenheterSomIkkeErAngittNavident(
                                organiseringState.idOver(),
                                foer(organiseringState.gyldigFom(), delperiodeStart) ? delperiodeStart : organiseringState.gyldigFom(),
                                etter(organiseringState.gyldigTom(), delperiodeSlutt) ? delperiodeSlutt : organiseringState.gyldigTom(),
                                navident
                            )
                        )
                    );
            }
        });
        return lederStates.stream().sorted(orgEnhetLederStateComparator).toList();
    }

    public List<LederForRessurs> getAktiveLedereForRessurs(String navident, boolean kunDOL) {
        return getLedereForRessurs(navident, kunDOL).stream()
            .filter(LederForRessurs::isActive)
            .sorted(lederForRessursComparator)
            .toList();
    }

    public List<IOrgenhet> getEnheterByLeder(String lederNavIdent) {
        return orgEnhetLederStateRepository.findAllByNavident(lederNavIdent).stream()
            .map(leder -> orgenhetService.getById(leder.orgEnhetId()))
            .flatMap(Optional::stream)
            .toList();
    }


    public List<OrgEnhetLederState> getLedereForOrgEnhetIPeriode(String orgEnhetId, LocalDate startdato, LocalDate sluttdato) {
        return orgEnhetLederStateRepository.findAllByOrgEnhetId(orgEnhetId).stream()
            .filter(lederState -> PeriodeUtil.overlapsInterval(startdato, sluttdato, lederState.startdato(), lederState.sluttdato()))
            .sorted(orgEnhetLederStateComparator)
            .toList();
    }

    public List<OrgEnhetLederState> getOrgEnheterNavidentErLederFor(String navident) {
        return orgEnhetLederStateRepository.findAllByNavident(navident).stream()
            .sorted(orgEnhetLederStateComparator)
            .toList();
    }

    public List<OrgEnhetLederState> getOrgEnheterNavidentErAktivLederFor(String navident) {
        return getOrgEnheterNavidentErLederFor(navident).stream()
            .filter(OrgEnhetLederState::isActive)
            .toList();
    }

    public List<OrgEnhetLederState> getLedereForOrgEnhet(String orgEnhetId) {
        return orgEnhetLederStateRepository.findAllByOrgEnhetId(orgEnhetId).stream()
            .sorted(orgEnhetLederStateComparator)
            .toList();
    }

    public Optional<OrgEnhetLederState> getAktivLederForOrgEnhet(String orgEnhetId) {
        return orgEnhetLederStateRepository.getActiveForOrgEnhet(orgEnhetId);
    }

    public Map<String, Map<String, OrgEnhetLederState>> getAktiveOrgEnhetLederStateMappedByNavidentAndOrgEnhetId() {
        Map<String, Map<String, OrgEnhetLederState>> orgEnhetLederStates = new HashMap<>();
        orgEnhetLederStateRepository.getOrgEnhetLederByNavidentMap().forEach((navident, orgEnhetLederStateMap) -> {
            var navidentMap = orgEnhetLederStates.computeIfAbsent(navident, k -> new HashMap<>());
            orgEnhetLederStateMap
                .forEach((fid, orgEnhetLederState) -> {
                    if (orgEnhetLederState.isActive()) {
                        navidentMap.put(orgEnhetLederState.orgEnhetId(), orgEnhetLederState);
                    }
                });
        });
        orgEnhetLederStates.entrySet().removeIf(entrySet -> entrySet.getValue().isEmpty());
        log.info("Antall aktive ledere: {}", orgEnhetLederStates.size());
        return orgEnhetLederStates;
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
}
