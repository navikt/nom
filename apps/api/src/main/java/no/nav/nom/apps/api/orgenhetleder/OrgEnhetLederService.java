package no.nav.nom.apps.api.orgenhetleder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.api.app.OrgEnhetLederResponse;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederSluttdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.nav.nom.libs.utils.common.util.DateUtil.isActive;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgEnhetLederService {

    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetLederCommandService orgEnhetLederCommandService;
    private final OrgenhetService orgenhetService;


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
            Map<String, Map<String, OrgEnhetLederState>> orgEnhetLederStates = orgEnhetLederStateRepository.getAllOrgEnhetLederStateMappedByNavidentAndOrgEnhetId();

            log.info("Starter å rydde i orgEnhetLederState");
            orgEnhetLederStates.forEach((navident, orgEnhetLederStateByNavident) -> {
                log.info("{} er leder i {} enheter", navident, orgEnhetLederStateByNavident.size());
                var dvhLederByNavident = dvhLedereForNavident.get(navident);
                if (isNull(dvhLederByNavident) || dvhLederByNavident.isEmpty()) {
                    log.info("{} er ikke leder i Unit4, alle blir avsluttet hos oss", navident);
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

    public List<OrgEnhetLederResponse> getLederByNavident(String navident) {
        return orgEnhetLederStateRepository.findAllByNavident(navident).stream().map(this::enrichWithData).toList();
    }

    public List<OrgEnhetLederResponse> getLederByOrgEnhetId(String orgEnhetId) {
        return orgEnhetLederStateRepository.findAllByOrgEnhetId(orgEnhetId).stream().map(this::enrichWithData).toList();
    }

    public Optional<OrgEnhetLederResponse> getActivLederForOrgEnhet(String orgEnhetId) {
        return orgEnhetLederStateRepository.getActiveForOrgEnhet(orgEnhetId).map(this::enrichWithData);
    }

    public OrgEnhetLederResponse enrichWithData(OrgEnhetLederState orgEnhetLederState) {
        var ressursVisningsnavn = ressursStateRepository.findByNavident(orgEnhetLederState.navident()).map(RessursState::visningsnavn).orElse(null);
        var orgEnhetNavn = orgenhetService.getById(orgEnhetLederState.orgEnhetId()).map(IOrgenhet::getNavn).orElse(null);

        return OrgEnhetLederResponse.builder()
            .fid(orgEnhetLederState.fid())
            .navident(orgEnhetLederState.navident())
            .visningsnavn(ressursVisningsnavn)
            .orgEnhetId(orgEnhetLederState.orgEnhetId())
            .orgEnhetNavn(orgEnhetNavn)
            .startdato(orgEnhetLederState.startdato())
            .sluttdato(orgEnhetLederState.sluttdato())
            .build();
    }
}
