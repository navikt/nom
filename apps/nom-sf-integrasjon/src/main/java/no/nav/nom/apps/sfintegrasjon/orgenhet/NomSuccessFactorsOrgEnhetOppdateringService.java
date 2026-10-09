package no.nav.nom.apps.sfintegrasjon.orgenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.behandling.BehandlingService;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.Behandling;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelse;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelseRepository;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.Leder;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.OverOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.repository.SfOutboxRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.common.StateNotFoundException;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.nom.libs.utils.common.util.DateUtils.isPeriodActive;

@Slf4j
@Service
public class NomSuccessFactorsOrgEnhetOppdateringService {

    private final OrganiseringStateRepository organiseringStateRepository;
    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrganisasjonHendelseRepository organisasjonHendelserRepository;
    private final SfOutboxRepository sfOutboxRepository;

    private final BehandlingService behandlingService;
    private final NomApiClient nomApiClient;

    private final NomStateOrganiseringListener nomStateOrganiseringListener;
    private final NomStateOrgEnhetListener nomStateOrgEnhetListener;
    private final NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener;

    private final LeaderElection leaderElection;

    private final ObjectMapper objectMapper;


    public NomSuccessFactorsOrgEnhetOppdateringService(
        OrganiseringStateRepository organiseringStateRepository,
        OrgEnhetLederStateRepository orgEnhetLederStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrganisasjonHendelseRepository organisasjonHendelserRepository,
        SfOutboxRepository sfOutboxRepository,
        BehandlingService behandlingService,
        NomApiClient nomApiClient,
        NomStateOrganiseringListener nomStateOrganiseringListener,
        NomStateOrgEnhetListener nomStateOrgEnhetListener,
        NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener,
        LeaderElection leaderElection,
        ObjectMapper objectMapper
    ) {
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.organisasjonHendelserRepository = organisasjonHendelserRepository;
        this.sfOutboxRepository = sfOutboxRepository;
        this.behandlingService = behandlingService;
        this.nomApiClient = nomApiClient;
        this.nomStateOrganiseringListener = nomStateOrganiseringListener;
        this.nomStateOrgEnhetListener = nomStateOrgEnhetListener;
        this.nomStateOrgEnhetLederListener = nomStateOrgEnhetLederListener;
        this.leaderElection = leaderElection;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 60000)
    public void oppdaterSuccessFactors() {
        if (leaderElection.isLeader() && isInitialStateReady()) {
            var unprocessedBehandlinger = behandlingService.getLatestNonProcessedBehandlingObjects()
                .stream()
                .collect(Collectors.toMap(Behandling::getBehandlingsId, behandling -> behandling, (b1, _) -> b1));
            var unprocessedBehandlingerId = unprocessedBehandlinger.keySet().stream().toList();

            var unprocessedOrganisasjonsHendelser = organisasjonHendelserRepository.findAllGroupedByOrgEnhetId(unprocessedBehandlingerId);

            unprocessedOrganisasjonsHendelser.forEach((orgEnhetId, organisasjonsHendelser) -> {
                var behandlingsIds = organisasjonsHendelser.stream().map(OrganisasjonHendelse::getBehandlingsId).toList();

                try {
                    var organiseringerState = organiseringStateRepository.findById(orgEnhetId);
                    var orgEnhet = orgEnhetStateRepository.findById(orgEnhetId)
                        .orElseThrow(() -> new StateNotFoundException(String.format("Fant ikke orgenhet med id %s", orgEnhetId)));
                    var orgEnhetLedereState = orgEnhetLederStateRepository.findAllByOrgEnhetId(orgEnhetId);

                    var endringshendelser = organisasjonsHendelser.stream().map(OrganisasjonHendelse::getEventType).collect(Collectors.toSet());

                    var kostnadsstedForOrgEnhet = nomApiClient.getKostnadsstedForOrgEnhet(orgEnhetId);

                    var endretAv = organisasjonsHendelser.stream()
                        .map(OrganisasjonHendelse::getMelding)
                        .map(eventMelding -> objectMapper.readTree(eventMelding)
                            .requiredAt("/metadata/inputMetadata/origination/initiatedBy")
                            .requireNonNull()
                            .asString())
                        .collect(Collectors.toSet());

                    var ledere = orgEnhetLedereState.stream().map(orgEnhetLederState -> new Leder(
                        orgEnhetLederState.navident(),
                        orgEnhetLederState.startdato(),
                        orgEnhetLederState.sluttdato()
                    )).toList();

                    List<OverOrgEnhet> overOrgEnheter = new ArrayList<>();
                    if (!Objects.equals(orgEnhet.nomNivaa(), NomNivaa.LINJEENHET)) {
                        overOrgEnheter.addAll(organiseringerState.stream()
                            .filter(organiseringState -> !Objects.equals(organiseringState.gyldigFom(), organiseringState.gyldigTom()))
                            .filter(organiseringState -> nonNull(organiseringState.idOver()))
                            // Datoene er perioden enheten lå under overenheten, ikke overenhetens egne datoer
                            .flatMap(organiseringState -> orgEnhetStateRepository.findById(organiseringState.idOver()).stream()
                                .map(overOrgEnhetState -> new OverOrgEnhet(
                                    overOrgEnhetState.id(),
                                    overOrgEnhetState.nomNivaa(),
                                    organiseringState.gyldigFom(),
                                    organiseringState.gyldigTom()
                                )))
                            .toList());
                    }

                    checkForNomNivaaAndFetchIfMissing(overOrgEnheter);

                    var nivaa = finnNivaa(orgEnhet);

                    var sfOrgEnhet = new SfOrgEnhet(
                        orgEnhet.id(),
                        orgEnhet.navn(),
                        nivaa,
                        erLovnode(orgEnhet.id()),
                        orgEnhet.orgEnhetsType(),
                        orgEnhet.tilgangsenhetId(),
                        kostnadsstedForOrgEnhet.map(OrgEnhetKostnadsstedMappingResponse::kostnadsstedId).orElse(""),
                        orgEnhet.gyldigFom(),
                        orgEnhet.gyldigTom(),
                        ledere,
                        overOrgEnheter
                    );

                    var outboxSporingsId = sfOutboxRepository.save(SfOutbox.builder()
                        .orgEnhetId(orgEnhetId)
                        .endretAv(endretAv)
                        .endringshendelser(endringshendelser)
                        .orgenhet(objectMapper.writeValueAsString(sfOrgEnhet))
                        .build()).getSporingsId();

                    behandlingService.oppdaterStatusTilSynket(behandlingsIds, outboxSporingsId);
                } catch (Exception e) {
                    behandlingService.oppdaterStatusTilFeilet(behandlingsIds);
                    log.error(e.getMessage(), e);
                }
            });
        }
    }

    private NomNivaa finnNivaa(OrgEnhetState orgEnhet) {
        if (nonNull(orgEnhet.nomNivaa())) {
            return orgEnhet.nomNivaa();
        }

        return isPeriodActive(orgEnhet.gyldigFom(), orgEnhet.gyldigTom())
            ? findActiveNomNivaaForOverOrgEnhet(orgEnhet.id())
            : findNewestNomNivaaForOverOrgEnhet(orgEnhet.id());
    }

    // Samme regel som NOM bruker for hvor ansatte kan plasseres: ingen aktive underenheter
    private boolean erLovnode(String orgEnhetId) {
        return !organiseringStateRepository.orgEnhetHasChildren(orgEnhetId);
    }

    private void checkForNomNivaaAndFetchIfMissing(List<OverOrgEnhet> overOrgEnheter) {
        overOrgEnheter.replaceAll(overOrgEnhet -> {
            if (nonNull(overOrgEnhet.nivaa())) {
                return overOrgEnhet;
            }
            var nivaa = isPeriodActive(overOrgEnhet.gyldigFom(), overOrgEnhet.gyldigTom())
                ? findActiveNomNivaaForOverOrgEnhet(overOrgEnhet.id())
                : findNewestNomNivaaForOverOrgEnhet(overOrgEnhet.id());
            return overOrgEnhet.toBuilder().nivaa(nivaa).build();
        });
    }

    private NomNivaa findNewestNomNivaaForOverOrgEnhet(String orgEnhetId) {
        var organiseringer = organiseringStateRepository.findById(orgEnhetId)
            .stream()
            .filter(organisering -> !Objects.equals(organisering.gyldigFom(), organisering.gyldigTom()))
            .toList();

        var nyesteOrganisering = organiseringer.stream().filter(organisering -> !organisering.gyldigFom().isAfter(LocalDate.now()))
            .max(Comparator.comparing(OrganiseringState::gyldigFom))
            .or(() -> organiseringer.stream().min(Comparator.comparing(OrganiseringState::gyldigFom)))
            .orElseThrow(() -> new StateNotFoundException(String.format("OrgEnhet med id %s har ingen organiseringer", orgEnhetId)));

        var orgEnhetState = orgEnhetStateRepository.findById(nyesteOrganisering.idOver())
            .orElseThrow(() -> new StateNotFoundException(String.format("OrgEnhet med id %s finnes ikke", nyesteOrganisering.idOver())));
        if (isNull(orgEnhetState.nomNivaa())) {
            return findNewestNomNivaaForOverOrgEnhet(orgEnhetState.id());
        }
        return orgEnhetState.nomNivaa();
    }

    private NomNivaa findActiveNomNivaaForOverOrgEnhet(String orgEnhetId) {
        var organiseringState = organiseringStateRepository.findActiveById(orgEnhetId)
            .orElseThrow(() -> new StateNotFoundException(String.format("Organisering %s finnes ikke eller har ingen aktive organisering", orgEnhetId)));
        var orgEnhetState = orgEnhetStateRepository.findById(organiseringState.idOver())
            .orElseThrow(() -> new StateNotFoundException(String.format("OrgEnhet med id %s finnes ikke", orgEnhetId)));
        if (isNull(orgEnhetState.nomNivaa())) {
            return findActiveNomNivaaForOverOrgEnhet(orgEnhetState.id());
        }
        return orgEnhetState.nomNivaa();
    }

    private boolean isInitialStateReady() {
        return nomStateOrganiseringListener.hasReadToEndOnStartup()
            && nomStateOrgEnhetListener.hasReadToEndOnStartup()
            && nomStateOrgEnhetLederListener.hasReadToEndOnStartup();
    }
}
