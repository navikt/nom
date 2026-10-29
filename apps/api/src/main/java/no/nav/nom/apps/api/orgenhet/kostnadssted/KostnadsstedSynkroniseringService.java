package no.nav.nom.apps.api.orgenhet.kostnadssted;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.koststed.KostnadsstedMappingResponseDTO;
import no.nav.nom.apps.api.clients.koststed.OrgEnhetKostnadsstedMappingClient;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreKostnadssted;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.isNull;
import static no.nav.nom.libs.utils.common.util.DateUtils.isPeriodActive;

/**
 * Henter jevnlig orgenhet-kostnadssted-mappinger fra OrgKostMapping, sammenligner
 * med kostnadssted lagret i {@link OrgEnhetState} og videreformidler endringer
 * som {@code KostnadsstedEndretEventV1}.
 */
@Slf4j
@Service
public class KostnadsstedSynkroniseringService {

    private final OrgEnhetKostnadsstedMappingClient kostnadsstedMappingClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final LeaderElection leaderElection;

    public KostnadsstedSynkroniseringService(
        OrgEnhetKostnadsstedMappingClient kostnadsstedMappingClient,
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgEnhetCommandService orgEnhetCommandService,
        LeaderElection leaderElection
    ) {
        this.kostnadsstedMappingClient = kostnadsstedMappingClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.leaderElection = leaderElection;
    }

    public record KostnadsstedEndring(String orgEnhetFid, String orgEnhetId, String gammeltKostnadsstedId, String nyttKostnadsstedId) {
    }

    @Scheduled(cron = "${kostnadssted.synk.cron:0 30 2 * * *}")
    public void synkroniserKostnadsstederScheduler() {
        if (leaderElection.isLeader()) {
            try {
                synkroniserKostnadssteder();
            } catch (Exception e) {
                log.error("Feil ved synkronisering av kostnadssteder fra OrgKostMapping", e);
            }
        }
    }

    public List<KostnadsstedEndring> synkroniserKostnadssteder() {
        var endringer = finnEndringer();
        if (endringer.isEmpty()) {
            log.info("Kostnadssted-synk: ingen endringer funnet");
            return endringer;
        }

        InputMetadata inputMetadata = InputMetadata.builder()
            .inputInterfaceName("nom-api BatchJob")
            .inputInterfaceMethodName("synkroniserKostnadssteder")
            .origination(new Origination("nom-api", "nom-api"))
            .build();

        endringer.forEach(endring -> {
            log.info("Kostnadssted-synk: orgenhet {} endrer kostnadssted fra {} til {}",
                endring.orgEnhetId(), endring.gammeltKostnadsstedId(), endring.nyttKostnadsstedId());
            orgEnhetCommandService.endreKostnadssted(
                endring.orgEnhetFid(),
                new EndreKostnadssted(endring.orgEnhetId(), endring.nyttKostnadsstedId(), inputMetadata)
            );
        });
        log.info("Kostnadssted-synk: {} endringer videreformidlet", endringer.size());
        return endringer;
    }

    public List<KostnadsstedEndring> finnEndringer() {
        var alleOrgEnheter = orgEnhetStateRepository.getAll();
        var aktiveMappinger = hentAktiveMappinger(alleOrgEnheter);

        List<KostnadsstedEndring> endringer = new ArrayList<>();
        alleOrgEnheter.stream()
            .filter(orgEnhet -> isPeriodActive(orgEnhet.gyldigFom(), orgEnhet.gyldigTom()))
            .forEach(orgEnhet -> {
                var mapping = aktiveMappinger.get(orgEnhet.id());
                // Mangler mapping betyr ikke nødvendigvis at kostnadssted skal fjernes; vi rører ikke disse.
                if (mapping == null || isNull(mapping.kostnadsstedId())) {
                    return;
                }
                if (!Objects.equals(mapping.kostnadsstedId(), orgEnhet.kostnadsstedId())) {
                    endringer.add(new KostnadsstedEndring(
                        orgEnhet.fid(), orgEnhet.id(), orgEnhet.kostnadsstedId(), mapping.kostnadsstedId()));
                }
            });
        return endringer;
    }

    private Map<String, KostnadsstedMappingResponseDTO> hentAktiveMappinger(Iterable<OrgEnhetState> alleOrgEnheter) {
        Map<String, KostnadsstedMappingResponseDTO> mappinger = new HashMap<>();
        for (var toppnode : alleOrgEnheter) {
            if (!isNull(toppnode.overordnetOrgEnhetFid()) || !isPeriodActive(toppnode.gyldigFom(), toppnode.gyldigTom())) {
                continue;
            }
            var resultat = kostnadsstedMappingClient.hentKostnadsstedMappingerForGittToppNode(toppnode.id());
            if (resultat == null) {
                continue;
            }
            resultat.stream()
                .filter(m -> m.orgenhetId() != null)
                .filter(m -> isPeriodActive(m.gyldigFom(), m.gyldigTom()))
                .forEach(m -> mappinger.put(m.orgenhetId(), m));
        }
        return mappinger;
    }
}

