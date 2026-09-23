package no.nav.nom.apps.api.ressurs;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.common.speiling.SpeilingService;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.FysiskBrukerstedNotFoundException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.nav.nom.libs.utils.common.util.DateUtils.isPeriodActive;

@Slf4j
@Service
public class FysiskBrukerstedService {
    static final String CACHE_TTL = "PT1H";
    public static final String MASSEFLYTTING_FYSISK_BRUKERSTED_VARSEL_FTLH = "masseflytting-fysiskbrukersted-varsel.ftlh";

    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursCommandService ressursCommandService;
    private final FysiskBrukerstedCache fysiskBrukerstedCache;
    private final AdClient adClient;
    private final Configuration freemarkerConfig;
    private final MicrosoftGraphClient microsoftGraphClient;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final SpeilingService speilingService;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;

    public FysiskBrukerstedService(
        AdClient adClient,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        RessursCommandService ressursCommandService,
        FysiskBrukerstedCache fysiskBrukerstedCache,
        Configuration freemarkerConfig,
        MicrosoftGraphClient microsoftGraphClient,
        OrgtilknytningCommandService orgtilknytningCommandService,
        SpeilingService speilingService,
        NomStateOrgtilknytningListener nomStateOrgtilknytningListener
    ) {
        this.adClient = adClient;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursCommandService = ressursCommandService;
        this.fysiskBrukerstedCache = fysiskBrukerstedCache;
        this.freemarkerConfig = freemarkerConfig;
        this.microsoftGraphClient = microsoftGraphClient;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.speilingService = speilingService;
        this.nomStateOrgtilknytningListener = nomStateOrgtilknytningListener;
    }

    public List<RessursState> getAlleRessurserMedFysiskBrukersted(String fysiskBrukerstedNr) {
        return ressursStateRepository.getAll().stream()
                .filter(ressurs -> Objects.equals(ressurs.primaryFysiskBrukersted(), fysiskBrukerstedNr))
                .toList();
    }

    public List<RessursState> getAlleRessurserMedFysiskBrukerstedIAktivOrgtilknytning(String fysiskBrukerstedNr) {
        var navidenter = orgtilknytningStateRepository.getAll().stream()
            .filter(PeriodeUtil::isCurrentPeriod)
            .filter(ot -> Objects.equals(ot.primaryFysiskBrukerstedNummer(), fysiskBrukerstedNr)
                || (ot.additionalFysiskeBrukerstederNummer() != null
                    && ot.additionalFysiskeBrukerstederNummer().contains(fysiskBrukerstedNr)))
            .map(OrgtilknytningState::navident)
            .distinct()
            .toList();
        return List.copyOf(ressursStateRepository.findByNavidentBulk(navidenter).values());
    }

    public List<RessursState> getAlleRessurserPaaEnhetMedFysiskBrukersted(String orgenhetId, String fysiskBrukerstedNr) {
        var navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet(orgenhetId);
        return ressursStateRepository.findByNavidentBulk(navidenter).values().stream()
            .filter(ressurs -> Objects.equals(ressurs.primaryFysiskBrukersted(), fysiskBrukerstedNr))
            .toList();
    }

    public HashMap<String, String> flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted(String fraFysiskBrukerstedNr, String tilFysiskBrukerstedNr, InputMetadata inputMetaData) {
        var navidenter = getAlleRessurserMedFysiskBrukersted(fraFysiskBrukerstedNr).stream().map(RessursState::navident).toList();
        return flyttRessurserTilFysiskBrukersted(navidenter, tilFysiskBrukerstedNr, inputMetaData);
    }

    public HashMap<String, String> flyttAlleRessurserPaaEnhetFraFysiskBrukerstedTilFysiskBrukersted(String orgenhet, String fraFysiskBrukerstedNr, String tilFysiskBrukerstedNr, InputMetadata inputMetaData ) {
        var navidenter = getAlleRessurserPaaEnhetMedFysiskBrukersted(orgenhet, fraFysiskBrukerstedNr).stream().map(RessursState::navident).toList();
        return flyttRessurserTilFysiskBrukersted(navidenter, tilFysiskBrukerstedNr, inputMetaData);
    }

    public HashMap<String, String> flyttRessurserTilFysiskBrukersted(List<String> navidenter, String tilFysiskBrukerstedNr, InputMetadata inputMetaData) {
        HashMap<String, String> feiledeNavidenter = new HashMap<>();
        navidenter.forEach(navident -> {
            try {
                oppdaterRessursFysiskBrukersted(navident, tilFysiskBrukerstedNr, inputMetaData);
            } catch (IllegalArgumentException e) {
                feiledeNavidenter.put(navident, e.getMessage());
                log.error("Feil ved oppdatering av ressurs {} til fysisk brukersted {}", navident, tilFysiskBrukerstedNr, e);
            }
        });
        sendMasseflyttFysiskBrukerstedEmail(tilFysiskBrukerstedNr, navidenter, feiledeNavidenter);
        return feiledeNavidenter;
    }

    public HashMap<String, String> flyttOrgtilknytningerTilNyFysiskBrukersted(List<String> otFids,
                                                                              String tilFysiskBrukerstedNr,
                                                                              String fraFysiskBrukerstedNr,
                                                                              InputMetadata inputMetaData
    ) {
        HashMap<String, String> feiledeEventer = new HashMap<>();
        var orgtilknytningerGruppertPaaFid = orgtilknytningStateRepository.findByFidIn(otFids)
            .stream()
            .filter(orgtilknytningState -> isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()))
            .filter(orgtilknytningState -> {
                var matchPrimary = fraFysiskBrukerstedNr.equals(orgtilknytningState.primaryFysiskBrukerstedNummer());
                var matchAdditional = orgtilknytningState.additionalFysiskeBrukerstederNummer()
                    .contains(fraFysiskBrukerstedNr);
                return matchPrimary || matchAdditional;
            })
            .collect(Collectors.toMap(OrgtilknytningState::fid, orgtilknytningState -> orgtilknytningState, (ot1, _) -> ot1));
        var alleNavidenterSomBleFlyttet = orgtilknytningerGruppertPaaFid.values().stream().map(OrgtilknytningState::navident).collect(Collectors.toList());

        // Registrer state-waits for alle gyldige OT-FIDs FØR vi sender Kafka-kommandoer
        var stateWaits = new HashMap<String, CompletableFuture<?>>();
        orgtilknytningerGruppertPaaFid.keySet().forEach(otFid ->
            stateWaits.put(otFid, nomStateOrgtilknytningListener.waitForStateAfterEventCreation(otFid))
        );

        otFids.forEach(otFid -> {
            try {
                var orgtilknytning = orgtilknytningerGruppertPaaFid.get(otFid);
                if (isNull(orgtilknytning)) {
                    throw OrgtilknytningNotFoundException.fid(otFid);
                }
                oppdaterOrgtilknytningFysiskBrukersted(orgtilknytningerGruppertPaaFid.get(otFid), tilFysiskBrukerstedNr, fraFysiskBrukerstedNr, inputMetaData);
            } catch (OrgtilknytningNotFoundException e) {
                feiledeEventer.put(otFid, e.getMessage());
                stateWaits.remove(otFid);
                log.error("Feil ved oppdatering av orgtilknytning {} til fysisk brukersted {}", otFid, tilFysiskBrukerstedNr, e);
            } catch (FysiskBrukerstedNotFoundException e) {
                var navident = orgtilknytningerGruppertPaaFid.get(otFid).navident();
                feiledeEventer.merge(navident, e.getMessage(), (existing, newMessage) -> existing + "; " + newMessage);
                stateWaits.remove(otFid);
                alleNavidenterSomBleFlyttet.remove(navident);
            }
        });

        // Vent på at OT-state er oppdatert fra Kafka før speiling
        ventPaaStateOppdateringer(stateWaits);

        sendMasseflyttFysiskBrukerstedEmail(tilFysiskBrukerstedNr, alleNavidenterSomBleFlyttet, feiledeEventer);
        alleNavidenterSomBleFlyttet.forEach(navident -> speilingService.speilBrukerstederFromMasseflytting(navident, inputMetaData));
        return feiledeEventer;
    }

    private void oppdaterRessursFysiskBrukersted(String navident, String fysiskBrukerstedNr, InputMetadata inputMetaData) {
        if (!fysiskBrukerstedExists(fysiskBrukerstedNr)) {
            throw new IllegalArgumentException("Ugyldig fysisk brukersted " + fysiskBrukerstedNr);
        }

        var fid = ressursStateRepository.findByNavident(navident)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke ressurs med navident " + navident))
            .fid();

        var ressursCmd = new OppdaterFysiskeBrukerstederCommand(
            fid,
            Set.of(fysiskBrukerstedNr),
            fysiskBrukerstedNr
        );
        ressursCommandService.oppdaterFysiskeBrukerstederCommand(ressursCmd, inputMetaData);
    }

    private void oppdaterOrgtilknytningFysiskBrukersted(OrgtilknytningState orgtilknytningState, String tilFysiskBrukerstedNr, String fraFysiskBrukerstedNr, InputMetadata inputMetaData) {
        if (!fysiskBrukerstedExists(tilFysiskBrukerstedNr)) {
            throw FysiskBrukerstedNotFoundException.notFound(tilFysiskBrukerstedNr);
        }
        class ChangePayload {String primaryFbr; List<String> additionalFbrList; boolean wasChanged;}
        var changePayload = new ChangePayload();
        { // init
            changePayload.primaryFbr = orgtilknytningState.primaryFysiskBrukerstedNummer();
            changePayload.additionalFbrList = List.copyOf(orgtilknytningState.additionalFysiskeBrukerstederNummer());
            changePayload.wasChanged = false;
        }

        var shouldChangePrimaryFysiskBrukersted = changePayload.primaryFbr.equals(fraFysiskBrukerstedNr);
        if (shouldChangePrimaryFysiskBrukersted) {
            changePayload.primaryFbr = tilFysiskBrukerstedNr;
            changePayload.wasChanged = true;
        }
        var shouldChangeAdditionalFysiskBrukersted = changePayload.additionalFbrList.contains(fraFysiskBrukerstedNr);
        if (shouldChangeAdditionalFysiskBrukersted) {
            changePayload.additionalFbrList = changePayload.additionalFbrList.stream()
                .map(fysiskBrukerstedNr -> fraFysiskBrukerstedNr.equals(fysiskBrukerstedNr) ? tilFysiskBrukerstedNr : fysiskBrukerstedNr)
                .toList();
            changePayload.wasChanged = true;
        }
        if (changePayload.wasChanged) {// remove duplicate entries [A,[A,B]] -> [A,[B]], [A,[A]] -> [A,[]], etc
            var filteredData = new ArrayList<String>(changePayload.additionalFbrList.size());
            for (var item : changePayload.additionalFbrList) {
                if (!filteredData.contains(item) && !changePayload.primaryFbr.equals(item)) {
                    filteredData.add(item);
                }
            }
            changePayload.additionalFbrList = filteredData;
        }

        if(changePayload.wasChanged){
            orgtilknytningCommandService.endreFysiskeBrukersted(new EndreFysiskeBrukerstederCommand(
                orgtilknytningState.fid(),
                orgtilknytningState.navident(),
                orgtilknytningState.orgEnhetId(),
                changePayload.primaryFbr,
                changePayload.additionalFbrList,
                null,
                inputMetaData
            ));
        } else{
            log.warn("Skulle oppdatere fysisk brukersted {} -> {} for orgtilknytning {}, men ingenting ble endret ", fraFysiskBrukerstedNr, tilFysiskBrukerstedNr, orgtilknytningState.fid());
        }
    }

    public boolean fysiskBrukerstedExists(String fysiskBrukerstedNr) {
        return fysiskBrukerstedCache.getAll().stream()
            .anyMatch(fysiskBrukersted -> fysiskBrukersted.nr().equals(fysiskBrukerstedNr));
    }

    public Optional<FysiskBrukersted> findFysiskBrukersted(String fysiskBrukerstedNr) {
        return fysiskBrukerstedCache.getAll().stream()
            .filter(fysiskBrukersted -> Objects.equals(fysiskBrukersted.nr(), fysiskBrukerstedNr))
            .findFirst();
    }

    public Map<String, FysiskBrukersted> getAllFysiskBrukersted() {
        return fysiskBrukerstedCache.getAll().stream()
            .collect(Collectors.toMap(FysiskBrukersted::nr, fysiskBrukersted -> fysiskBrukersted,
                (fysiskBrukersted, _) -> fysiskBrukersted));
    }

    public void sendMasseflyttFysiskBrukerstedEmail(
        String tilFysiskbrukersted,
        List<String> navidenter,
        HashMap<String, String> feiledeNavidenter
    ) {
        try {
            if (!SecurityUtil.hasAuthentication()) {
                log.error("Ingen innlogget bruker, kan ikke sende epost");
                return;
            }

            // Henter innsender sin epost adresse
            String innsenderNavident = SecurityUtil.getAuthenticatedNavIdent();
            var innsenderVisningsnavn = ressursStateRepository.findByNavident(innsenderNavident)
                .map(RessursState::visningsnavn)
                .map(Visningsnavn::getFormattedVisningsnavn)
                .orElse("UKJENT");

            var recipientList = adClient.getAdRessurser(Set.of(innsenderNavident)).getRessurser().stream()
                .map(AdRessurs::getEmail)
                .collect(Collectors.toList());

            String subject = String.format("Masseflytting av ressurser til fysiskbrukersted %s", tilFysiskbrukersted);
            String body = getFormattedMasseflyttEmailBody(
                tilFysiskbrukersted,
                navidenter.size() - feiledeNavidenter.size(),
                feiledeNavidenter.size(),
                feiledeNavidenter.keySet(),
                innsenderVisningsnavn
            );
            log.info("Masseflytting epost. Mottakere: {}\nBody:\n{}", recipientList, body);
            EmailMessage message = new EmailMessage(subject, body, recipientList);
            microsoftGraphClient.sendEmail(message).subscribe();

        } catch (ADResponseException e) {
            log.error("Fant ikke innsender sin epost adresse for masseflytting", e);
        } catch (IOException | TemplateException e) {
            log.error("Feil ved generering av epost innhold", e);
        }
    }

    private String getFormattedMasseflyttEmailBody(
        String tilFysiskbrukersted,
        int antallFlyttet,
        int feiletAntall,
        Set<String> feiledeNavidenter,
        String innsenderVisningsnavn
    ) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "nyFysiskbrukersted", tilFysiskbrukersted,
            "antallFlyttet", antallFlyttet,
            "feiletAntall", feiletAntall,
            "total", antallFlyttet + feiletAntall,
            "feiletNavidenter", String.join(",\n", feiledeNavidenter),
            "innsenderVisningsnavn", innsenderVisningsnavn
        );
        freemarkerConfig.getTemplate(MASSEFLYTTING_FYSISK_BRUKERSTED_VARSEL_FTLH).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    private void ventPaaStateOppdateringer(Map<String, CompletableFuture<?>> stateWaits) {
        if (stateWaits.isEmpty()) {
            return;
        }
        try {
            CompletableFuture.allOf(stateWaits.values().toArray(CompletableFuture[]::new))
                .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Timeout eller feil ved venting på OT state-oppdatering for masseflytting fysisk brukersted - fortsetter med speiling", e);
            stateWaits.keySet().forEach(nomStateOrgtilknytningListener::removeWait);
        }
    }
}

