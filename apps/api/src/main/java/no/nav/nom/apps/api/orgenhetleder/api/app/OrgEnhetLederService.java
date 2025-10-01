package no.nav.nom.apps.api.orgenhetleder.api.app;

import io.getunleash.Unleash;
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
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
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
    private final Unleash unleash;
    private final OrgEnhetLederCommandService orgEnhetLederCommandService;
    private final LeaderElection leaderElection;
    private final OrgenhetService orgenhetService;


    public void persisterLedereForOrgenheter(List<DvhLeder> filteredLedere) {
        if (!unleash.isEnabled("nom.persistere-ledere")) return;
        Origination origination = new Origination("nom-api", "nom-api");
        InputMetadata inputMetadata = new InputMetadata("nom-api BatchJob", "persisterLedereForOrgenheter", null, origination);

        if (leaderElection.isLeader() && !filteredLedere.isEmpty()) {
            filteredLedere.forEach(dvhLedere -> {
                var orgEnhetId = orgenhetService.getEnhetByAgressoIdNivaAndName(dvhLedere.getOrgEnhetNr(), dvhLedere.getOrgNiv(), dvhLedere.getOrgenhetNavn())
                    .orElseThrow(() -> OrgEnhetNotFoundException.agressoIdNivaaNavn(
                        dvhLedere.getOrgEnhetNr(),
                        dvhLedere.getOrgNiv(),
                        dvhLedere.getOrgenhetNavn()
                )).getId();

                var orgEnhetLedere = orgEnhetLederStateRepository.findByOrgEnhetId(orgEnhetId);
                inputMetadata.toBuilder().inputInterfaceMethodArguments(dvhLedere).build();

                if (orgEnhetLedere.isEmpty()) {
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

                    var oneOfCurrentLeadersMatchDvhLeader = aktivLedere.stream()
                        .filter(orgEnhetLederState -> orgEnhetLederState.navident().equals(dvhLedere.getNavIdent()))
                        .findFirst();

                    if (oneOfCurrentLeadersMatchDvhLeader.isPresent()) {
                        aktivLedere.remove(oneOfCurrentLeadersMatchDvhLeader.get());
                    } else {
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
                        var fid = orgEnhetLederState.fid();
                        if (isNull(orgEnhetLederState.startdato())) {
                            orgEnhetLederCommandService.oppdaterOrgEnhetLederStartdato(
                                fid,
                                new OppdaterOrgEnhetLederStartdato(LocalDate.now().minusDays(2), inputMetadata)
                            );
                        }
                        orgEnhetLederCommandService.oppdaterOrgEnhetLederSluttdato(
                            fid,
                            new OppdaterOrgEnhetLederSluttdato(LocalDate.now().minusDays(1), inputMetadata)
                        );
                    });
                }
            });
        }
    }

    public List<OrgEnhetLederResponse> getLederByNavident(String navident) {
        return orgEnhetLederStateRepository.findByNavident(navident).stream().map(this::enrichWithData).toList();
    }

    public List<OrgEnhetLederResponse> getLederByOrgEnhetId(String orgEnhetId) {
        return orgEnhetLederStateRepository.findByOrgEnhetId(orgEnhetId).stream().map(this::enrichWithData).toList();
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
