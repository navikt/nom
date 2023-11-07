package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.EndreOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateListener;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateListener;
import no.nav.nom.apps.api.ressurs.RessursHendelseForEventsourceService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.ANNEN_STATLIG;

@Slf4j
@Component
@RequiredArgsConstructor
public class DvhRestConsumer {
    public static final String CACHE = "cache-source-dvh";
    private final long LOAD_INTERVAL = 30 * 60 * 1000; // sync hvert 30min
    private final DvhRepository dvhRepository;
    private final DvhService dvhService;
    private final NomProxyClient nomProxyClient;
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final RessursStateListener ressursStateListener;
    private final OrgtilknytningStateListener orgtilknytningStateListener;
    private final OrgEnhetStateListener orgEnhetStateListener;
    private final RessursHendelseForEventsourceService ressursHendelseForEventsourceService;
    private final LeaderElection leaderElection;
    private final DvhOrgenhetsLast dvhOrgenhetsLast;
    private final OrgEnhetTypeHandler orgEnhetTypeHandler;

    private final RessursStillingLoader ressursStillingLoader;
    private final EksterneRessurserLoader eksterneRessurserLoader;

    private final OrganiseringEventCommandService organiseringEventCommandService;

    private final OrgenhetService orgenhetService;
    private final OrganiseringStateRepository organiseringStateRepository;

    public boolean initloadComplete() {
        return initialLoadComplete.get();
    }


    @Scheduled(fixedDelay = LOAD_INTERVAL) // Run on startup and every 30 min
    public void scheduledOppdateringFraDvh() {
        log.info("Starter schedulert oppdatering fra DVH");
        LocalDateTime start = LocalDateTime.now();

        //        TODO Remove before merge!
        //        while ( !isInitialStateReady() ) { // Wait on startup until state topics are read
        //            try {
        //                Thread.sleep(5_000);
        //                log.debug("InitialState er ikke klar enda");
        //                log.debug("Ressurs state: {}  Orgtilkn. state: {}  Orgenhet state: {}", ressursStateListener.initloadComplete(), orgtilknytningStateListener.initloadComplete(), orgEnhetStateListener.initloadComplete());
        //            } catch (Exception ignored) {
        //                log.error("Waiting for initialState last feilet", ignored);
        //            }
        //        }

        log.info("State topics er lest, fortsetter schedulert oppdatering fra DVH etter {}", durationAsH24(start));

        loadLedereAndOrganiseringerFromDvh();
        loadTlfFromDvh();


        if (!initialLoadComplete.get()) { // Pod kan markeres som klar etter oppstart
            initialLoadComplete.set(true);
            log.info("Initial oppdatering fra DVH er klar. Etter {}", durationAsH24(start));
        }

        if (leaderElection.isLeader()) { // Oppdater state fra dvh - holder at en Pod gjør dette
            updateOrgtilknytningerStateFromDvh();

            updateOrgtilknytningerForEksterneFromDvh();

            loadRessurserFromDvh();

            dvhOrgenhetsLast.lastOrgenhetsDataFraDvhTilEvtsrc();

            orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();
        }

        log.info("Ferdig med schedulert oppdatering fra DVH. Tok {}", durationAsH24(start));
    }

    public void loadRessurserOgOrgEnheter() {
        if (leaderElection.isLeader()) {
            loadRessurserFromDvh();

            dvhOrgenhetsLast.lastOrgenhetsDataFraDvhTilEvtsrc();
        }
    }

    @SneakyThrows
    public void loadRessurserFromDvh() {
        log.info("Laster ressurser fra dvh");
        try {
            var jsonRessurser = dvhExchange("/proxy/dvh/json/Hr/NOM3/NOM_ANSATTE_BUCKETS_v2");

            val dvhRessurs = StreamSupport.stream(jsonRessurser.get("NOM_ANSATTE_BUCKETS_v2Response").spliterator(), false)
                .map(DvhRessurs::new)
                .distinct()
                .collect(Collectors.groupingBy(DvhRessurs::getPersonIdent))
                .values()
                .stream()
                .peek(l -> {
                    if (l.size() > 1) {
                        l.removeAll(l.stream().filter(Predicate.not(DvhRessurs::getRemedyAktiv)).toList());
                        if (l.size() > 1) {
                            val personIdent = l.get(0).getPersonIdent();
                            val navIdenter = l.stream().map(DvhRessurs::getNavIdent).toList();
                            log.warn(SECURE_LOGS, "Forkastet lesing av ansatt med personident " + personIdent + " har flere aktive navidenter: " + navIdenter);
                            l.clear();
                        }
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(DvhRessurs::getNavIdent))
                .values()
                .stream()
                .filter(l -> l.size() == 1)
                .flatMap(Collection::stream)
                .toList();

            ressursHendelseForEventsourceService.handterRessursIderFraDvh(dvhRessurs);
            dvhRepository.saveAnnenStatNavIdenter(findAnnenStatNavIdenter(dvhRessurs));
        } catch (Exception e) {
            log.error("Feilet i last av ressurser fra dvh", e);
        }
    }

    private Set<String> findAnnenStatNavIdenter(List<DvhRessurs> dvhRessurs) {
        return dvhRessurs.stream()
            .filter(r -> r.getRemedyAnsattType().equals(ANNEN_STATLIG.beskrivelse))
            .map(DvhRessurs::getNavIdent)
            .collect(Collectors.toSet());
    }

    private void updateOrgtilknytningerStateFromDvh() {
        if (initialLoadComplete.get() && leaderElection.isLeader()) {
            log.info("Oppdater Orgtilnytninger state fra dvh");
            lastOrgtilknytningerTilEvtsrc();
        }
    }

    public void lastOrgtilknytningerTilEvtsrc() {
        try {
            ressursStillingLoader.loadAndProcess();
        } catch (Exception e) {
            log.error("Feilet i last av ressursStillinger", e);
        }
    }

    public void updateOrgtilknytningerForEksterneFromDvh() {
        log.info("Oppdater Orgtilknytninger for eksterne fra dvh");
        eksterneRessurserLoader.loadAndProcess();
    }


    public void loadTlfFromDvh() {
        log.info("Laster tlf fra dvh");
        try {
            val jsonTlfNr = dvhExchange("/proxy/dvh/json/Hr/NOM2/NOM_TLF_v1");
            val tlfRader = StreamSupport.stream(jsonTlfNr.get("NOM_TLF_v1Result").spliterator(), false)
                .filter(this::validateTlf)
                .map(DvhTelefon::new)
                .collect(Collectors.toList());
            log.info("Telefonnr lastet: Antall rader = " + tlfRader.size());
            dvhRepository.saveTlf(tlfRader);
        } catch (Exception e) {
            log.error("Failure loading tlf data from DVH", e);
        }
    }

    private boolean validateTlf(JsonNode jsonNode) {
        val isSecretNumber = !jsonNode.hasNonNull("issecretnumber") || (jsonNode.hasNonNull("issecretnumber") && !jsonNode.get("issecretnumber").asBoolean());
        val userRef1 = jsonNode.get("userreference1");

        return isSecretNumber && (userRef1.asText().startsWith("PT-") || userRef1.asText().startsWith("PK-"));
    }

    public void loadLedereAndOrganiseringerFromDvh() {
        log.info("Laster ledere og Organisering fra dvh");
        try {
            val jsonOrgenheter = dvhExchange("/proxy/dvh/json/Hr/NOM2/NOM_HRORG_HIERARKI2_v2");
            val ledere = new LinkedList<DvhLeder>();
            val organiseringer = new LinkedList<DvhOrganisering>();

            StreamSupport.stream(jsonOrgenheter.get("NOM_HRORG_HIERARKI2_v2Response").spliterator(), false)
                .filter(this::determineEnhetActive)
                .filter(this::determineEnhetNotIgnored)
                .forEach(e -> {
                    if (validateOrganisering(e)) {
                        DvhOrganisering dvhOrganisering = new DvhOrganisering(e);
                        organiseringer.add(dvhOrganisering);

                        // TODO remove, and get it properly from state topics
                        // save to the repository for the time being
                        Optional<String> stateFid = getFidForOrganisering(dvhOrganisering);

                        if (stateFid.isPresent()) {
                            if (erOrganiseringEndret(stateFid.get(), dvhOrganisering)) {
                                //if organisering is changed
                                organiseringEventCommandService.endreOrganisering(stateFid.get(), getEndreOrganiseringCmd(dvhOrganisering));
                            }
                        } else {
                            var opprettetFid = organiseringEventCommandService.opprettOrganisering(getOpprettOrganiseringCmd(dvhOrganisering));
                            addToStateRepository(opprettetFid, dvhOrganisering);
                        }

                    }

                    if (validateLeder(e)) {
                        ledere.add(new DvhLeder(e));
                    }
                });

            dvhService.saveLedere(ledere);
            dvhService.saveOrganiseringerAndSetOrganiserteOrgenheter(organiseringer);
        } catch (Exception e) {
            log.error("Failure loading ledere og Organisering data from DVH", e);
        }
    }

    private void addToStateRepository(String fid, DvhOrganisering dvhOrganisering) {
        var nomIdUnder = orgenhetService
            .getEnhetByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdUnder(), dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder());

        var nomIdOver = orgenhetService
            .getEnhetByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdOver(), dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver());

        if (nomIdUnder.isEmpty() || nomIdOver.isEmpty()) {
            return;
        }

        OrganiseringState organiseringState = OrganiseringState.builder()
            .fid(fid)
            .idUnder(nomIdUnder.get().getId())
            .idOver(nomIdOver.get().getId())
            .navnUnder(dvhOrganisering.getNavnUnder())
            .navnOver(dvhOrganisering.getNavnOver())
            .gyldigFom(dvhOrganisering.getGyldigFom())
            .gyldigTom(Optional.ofNullable(dvhOrganisering.getGyldigTom()))
            .build();

        organiseringStateRepository.put(fid, organiseringState);
    }

    private Optional<String> getFidForOrganisering(DvhOrganisering dvhOrganisering) {
        // TODO Flytte til en service i stedet for å bruke repository direkte?
        return Optional.ofNullable(organiseringStateRepository
            .findFidByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdUnder(), dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder()));
    }

    private boolean erOrganiseringEndret(String fid, DvhOrganisering dvhOrg) {
        // TODO
        boolean organiseringEndret = false;

        if (fid.startsWith("organisering")) {
            Optional<OrganiseringState> eksisterendeOrganisering = organiseringStateRepository.findByFid(fid);

            if (eksisterendeOrganisering.isPresent()) {
                OrganiseringState eksisterendeOrg = eksisterendeOrganisering.get();

                var orgEnhetUnder = orgenhetService
                    .getEnhetByAgressoIdNivaAndName(dvhOrg.getAgressoIdUnder(), dvhOrg.getOrgNivUnder(), dvhOrg.getNavnUnder());

                var orgEnhetOver = orgenhetService
                    .getEnhetByAgressoIdNivaAndName(dvhOrg.getAgressoIdOver(), dvhOrg.getOrgNivOver(), dvhOrg.getNavnOver());

                if (orgEnhetUnder.isPresent() && orgEnhetOver.isPresent()) {

                    if (!eksisterendeOrg.idUnder().equals(orgEnhetUnder.get().getId()) ||
                        !eksisterendeOrg.idOver().equals(orgEnhetOver.get().getId()) ||
                        !eksisterendeOrg.navnUnder().equals(dvhOrg.getNavnUnder()) ||
                        !eksisterendeOrg.navnOver().equals(dvhOrg.getNavnOver()) ||
                        !eksisterendeOrg.gyldigFom().equals(dvhOrg.getGyldigFom()) ||
                        !eksisterendeOrg.gyldigTom().equals(Optional.ofNullable(dvhOrg.getGyldigTom()))
                    ) {
                        organiseringEndret = true;
                    }
                }
            }
        }
        return organiseringEndret;
    }

    private EndreOrganiseringCmd getEndreOrganiseringCmd(DvhOrganisering dvhOrganisering) {
        return new EndreOrganiseringCmd(
            dvhOrganisering.getAgressoIdOver(),
            dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivOver(),
            dvhOrganisering.getOrgNivUnder(),
            dvhOrganisering.getNavnOver(),
            dvhOrganisering.getNavnUnder(),
            dvhOrganisering.getGyldigFom(),
            dvhOrganisering.getGyldigTom(),
            createInputMetadata(dvhOrganisering.getRawData())
        );
    }

    private OpprettOrganiseringCmd getOpprettOrganiseringCmd(DvhOrganisering dvhOrganisering) {
        return new OpprettOrganiseringCmd(
            dvhOrganisering.getAgressoIdOver(),
            dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivOver(),
            dvhOrganisering.getOrgNivUnder(),
            dvhOrganisering.getNavnOver(),
            dvhOrganisering.getNavnUnder(),
            dvhOrganisering.getGyldigFom(),
            dvhOrganisering.getGyldigTom(),
            createInputMetadata(dvhOrganisering.getRawData())
        );
    }

    private boolean determineEnhetNotIgnored(JsonNode jsonNode) {
        // 'deltar i rekruttering' og 'nav ventelønn' er kun for intern bruk i agresso.
        val orgnodeKode = jsonNode.get("ORGNODE_KODE").asText();
        val deltarIRekruttering = orgnodeKode.matches("0+");
        val navVenteloenn = orgnodeKode.equals("9999920");
        return !(deltarIRekruttering || navVenteloenn);
    }

    private JsonNode dvhExchange(String urlPath) {
        return nomProxyClient.getWebClient().get()
            .uri(urlPath)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    }

    private boolean determineEnhetActive(JsonNode e) {
        return e.get("ORGNODE_STATUS").asText().equals("N");
    }

    private boolean validateOrganisering(JsonNode e) {
        final var validate = e.hasNonNull("ORGNODE_KODE_OVER") && e.hasNonNull("ORGNODE_KODE")
            && e.hasNonNull("ORGNODE_NIVA_OVER") && e.hasNonNull("ORGNODE_NIVA");
        if (!validate) {
            log.info("Ugyldig organisering fra dvh, ORGNODE_KODE_OVER: {}, ORGNODE_KODE: {}", getAsText(e, "ORGNODE_KODE_OVER"), getAsText(e, "ORGNODE_KODE"));
        }
        return validate;
    }

    private boolean validateLeder(JsonNode e) {
        final var validate = e.hasNonNull("LEDER_NAVIDENT") && e.hasNonNull("ORGNODE_KODE") && e.hasNonNull("ORGNODE_NIVA");
        if (!validate) {
            log.info("Ugyldig leder fra dvh, LEDER_NAVIDENT: {}, ORGNODE_KODE: {}", getAsText(e, "LEDER_NAVIDENT"), getAsText(e, "ORGNODE_KODE"));
        }
        return validate;
    }

    private String getAsText(JsonNode e, String field) {
        JsonNode jsonNode = e.get(field);
        return jsonNode == null ? "<null>" : jsonNode.asText();
    }

    private boolean isInitialStateReady() {
        return ressursStateListener.initloadComplete() && orgtilknytningStateListener.initloadComplete() && orgEnhetStateListener.initloadComplete();
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    private InputMetadata createInputMetadata(Object rawData) {
        return new InputMetadata("nom-api BatchJob", "Datavarehus", rawData, new Origination("nom-api", "nom-api"));
    }
}
