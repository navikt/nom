package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RequiredValueMissingException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.DelayedEvent;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.DelayedEventRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonRepository;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningSektorEndretEventV1;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPrimaryTelefonnummerOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.Ressurs;
import no.nav.nom.generated.model.RessursState;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeEndOfDay;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.formatFornavnWithMellomnavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.isActive;

@Service
public class NomIntegrasjonService {

    private final Logger logger = LoggerFactory.getLogger(NomIntegrasjonService.class);

    private final NomIntegrasjonRepository nomIntegrasjonRepository;

    private final NomApiClient nomApiClient;

    private final LeaderElection leaderElection;

    private final DelayedEventRepository delayedEventRepository;

    private final ObjectMapper objectMapper;
    private final Unleash unleash;


    public NomIntegrasjonService(
        NomIntegrasjonRepository nomIntegrasjonRepository,
        NomApiClient nomApiClient,
        LeaderElection leaderElection,
        DelayedEventRepository delayedEventRepository,
        ObjectMapper objectMapper,
        Unleash unleash
    ) {
        this.nomIntegrasjonRepository = nomIntegrasjonRepository;
        this.nomApiClient = nomApiClient;
        this.leaderElection = leaderElection;
        this.delayedEventRepository = delayedEventRepository;
        this.objectMapper = objectMapper;
        this.unleash = unleash;
    }

    // Run at 00:15 every night
    @Scheduled(cron = "0 15 0 * * *")
    public void findRessurserWithSluttdatoYesterdayAndDeleteThemFromRemedy() {
        if (leaderElection.isLeader()) {
            triggerDelayedEvents();
        }
    }

    @EventListener
    public void contextRefreshed(ContextRefreshedEvent event) {
        if (leaderElection.isLeader()) {
            triggerDelayedEvents();
        }
    }

    public void handleRessursEvent(RessursEvent ressursEvent) {

        if (unleash.isEnabled("nom.ressurs.opprett_statlig")) {
            if (ressursEvent instanceof RessursOpprettetEventV1 ressursOpprettetEventV1) {
                createRessurs(ressursOpprettetEventV1);

            } else if (ressursEvent instanceof RessursSluttetEventV1 ressursSluttetEventV1) {
                handleRessursSluttetEventV1(ressursSluttetEventV1);

            } else if (ressursEvent instanceof RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1) {
                handleRessursSluttdatoOppdatertEventV1(ressursSluttdatoOppdatertEventV1);

            } else if (ressursEvent instanceof RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertV1) {
                updateRessursPersonalia(ressursPersonaliaOppdatertV1);

            } else if (ressursEvent instanceof RessursFysiskeBrukerstederOppdatertEventV1 ressursFysiskeBrukerstederOppdatertEventV1) {
                updateRessursFysiskeBrukersteder(ressursFysiskeBrukerstederOppdatertEventV1);
            } else if (ressursEvent instanceof RessursPrimaryTelefonnummerOppdatertEventV1 ressursPrimaryTelefonnummerOppdatertEventV1) {
                updateRessursTelefonnummer(ressursPrimaryTelefonnummerOppdatertEventV1);
            }
        } else {
            if (ressursEvent instanceof RessursOpprettetEventV1 ressursOpprettetEventV1
                && ressursOpprettetEventV1.getRessursOpprettetEventData().sektor().kanOppretteRessurs()) {
                createRessurs(ressursOpprettetEventV1);

            } else if (ressursEvent instanceof RessursSluttetEventV1 ressursSluttetEventV1
                && isFidEksternEllerKommunalRessurs(ressursEvent.getFid())
            ) {
                handleRessursSluttetEventV1(ressursSluttetEventV1);

            } else if (ressursEvent instanceof RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1) {
                handleRessursSluttdatoOppdatertEventV1(ressursSluttdatoOppdatertEventV1);

            } else if (ressursEvent instanceof RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertV1
                && isFidEksternEllerKommunalRessurs(ressursEvent.getFid())
            ) {
                updateRessursPersonalia(ressursPersonaliaOppdatertV1);

            } else if (ressursEvent instanceof RessursFysiskeBrukerstederOppdatertEventV1 ressursFysiskeBrukerstederOppdatertEventV1
                && isFidEksternEllerKommunalRessurs(ressursEvent.getFid())) {
                updateRessursFysiskeBrukersteder(ressursFysiskeBrukerstederOppdatertEventV1);
            } else if (ressursEvent instanceof RessursPrimaryTelefonnummerOppdatertEventV1 ressursPrimaryTelefonnummerOppdatertEventV1
                && isFidEksternEllerKommunalRessurs(ressursEvent.getFid())) {
                updateRessursTelefonnummer(ressursPrimaryTelefonnummerOppdatertEventV1);
            }
        }
    }

    public void handleOrgtilknytningEvent(OrgtilknytningEvent orgtilknytningEvent) {

        if (unleash.isEnabled("nom.ressurs.opprett_statlig")) {
            if (orgtilknytningEvent instanceof OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1
                && orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().dagligOppfolging()) {
                handleOrgtilknytningDagligOppfolgingEndretEvent(orgtilknytningDagligOppfolgingEndretEventV1);
            } else if (orgtilknytningEvent instanceof OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2
                && isOrgtilknytningDOL(orgtilknytningDatoEndretEventV2.getFid())) {
                handleOrgtilknytningDatoEndretEvent(orgtilknytningDatoEndretEventV2);
            } else if (orgtilknytningEvent instanceof  OrgtilknytningSektorEndretEventV1 orgtilknytningSektorEndretEventV1
                && isOrgtilknytningDOL(orgtilknytningSektorEndretEventV1.getFid())) {
                handleOrgtilknytningSektorEndretEvent(orgtilknytningSektorEndretEventV1);
            } else if (orgtilknytningEvent instanceof OrgtilknytningFjernetEventV1 orgtilknytningFjernetEventV1) {
                deleteOrgtilknytningEvents(orgtilknytningFjernetEventV1.getFid());
            }
        } else {
            if (orgtilknytningEvent instanceof OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1
                && orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().dagligOppfolging()
                && isNavidentEksternEllerKommunalRessurs(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData()
                .navident())) {

                handleOrgtilknytningDagligOppfolgingEndretEvent(orgtilknytningDagligOppfolgingEndretEventV1);
            } else if (orgtilknytningEvent instanceof OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2
                && isOrgtilknytningEksternEllerKommunalAndDagligOppfolgning(orgtilknytningDatoEndretEventV2.getFid())) {

                handleOrgtilknytningDatoEndretEvent(orgtilknytningDatoEndretEventV2);
            } else if (orgtilknytningEvent instanceof  OrgtilknytningSektorEndretEventV1 orgtilknytningSektorEndretEventV1
                && isOrgtilknytningEksternEllerKommunalAndDagligOppfolgning(orgtilknytningSektorEndretEventV1.getFid())) {
                handleOrgtilknytningSektorEndretEvent(orgtilknytningSektorEndretEventV1);
            } else if (orgtilknytningEvent instanceof OrgtilknytningFjernetEventV1 orgtilknytningFjernetEventV1) {
                deleteOrgtilknytningEvents(orgtilknytningFjernetEventV1.getFid());
            }
        }
    }

    private void deleteOrgtilknytningEvents(String fid) {
        logger.info("Deleting Orgtilknytning events with fid {}", fid);
        delayedEventRepository.deleteDelayedOrgtilknytningEvents(fid);
    }

    private void handleOrgtilknytningDagligOppfolgingEndretEvent(OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1) {
        if (orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigFom().isAfter(LocalDate.now())
            && sjekkAktivOrgTilknytning(orgtilknytningDagligOppfolgingEndretEventV1)) {
            logger.info(
                "Delaying remedy update for OrgtilknytningDagligOppfolgingEndretEventV1 with fid {}",
                orgtilknytningDagligOppfolgingEndretEventV1.getFid()
            );

            delayEventUpdate(
                orgtilknytningDagligOppfolgingEndretEventV1,
                orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigFom()
            );
        } else if (!isOrgtilknytningHistorical(orgtilknytningDagligOppfolgingEndretEventV1)) {
            updateOrgtilknytning(orgtilknytningDagligOppfolgingEndretEventV1);
        }
    }

    private void handleOrgtilknytningDatoEndretEvent(OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2) {
        if (orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigFom().isAfter(LocalDate.now()) && sjekkAktivOrgTilknytning(
            orgtilknytningDatoEndretEventV2)) {
            logger.info("Delaying remedy update for OrgtilknytningDatoEndretEventV2 with fid {}", orgtilknytningDatoEndretEventV2.getFid());

            delayEventUpdate(
                orgtilknytningDatoEndretEventV2,
                orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigFom()
            );
        } else if (!isOrgtilknytningHistorical(orgtilknytningDatoEndretEventV2)) {
            updateOrgtilknytning(orgtilknytningDatoEndretEventV2);
        }
    }

    private void handleOrgtilknytningSektorEndretEvent(OrgtilknytningSektorEndretEventV1 orgtilknytningSektorEndretEventV1) {
        var orgtilknytning = nomApiClient.getOrgtilknytning(orgtilknytningSektorEndretEventV1.getFid());
        if (orgtilknytning.getGyldigFom().isAfter(LocalDate.now())) {
            logger.info("Delaying remedy update for OrgtilknytningSektorEndretEventV1 with fid {}", orgtilknytningSektorEndretEventV1.getFid());

            delayEventUpdate(
                orgtilknytningSektorEndretEventV1,
                orgtilknytning.getGyldigFom()
            );
        } else if (!orgtilknytning.getGyldigTom().isBefore(LocalDate.now())) {
            updateOrgtilknytningSektor(orgtilknytningSektorEndretEventV1);
        }
    }

    private static boolean isOrgtilknytningHistorical(OrgtilknytningEvent orgtilknytningEvent) {
        if (orgtilknytningEvent instanceof OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1
            && orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigTom().isPresent()) {
            return orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigTom().get().isBefore(LocalDate.now());
        }
        if (orgtilknytningEvent instanceof OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2
            && orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigTom().isPresent()) {
            return orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigTom().get().isBefore(LocalDate.now());
        }
        return false;
    }

    private boolean sjekkAktivOrgTilknytning(OrgtilknytningEvent orgtilknytningEvent) {
        if (orgtilknytningEvent instanceof OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1) {
            return nomApiClient.getOrgtilknytningerForNavident(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData()
                    .navident()).stream()
                .anyMatch(NomIntegrasjonService::orgtilknytningIsActiveOnCurrentDate);
        }

        if (orgtilknytningEvent instanceof OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2) {
            var orgtilknytning = nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid());
            return orgtilknytningIsActiveOnCurrentDate(orgtilknytning);
        }
        return false;
    }

    private static boolean orgtilknytningIsActiveOnCurrentDate(OrgtilknytningState orgtilknytningState) {
        return !orgtilknytningState.getGyldigFom().isAfter(LocalDate.now()) && !orgtilknytningState.getGyldigTom().isBefore(LocalDate.now());
    }

    private void handleDelayedOrgtiknytning(OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1) {
        logger.info("Handling delayed update of OrgtilknytningDagligOppfolgingEndretEventV1 with fid {}", orgtilknytningDagligOppfolgingEndretEventV1.getFid());

        try {
            var orgtilknytningDolEventData = orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData();
            var orgtilknytningState = nomApiClient.getOrgtilknytning(orgtilknytningDagligOppfolgingEndretEventV1.getFid());

            if (orgtilknytningState.getErDagligOppfolging() &&
                orgtilknytningDolEventData.gyldigFom().equals(orgtilknytningState.getGyldigFom()) &&
                orgtilknytningDolEventData.gyldigTom().equals(Optional.ofNullable(orgtilknytningState.getGyldigTom()))) {
                updateOrgtilknytning(orgtilknytningDagligOppfolgingEndretEventV1);
            }
        } catch (OrgtilknytningNotFoundException e) {
            logger.info("Delayed update of OrgtilknytningDagligOppfolgingEndretEventV1 cancelled. {}", e.getMessage());
        }
    }

    private void handleDelayedOrgtiknytningSektor(OrgtilknytningSektorEndretEventV1 orgtilknytningSektorEndretEventV1) {
        logger.info("Handling delayed update of OrgtilknytningSektorEndretEventV1 with fid {}", orgtilknytningSektorEndretEventV1.getFid());

        try {
            var orgtilknytningState = nomApiClient.getOrgtilknytning(orgtilknytningSektorEndretEventV1.getFid());

            if (orgtilknytningState.getErDagligOppfolging()
                && isActive(orgtilknytningState.getGyldigFom(), orgtilknytningState.getGyldigTom())) {
                updateOrgtilknytningSektor(orgtilknytningSektorEndretEventV1);
            }
        } catch (OrgtilknytningNotFoundException e) {
            logger.info("Delayed update of OrgtilknytningSektorEndretEventV1 cancelled. {}", e.getMessage());
        }
    }

    private void handleDelayedOrgtiknytning(OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2) {
        logger.info("Handling delayed update of OrgtilknytningDatoEndretEventV2 with fid {}", orgtilknytningDatoEndretEventV2.getFid());

        try {
            var orgtilknytningEventData = orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData();
            var orgtilknytningState = nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid());

            if (orgtilknytningState.getErDagligOppfolging() &&
                orgtilknytningEventData.gyldigFom().equals(orgtilknytningState.getGyldigFom()) &&
                orgtilknytningEventData.gyldigTom().equals(Optional.ofNullable(orgtilknytningState.getGyldigTom()))) {
                updateOrgtilknytning(orgtilknytningDatoEndretEventV2);
            }
        } catch (OrgtilknytningNotFoundException e) {
            logger.info("Delayed update of OrgtilknytningDatoEndretEventV2. {} cancelled", e.getMessage());
        }
    }

    private void updateOrgtilknytning(OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2) {
        logger.info("Updating remedy data for OrgtilknytningDatoEndretEventV2 with fid {}", orgtilknytningDatoEndretEventV2.getFid());

        var orgtilknytningDatoEndretEventData = orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData();
        var orgtilknytningState = nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid());

        var ressursState = nomApiClient.getRessursByNavident(orgtilknytningState.getNavident());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        var gyldigTom = orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigTom()
            .filter(dato -> remedyCodeToNomSektor(nomIntegrasjonEntityWithDefaultDataFromNom.getSektor()).equals(Sektor.EKSTERN))
            .orElse(null);

        var remedyEnhetIdForOrgenhet = getRemedyEnhetId(orgtilknytningState.getOrgEnhetId());

        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .enhetsId(remedyEnhetIdForOrgenhet)
            .tiltredelsesdato(epochSecondsNorwegianTimeNoon(orgtilknytningDatoEndretEventData.gyldigFom()))
            .utlopsdato(isNull(gyldigTom) ? null : epochSecondsNorwegianTimeEndOfDay(gyldigTom))
            .masseflytting(!isNull(orgtilknytningState.getMasseflyttetFraFid()) ? "X" : null)
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            orgtilknytningDatoEndretEventV2.getMetadata().inputMetadata().origination().initiatedBy()
        );
    }

    private void updateOrgtilknytning(OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1) {
        logger.info("Updating remedy data for OrgtilknytningDagligOppfolgingEndretEventV1 with fid {}", orgtilknytningDagligOppfolgingEndretEventV1.getFid());

        var ressursSkalBeholdeTilganger =
            orgtilknytningDagligOppfolgingEndretEventV1.getMetadata().inputMetadata().inputInterfaceMethodName().equals("masseflytting")
                && erMasseflytting(orgtilknytningDagligOppfolgingEndretEventV1.getMetadata().inputMetadata());

        var orgtilknytningDagligOppfolgingEndretEventData = orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData();

        var ressursState = nomApiClient.getRessursByNavident(orgtilknytningDagligOppfolgingEndretEventData.navident());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        var newRemedyEnhetId = getRemedyEnhetId(orgtilknytningDagligOppfolgingEndretEventData.orgEnhetId());

        var gyldigTom = orgtilknytningDagligOppfolgingEndretEventData.gyldigTom()
            .filter(dato -> remedyCodeToNomSektor(nomIntegrasjonEntityWithDefaultDataFromNom.getSektor()).equals(Sektor.EKSTERN))
            .orElse(null);

        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .enhetsId(newRemedyEnhetId)
            .tiltredelsesdato(epochSecondsNorwegianTimeNoon(orgtilknytningDagligOppfolgingEndretEventData.gyldigFom()))
            .utlopsdato(isNull(gyldigTom) ? null : epochSecondsNorwegianTimeEndOfDay(gyldigTom))
            .masseflytting(ressursSkalBeholdeTilganger ? "X" : null)
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            orgtilknytningDagligOppfolgingEndretEventV1.getMetadata().inputMetadata().origination().initiatedBy()
        );

    }

    private void updateOrgtilknytningSektor(OrgtilknytningSektorEndretEventV1 orgtilknytningSektorEndretEventV1) {
        logger.info("Updating remedy data for OrgtilknytningSektorEndretEventV1 with fid {}", orgtilknytningSektorEndretEventV1.getFid());

        var orgtilknytningSektorEndretEventData = orgtilknytningSektorEndretEventV1.getOrgtilknytningSektorEndretEventData();

        var ressursState = nomApiClient.getRessursByNavident(orgtilknytningSektorEndretEventData.navident());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .sektor(nomSektorToRemedyCode(orgtilknytningSektorEndretEventData.sektor()))
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            orgtilknytningSektorEndretEventV1.getMetadata().inputMetadata().origination().initiatedBy()
        );
    }

    private void handleRessursSluttetEventV1(RessursSluttetEventV1 ressursSluttetEventV1) {

        logger.info("Updating remedy data for RessursSluttetEventV1 with fid {}", ressursSluttetEventV1.getFid());

        var sluttdatoForRessurs = ressursSluttetEventV1.getRessursSluttetEventData().sluttdato();

        if (sluttdatoForRessurs.isBefore(LocalDate.now())) {
            avsluttRessursInRemedy(
                ressursSluttetEventV1.getFid(),
                sluttdatoForRessurs,
                ressursSluttetEventV1.getMetadata().inputMetadata().origination().initiatedBy()
            );
        } else {
            delayEventUpdate(ressursSluttetEventV1, sluttdatoForRessurs.plusDays(1));
        }
    }

    private void handleDelayedRessursSluttetEventV1(RessursSluttetEventV1 ressursSluttetEventV1) {
        logger.info("Handling delayed %s with fid %s".formatted(ressursSluttetEventV1.getClass().getSimpleName(), ressursSluttetEventV1.getFid()));

        var ressursState = nomApiClient.getRessursByFid(ressursSluttetEventV1.getFid());

        var sluttDatoInEventIsStillSluttdatoForRessurs = ressursSluttetEventV1.getRessursSluttetEventData().sluttdato()
            .equals(ressursState.getSluttdato());

        if (sluttDatoInEventIsStillSluttdatoForRessurs) {
            handleRessursSluttetEventV1(ressursSluttetEventV1);
        }
    }

    private void handleRessursSluttdatoOppdatertEventV1(RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1) {

        logger.info(
            "Updating remedy data for {} with fid {}",
            ressursSluttdatoOppdatertEventV1.getClass().getSimpleName(),
            ressursSluttdatoOppdatertEventV1.getFid()
        );

        ressursSluttdatoOppdatertEventV1.getRessursSluttdatoOppdatertEventData().sluttdato().ifPresent(
            sluttdatoForRessurs -> {
                if (sluttdatoForRessurs.isBefore(LocalDate.now())) {
                    avsluttRessursInRemedy(
                        ressursSluttdatoOppdatertEventV1.getFid(),
                        sluttdatoForRessurs,
                        ressursSluttdatoOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy()
                    );
                } else {
                    delayEventUpdate(ressursSluttdatoOppdatertEventV1, sluttdatoForRessurs.plusDays(1));
                }
            }
        );
    }

    private void delayEventUpdate(Event event, LocalDate until) {
        delayedEventRepository.save(
            DelayedEvent.builder()
                .delayedUntil(until)
                .event(event)
                .build());
    }

    private void handleDelayedRessursSluttdatoOppdatertEventV1(RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1) {
        logger.info("Handling delayed %s with fid %s".formatted(
            ressursSluttdatoOppdatertEventV1.getClass().getSimpleName(),
            ressursSluttdatoOppdatertEventV1.getFid()
        ));

        var ressursState = nomApiClient.getRessursByFid(ressursSluttdatoOppdatertEventV1.getFid());

        var sluttDatoInEventIsStillSluttdatoForRessurs = ressursSluttdatoOppdatertEventV1.getRessursSluttdatoOppdatertEventData().sluttdato()
            .equals(Optional.ofNullable(ressursState.getSluttdato()));

        if (sluttDatoInEventIsStillSluttdatoForRessurs) {
            handleRessursSluttdatoOppdatertEventV1(ressursSluttdatoOppdatertEventV1);
        }
    }

    private void avsluttRessursInRemedy(String ressursFid, LocalDate sluttdatoForRessurs, String initiatedBy) {
        if (sluttdatoForRessurs.isBefore(LocalDate.now())) {
            var ressursState = nomApiClient.getRessursByFid(ressursFid);

            markRessursForDeletion(
                ressursState.getNavident(),
                initiatedBy
            );

            logger.info("Deleted ressurs with fid {} and with sluttdato {}", ressursFid, sluttdatoForRessurs);
        }
    }

    public void findRessurserWithSluttdatoAndMarkForDeletion(LocalDate sluttdato, String initiatedBy) {
        var ressurserWithSluttdato =
            Stream.concat(
                nomApiClient.getRessurser(no.nav.nom.generated.model.Sektor.EKSTERN, sluttdato).stream(),
                nomApiClient.getRessurser(no.nav.nom.generated.model.Sektor.NAV_KOMMUNAL, sluttdato).stream()
            ).toList();

        var ressurserToDeleteFromRemedy = ressurserWithSluttdato.stream()
            .filter(ressurs -> nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(ressurs.getNavident())
                .map(existingNomIntegrasjonEntityForRessurs ->
                    existingNomIntegrasjonEntityForRessurs.getEntryOperasjon().equals("Slette"))
                .orElse(true)
            ).toList();

        ressurserToDeleteFromRemedy.forEach(ressursSomSkalSlettes ->
            markRessursForDeletion(ressursSomSkalSlettes.getNavident(), initiatedBy)
        );

        logger.info("Deleted following resssurser from remedy: {}", ressurserToDeleteFromRemedy.stream().map(RessursState::getNavident).toList());
    }

    protected void triggerDelayedEvents() {
        var delayedEventsReadyForHandling = delayedEventRepository.findByDelayedUntilLessThanEqual(LocalDate.now());

        delayedEventsReadyForHandling.forEach(this::handleDelayedEvent);

    }

    private void handleDelayedEvent(DelayedEvent delayedEvent) {
        var nomEvent = delayedEvent.getEvent();

        if (nomEvent instanceof RessursSluttetEventV1 ressursSluttetEventV1) {
            handleDelayedRessursSluttetEventV1(ressursSluttetEventV1);
        } else if (nomEvent instanceof RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1) {
            handleDelayedRessursSluttdatoOppdatertEventV1(ressursSluttdatoOppdatertEventV1);
        } else if (nomEvent instanceof OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1) {
            handleDelayedOrgtiknytning(orgtilknytningDagligOppfolgingEndretEventV1);
        } else if (nomEvent instanceof OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2) {
            handleDelayedOrgtiknytning(orgtilknytningDatoEndretEventV2);
        } else if (nomEvent instanceof OrgtilknytningSektorEndretEventV1 orgtilknytningSektorEndretEventV1) {
            handleDelayedOrgtiknytningSektor(orgtilknytningSektorEndretEventV1);
        }

        delayedEventRepository.deleteById(delayedEvent.getDelayedEventId());
    }

    private void createRessurs(RessursOpprettetEventV1 ressursOpprettetEventV1) {
        var ressursOpprettetEventData = ressursOpprettetEventV1.getRessursOpprettetEventData();
        var ressursOpprettetAdditionalData = ressursOpprettetEventV1.getRessursOpprettetAdditionalData();
        Map<String, Object> inputInterfaceMethodArguments = objectMapper.convertValue(
            ressursOpprettetEventV1.getMetadata().inputMetadata().inputInterfaceMethodArguments(),
            new TypeReference<>() {}
        );

        logger.info("Creating ressurs for RessursOpprettetEventV1 with fid: {}", ressursOpprettetEventV1.getFid());

        var remedyEnhetsId = getRemedyEnhetId(
            Optional.ofNullable((String) inputInterfaceMethodArguments.get("orgEnhetId"))
                .orElseThrow(() -> RequiredValueMissingException.forField("orgEnhetId"))
        );

        var sluttDato = Optional.ofNullable((String) inputInterfaceMethodArguments.get("sluttDato"))
            .filter(ressursSluttdato -> ressursOpprettetEventV1.getRessursOpprettetEventData().sektor().equals(Sektor.EKSTERN))
            .map(LocalDate::parse)
            .orElse(null);

        var fysiskBrukerstedNr = Optional.ofNullable((String) inputInterfaceMethodArguments.get("fysiskBrukerstedNr"))
            .orElseThrow(() -> RequiredValueMissingException.forField("fysiskBrukerstedNr"));

        NomIntegrasjonEntity newNomIntegrasjonEntity = NomIntegrasjonEntity.builder()
            .enhetsId(remedyEnhetsId)
            .etternavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().etternavn()).trim())
            .fodselsnummer(ressursOpprettetEventData.personident())
            .fornavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().getFornavnMellomnavn()).trim())
            .fysiskBrukerstedId(fysiskBrukerstedNr)
            .navId(ressursOpprettetAdditionalData.navident().orElseThrow(() -> RequiredValueMissingException.forField("navId")))
            .sektor(nomSektorToRemedyCode(ressursOpprettetEventData.sektor()))
            .tiltredelsesdato(ressursOpprettetAdditionalData.startdato()
                .map(Utils::epochSecondsNorwegianTimeNoon)
                .orElseThrow(() -> RequiredValueMissingException.forField("tiltredelsesdato")))
            .utlopsdato(isNull(sluttDato) ? null : epochSecondsNorwegianTimeEndOfDay(sluttDato))
            .mobil(ressursOpprettetEventData.primaryTelefonnummer().orElse(null))
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            ressursOpprettetEventV1.getMetadata().inputMetadata().origination().initiatedBy()
        );

        logger.info("Created ressurs for fid: " + ressursOpprettetEventV1.getFid());
    }

    private void updateRessursPersonalia(RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertV1) {
        logger.info(
            "Updating remedy data for {} with fid {}", ressursPersonaliaOppdatertV1.getClass().getSimpleName(), ressursPersonaliaOppdatertV1.getFid()
        );

        var ressursState = nomApiClient.getRessursByFid(ressursPersonaliaOppdatertV1.getFid());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        var newVisningsnavn = ressursPersonaliaOppdatertV1.getPersonaliaOppdatertEventData().visningsnavn();
        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .fornavn(capitalizeNavn(newVisningsnavn.getFornavnMellomnavn()).trim())
            .etternavn(capitalizeNavn(newVisningsnavn.etternavn()).trim())
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            ressursPersonaliaOppdatertV1.getMetadata().inputMetadata().origination().initiatedBy()
        );
    }

    private void updateRessursFysiskeBrukersteder(RessursFysiskeBrukerstederOppdatertEventV1 ressursFysiskeBrukerstederOppdatertEventV1) {
        logger.info(
            "Updating remedy data for {} with fid {}",
            ressursFysiskeBrukerstederOppdatertEventV1.getClass().getSimpleName(),
            ressursFysiskeBrukerstederOppdatertEventV1.getFid()
        );

        var ressursState = nomApiClient.getRessursByFid(ressursFysiskeBrukerstederOppdatertEventV1.getFid());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .fysiskBrukerstedId(
                ressursFysiskeBrukerstederOppdatertEventV1
                    .getRessursFysiskeBrukerstederOppdatertEventData()
                    .primaryFysiskBrukersted()
            )
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            ressursFysiskeBrukerstederOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy()
        );
    }

    private void updateRessursTelefonnummer(RessursPrimaryTelefonnummerOppdatertEventV1 ressursPrimaryTelefonnummerOppdatertEventV1) {
        logger.info(
            "Updating remedy data for {} with fid {}",
            ressursPrimaryTelefonnummerOppdatertEventV1.getClass().getSimpleName(),
            ressursPrimaryTelefonnummerOppdatertEventV1.getFid()
        );

        var ressursState = nomApiClient.getRessursByFid(ressursPrimaryTelefonnummerOppdatertEventV1.getFid());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);
        var primaryTelefonnummer = ressursPrimaryTelefonnummerOppdatertEventV1
            .getMobilnummerOppdatertEventData()
            .primaryTelefonnummer();
        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .mobil((isNull(primaryTelefonnummer) || primaryTelefonnummer.isBlank()) ? null : primaryTelefonnummer)
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            ressursPrimaryTelefonnummerOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy()
        );
    }

    private NomIntegrasjonEntity nomIntegrasjonEntityFromNomData(Ressurs ressurs) {
        var dagligOppfolgningOrgTilknytning = nomApiClient.getOrgtilknytningerForNavident(ressurs.getNavident()).stream()
            .filter(orgtilknytningState -> isActive(orgtilknytningState.getGyldigFom(), orgtilknytningState.getGyldigTom()))
            .filter(OrgtilknytningState::getErDagligOppfolging)
            .findFirst();

        if (dagligOppfolgningOrgTilknytning.isEmpty()) {
            logger.info("Ingen aktiv DOL funnet for ressurs med navident {}, prøver å finne fremtidig DOL", ressurs.getNavident());
            dagligOppfolgningOrgTilknytning = nomApiClient.getOrgtilknytningerForNavident(ressurs.getNavident()).stream()
                .sorted(Comparator.comparing(OrgtilknytningState::getGyldigFom))
                .filter(orgtilknytningState -> orgtilknytningState.getGyldigFom().isAfter(LocalDate.now()))
                .filter(OrgtilknytningState::getErDagligOppfolging)
                .findFirst();
            dagligOppfolgningOrgTilknytning.ifPresent(orgtilknytningState -> logger.info(
                "Fant fremtidig DOL funnet for ressurs med navident {}, orgenhetId {}",
                ressurs.getNavident(),
                orgtilknytningState.getOrgEnhetId()
            ));
        }

        return NomIntegrasjonEntity.builder()
            .enhetsId(dagligOppfolgningOrgTilknytning.map(orgtilknytningState -> getRemedyEnhetId(orgtilknytningState.getOrgEnhetId())).orElse(null))
            .etternavn(capitalizeNavn(ressurs.getVisningsnavn().getEtternavn()).trim())
            .fodselsnummer(ressurs.getPersonident())
            .fornavn(capitalizeNavn(
                formatFornavnWithMellomnavn(ressurs.getVisningsnavn()).trim()
            ))
            .fysiskBrukerstedId(ressurs.getPrimaryFysiskBrukersted())
            .navId(ressurs.getNavident())
            .sektor(nomSektorToRemedyCode(dagligOppfolgningOrgTilknytning.map(OrgtilknytningState::getSektor).orElse(ressurs.getSektor())))
            .tiltredelsesdato(
                dagligOppfolgningOrgTilknytning.map(OrgtilknytningState::getGyldigFom).map(Utils::epochSecondsNorwegianTimeNoon).orElse(null)
            )
            .utlopsdato(dagligOppfolgningOrgTilknytning.map(OrgtilknytningState::getSektor).orElse(ressurs.getSektor()).equals(no.nav.nom.generated.model.Sektor.NAV_KOMMUNAL)
                || ressurs.getSektor().equals(no.nav.nom.generated.model.Sektor.NAV_STATLIG)
                ? null
                : dagligOppfolgningOrgTilknytning.map(OrgtilknytningState::getGyldigTom).map(Utils::epochSecondsNorwegianTimeEndOfDay).orElse(null)
            )
            .mobil(isNull(ressurs.getPrimaryTelefonnummer()) || ressurs.getPrimaryTelefonnummer().isBlank() ? null : ressurs.getPrimaryTelefonnummer())
            .masseflytting(dagligOppfolgningOrgTilknytning.map(OrgtilknytningState::getMasseflyttetFraFid).isPresent() ? "X" : null)
            .build();
    }

    private boolean isNavidentEksternEllerKommunalRessurs(String navident) {
        var orgtilknytning = getDolOrgtilknytningStateByNavident(navident);
        var sektor = orgtilknytning.map(OrgtilknytningState::getSektor).orElse(null);
        if (isNull(sektor)) {
            sektor = nomApiClient.getRessursByNavident(navident).getSektor();
        }
        return sektor.equals(no.nav.nom.generated.model.Sektor.EKSTERN)
            || sektor.equals(no.nav.nom.generated.model.Sektor.NAV_KOMMUNAL);
    }

    private boolean isFidEksternEllerKommunalRessurs(String fid) {
        var orgtilknytning = getDolOrgtilknytningStateByFid(fid);
        var sektor = orgtilknytning.map(OrgtilknytningState::getSektor).orElse(null);
        if (isNull(sektor)) {
            sektor = nomApiClient.getRessursByFid(fid).getSektor();
        }
        return sektor.equals(no.nav.nom.generated.model.Sektor.EKSTERN)
            || sektor.equals(no.nav.nom.generated.model.Sektor.NAV_KOMMUNAL);
    }

    private boolean isOrgtilknytningEksternEllerKommunalAndDagligOppfolgning(String orgtilknytningFid) {
        var orgtilknytning = nomApiClient.getOrgtilknytning(orgtilknytningFid);

        return orgtilknytning.getErDagligOppfolging() && isNavidentEksternEllerKommunalRessurs(orgtilknytning.getNavident());
    }

    protected void saveNewNomIntegrasjonEntity(NomIntegrasjonEntity newNomIntegrasjonEntity, String initiatedByNavident) {

        validateThatRequiredValuesAreSet(newNomIntegrasjonEntity);

        if (nomIntegrasjonEntityProvidesNewDataFromNom(newNomIntegrasjonEntity)) {
            nomIntegrasjonRepository.save(
                newNomIntegrasjonEntity.toBuilder()
                    .entryId(nextId())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .entryBestiller(initiatedByNavident)
                    .build()
            );
        }
    }

    private void markRessursForDeletion(String navident, String initiatedBy) {
        nomIntegrasjonRepository.save(
            NomIntegrasjonEntity.builder()
                .entryId(nextId())
                .navId(navident)
                .entryOperasjon("Slette")
                .entryStatus("Ubehandlet")
                .entryBestiller(initiatedBy)
                .build()
        );
    }

    private boolean nomIntegrasjonEntityProvidesNewDataFromNom(NomIntegrasjonEntity newNomIntegrasjonEntity) {
        return nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(newNomIntegrasjonEntity.getNavId())
            .map(latestEntityForNavident -> !(
                Objects.equals(latestEntityForNavident.getEnhetsId(), newNomIntegrasjonEntity.getEnhetsId())
                    && Objects.equals(latestEntityForNavident.getEtternavn(), newNomIntegrasjonEntity.getEtternavn())
                    && Objects.equals(latestEntityForNavident.getFodselsnummer(), newNomIntegrasjonEntity.getFodselsnummer())
                    && Objects.equals(latestEntityForNavident.getFornavn(), newNomIntegrasjonEntity.getFornavn())
                    && Objects.equals(latestEntityForNavident.getFysiskBrukerstedId(), newNomIntegrasjonEntity.getFysiskBrukerstedId())
                    && Objects.equals(latestEntityForNavident.getNavId(), newNomIntegrasjonEntity.getNavId())
                    && Objects.equals(latestEntityForNavident.getSektor(), newNomIntegrasjonEntity.getSektor())
                    && Objects.equals(latestEntityForNavident.getTiltredelsesdato(), newNomIntegrasjonEntity.getTiltredelsesdato())
                    && Objects.equals(latestEntityForNavident.getUtlopsdato(), newNomIntegrasjonEntity.getUtlopsdato())
                    && Objects.equals(latestEntityForNavident.getMobil(), newNomIntegrasjonEntity.getMobil())
                    && Objects.equals(latestEntityForNavident.getMasseflytting(), newNomIntegrasjonEntity.getMasseflytting())
                    && Objects.equals(latestEntityForNavident.getDirekteTelefon(), newNomIntegrasjonEntity.getDirekteTelefon())
            ))
            .orElse(true);
    }

    private void validateThatRequiredValuesAreSet(NomIntegrasjonEntity nomIntegrasjonEntity) {
        if (nomIntegrasjonEntity.getEnhetsId() == null) {
            throw RequiredValueMissingException.forField("enhetsId");
        }
        if (nomIntegrasjonEntity.getEtternavn() == null) {
            throw RequiredValueMissingException.forField("etternavn");
        }
        if (nomIntegrasjonEntity.getFodselsnummer() == null) {
            throw RequiredValueMissingException.forField("fodselsnummer");
        }
        if (nomIntegrasjonEntity.getFornavn() == null) {
            throw RequiredValueMissingException.forField("fornavn");
        }
        if (nomIntegrasjonEntity.getFysiskBrukerstedId() == null) {
            throw RequiredValueMissingException.forField("fysiskBrukerstedId");
        }
        if (nomIntegrasjonEntity.getNavId() == null) {
            throw RequiredValueMissingException.forField("navId");
        }
        if (nomIntegrasjonEntity.getSektor() == null) {
            throw RequiredValueMissingException.forField("sektor");
        }
        if (nomIntegrasjonEntity.getTiltredelsesdato() == null) {
            throw RequiredValueMissingException.forField("tiltredelsesdato");
        }

        if (nomIntegrasjonEntity.getSektor() == 2 && nomIntegrasjonEntity.getUtlopsdato() == null) {
            throw RequiredValueMissingException.forField("utløpsdato");
        }
    }

    private Optional<OrgtilknytningState> getDolOrgtilknytningStateByNavident(String navident) {
        var dagligOppfolgningOrgTilknytning = nomApiClient.getOrgtilknytningerForNavident(navident).stream()
            .filter(orgtilknytningState -> isActive(orgtilknytningState.getGyldigFom(), orgtilknytningState.getGyldigTom()))
            .filter(OrgtilknytningState::getErDagligOppfolging)
            .findFirst();

        if (dagligOppfolgningOrgTilknytning.isEmpty()) {
            logger.info("Ingen aktiv DOL funnet for ressurs med navident {}, prøver å finne fremtidig DOL", navident);
            dagligOppfolgningOrgTilknytning = nomApiClient.getOrgtilknytningerForNavident(navident).stream()
                .sorted(Comparator.comparing(OrgtilknytningState::getGyldigFom))
                .filter(orgtilknytningState -> orgtilknytningState.getGyldigFom().isAfter(LocalDate.now()))
                .filter(OrgtilknytningState::getErDagligOppfolging)
                .findFirst();
            dagligOppfolgningOrgTilknytning.ifPresent(orgtilknytningState -> logger.info(
                "Fant fremtidig DOL funnet for ressurs med navident {}, orgenhetId {}",
                navident,
                orgtilknytningState.getOrgEnhetId()
            ));
        }

        return dagligOppfolgningOrgTilknytning;
    }

    private Optional<OrgtilknytningState> getDolOrgtilknytningStateByFid(String fid) {
        return getDolOrgtilknytningStateByNavident(nomApiClient.getRessursByNavident(fid).getNavident());

    }

    private boolean isOrgtilknytningDOL(String fid) {
        return nomApiClient.getOrgtilknytning(fid).getErDagligOppfolging();
    }

    private String getRemedyEnhetId(String orgEnhetId) {
        return Optional.ofNullable(nomApiClient.getOrgEnhet(orgEnhetId).getRemedyEnhetId())
            .orElseThrow(() -> new RequiredValueMissingException(String.format("No remedyEnhetId set for orgEnhet with id %s", orgEnhetId)));
    }

    // https://nav-it.slack.com/archives/C040RV7KCH5/p1696498426739059
    protected String nextId() {
        return nomIntegrasjonRepository.findTopByOrderByEntryIdDesc()
            .map(latestEntry -> Long.parseLong(latestEntry.getEntryId()))
            .map(latestId -> fifteenCharLengthZeroPaddedNumber(latestId + 1))
            .orElse(fifteenCharLengthZeroPaddedNumber(0L));
    }

    public List<NomIntegrasjonEntity> getNomIntegrasjonEntities() {
        return nomIntegrasjonRepository.findAll().stream()
            .map(nomIntegrasjonEntity ->
                nomIntegrasjonEntity.toBuilder()
                    .fodselsnummer(
                        Optional.ofNullable(nomIntegrasjonEntity.getFodselsnummer())
                            .map(fodselsnummer -> fodselsnummer.replaceFirst(".{5}$", "*****"))
                            .orElse(null)
                    )
                    .build()
            )
            .toList();
    }

    private boolean erMasseflytting(InputMetadata inputMetadata) {
        Map<String, Object> inputInterfaceMethodArguments = objectMapper.convertValue(
            inputMetadata.inputInterfaceMethodArguments(),
            new TypeReference<>() {}
        );
        return (boolean) inputInterfaceMethodArguments.get("beholdTilgang");
    }

    protected Integer nomSektorToRemedyCode(no.nav.nom.generated.model.Sektor sektor) {
        return nomSektorToRemedyCode(Sektor.valueOf(sektor.getValue()));
    }

    protected Integer nomSektorToRemedyCode(Sektor sektor) {
        return switch (sektor) {
            case NAV_STATLIG -> 0;
            case NAV_KOMMUNAL -> 1;
            case EKSTERN -> 2;
        };
    }

    protected Sektor remedyCodeToNomSektor(Integer code) {
        return switch (code) {
            case 0 -> Sektor.NAV_STATLIG;
            case 1 -> Sektor.NAV_KOMMUNAL;
            case 2 -> Sektor.EKSTERN;
            default -> throw new IllegalArgumentException("Invalid code for Sektor");
        };
    }

    public void handleNomMasteredRessursEvent(String navIdent, LocalDateTime nomMasteredRessursEvent) {
        if(unleash.isEnabled("nom.sende-manuelle-mastringer-av-ressurs-til-remedy")){
            var ressursState = nomApiClient.getRessursByNavident(navIdent);
            var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);
            logger.info("Handling mastered ressurs event for navident {}, masterer at: {}, nomIntegrasjonEntity : {}",
                navIdent, nomMasteredRessursEvent, nomIntegrasjonEntityWithDefaultDataFromNom);
            // there is no user authentification context here, so we use the default user "nom-admin"
            saveNewNomIntegrasjonEntity(nomIntegrasjonEntityWithDefaultDataFromNom, "nom-admin");
        }
    }
}
