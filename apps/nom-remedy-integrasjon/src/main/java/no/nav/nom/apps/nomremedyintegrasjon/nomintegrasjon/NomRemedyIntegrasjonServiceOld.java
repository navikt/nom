package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RequiredValueMissingException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.jpa.DelayedEventJpa;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.jpa.DelayedEventRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningSektorEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursJobbtelefonOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPrimaryTelefonnummerOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.Ressurs;
import no.nav.nom.generated.model.RessursState;
import no.nav.nom.generated.model.Telefon;
import no.nav.nom.generated.model.TelefonType;
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
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeEndOfDay;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.formatFornavnWithMellomnavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.isActive;
import static org.apache.kafka.common.utils.Utils.isBlank;

@Service
public class NomRemedyIntegrasjonServiceOld {

    private final Logger logger = LoggerFactory.getLogger(NomRemedyIntegrasjonServiceOld.class);

    private final RemedyIntegrasjonRepositoryService remedyIntegrasjonRepositoryService;

    private final NomApiClient nomApiClient;

    private final LeaderElection leaderElection;

    private final DelayedEventRepository delayedEventRepository;

    private final ObjectMapper objectMapper;
    private final Unleash unleash;


    public NomRemedyIntegrasjonServiceOld(
        RemedyIntegrasjonRepositoryService remedyIntegrasjonRepositoryService,
        NomApiClient nomApiClient,
        LeaderElection leaderElection,
        DelayedEventRepository delayedEventRepository,
        ObjectMapper objectMapper,
        Unleash unleash
    ) {
        this.remedyIntegrasjonRepositoryService = remedyIntegrasjonRepositoryService;
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
        }
        else if (ressursEvent instanceof RessursPrimaryTelefonnummerOppdatertEventV1 ressursPrimaryTelefonnummerOppdatertEventV1) {
            updateRessursTelefonnummer(ressursPrimaryTelefonnummerOppdatertEventV1);
        } else if (ressursEvent instanceof RessursJobbtelefonOppdatertEventV1 ressursJobbtelefonOppdatertEventV1) {
            //updateRessursTelefonnummer(ressursJobbtelefonOppdatertEventV1);
        }
    }

    public void handleOrgtilknytningEvent(OrgtilknytningEvent orgtilknytningEvent) {
        if (orgtilknytningEvent instanceof OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1
            && orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().dagligOppfolging()) {
            handleOrgtilknytningDagligOppfolgingEndretEvent(orgtilknytningDagligOppfolgingEndretEventV1);
        } else if (orgtilknytningEvent instanceof OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2
            && isOrgtilknytningDOL(orgtilknytningDatoEndretEventV2.getFid()) && !isOrgtilknytningHistorical(orgtilknytningDatoEndretEventV2)) {
            handleOrgtilknytningDatoEndretEvent(orgtilknytningDatoEndretEventV2);
        } else if (orgtilknytningEvent instanceof OrgtilknytningSektorEndretEventV1 orgtilknytningSektorEndretEventV1
            && isOrgtilknytningDOL(orgtilknytningSektorEndretEventV1.getFid())) {
            handleOrgtilknytningSektorEndretEvent(orgtilknytningSektorEndretEventV1);
        } else if (orgtilknytningEvent instanceof OrgtilknytningFjernetEventV1 orgtilknytningFjernetEventV1) {
            deleteOrgtilknytningEvents(orgtilknytningFjernetEventV1.getFid());
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
        if (orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigFom().isAfter(LocalDate.now())) {
            logger.info("Delaying remedy update for OrgtilknytningDatoEndretEventV2 with fid {}", orgtilknytningDatoEndretEventV2.getFid());

            delayEventUpdate(
                orgtilknytningDatoEndretEventV2,
                orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigFom()
            );
        } else {
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
        } else if (isNull(orgtilknytning.getGyldigTom()) || !orgtilknytning.getGyldigTom().isBefore(LocalDate.now())) {
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
                .anyMatch(NomRemedyIntegrasjonServiceOld::orgtilknytningIsActiveOnCurrentDate);
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

    private void handleDelayedOrgtiknytning(OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2) {
        logger.info("Handling delayed update of OrgtilknytningDatoEndretEventV2 with fid {}", orgtilknytningDatoEndretEventV2.getFid());

        try {
            var orgtilknytningState = nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid());

            if (orgtilknytningState.getErDagligOppfolging()
                && orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigFom().equals(orgtilknytningState.getGyldigFom())
                && orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigTom().equals(Optional.ofNullable(orgtilknytningState.getGyldigTom()))) {
                updateOrgtilknytning(orgtilknytningDatoEndretEventV2);
            }
        } catch (OrgtilknytningNotFoundException e) {
            logger.info("Delayed update of OrgtilknytningDatoEndretEventV2 cancelled. {}", e.getMessage());
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

    private void updateOrgtilknytning(OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2) {
        logger.info("Updating remedy data for OrgtilknytningDatoEndretEventV2 with fid {}", orgtilknytningDatoEndretEventV2.getFid());

        var orgtilknytningDatoEndretEventData = orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData();
        var orgtilknytningState = nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid());

        var ressursState = nomApiClient.getRessursByNavident(orgtilknytningState.getNavident());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        var remedyEnhetIdForOrgenhet = getRemedyEnhetId(orgtilknytningState.getOrgEnhetId());
        var nyTiltredelsesdato = epochSecondsNorwegianTimeNoon(orgtilknytningDatoEndretEventData.gyldigFom());

        var latestEntry = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(ressursState.getNavident());
        if (latestEntry.isPresent() && latestEntry.get().getTiltredelsesdato() != null && latestEntry.get().getTiltredelsesdato().equals(nyTiltredelsesdato)
        ) {
            logger.info("No changes detected for OrgtilknytningDatoEndretEventV2 with fid {}", orgtilknytningDatoEndretEventV2.getFid());
            return;
        }

        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .enhetsId(remedyEnhetIdForOrgenhet)
            .tiltredelsesdato(nyTiltredelsesdato)
            .build();

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
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

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
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

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
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

        var ressursState = nomApiClient.getRessursByFid(ressursSluttdatoOppdatertEventV1.getFid());
        if (ressursState.getSektor().equals(no.nav.nom.generated.model.Sektor.EKSTERN)) {
            updateUtlopsdatoEkstern(ressursSluttdatoOppdatertEventV1, ressursState);
        }

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

    private void updateUtlopsdatoEkstern(RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1, Ressurs ressursState) {
        var ressursSluttdatoOppdatertEventData = ressursSluttdatoOppdatertEventV1.getRessursSluttdatoOppdatertEventData();
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        ressursSluttdatoOppdatertEventData.sluttdato().ifPresent(sluttdato -> {
            if (sluttdato.isAfter(LocalDate.now())) {
                var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(sluttdato))
                    .build();

                remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
                    newNomIntegrasjonEntity,
                    ressursSluttdatoOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy()
                );
            }
        });
    }

    private void delayEventUpdate(Event event, LocalDate until) {
        delayedEventRepository.save(
            DelayedEventJpa.builder()
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
            if (!unleash.isEnabled("nom-remedy.ny-remedy-integrasjon")) {
                remedyIntegrasjonRepositoryService.markRessursForDeletion(
                    ressursState.getNavident(),
                    initiatedBy
                );
            }

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
            .filter(ressurs -> remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(ressurs.getNavident())
                .map(existingNomIntegrasjonEntityForRessurs ->
                    existingNomIntegrasjonEntityForRessurs.getEntryOperasjon().equals("Slette"))
                .orElse(true)
            ).toList();

        if (!unleash.isEnabled("nom-remedy.ny-remedy-integrasjon")) {
            ressurserToDeleteFromRemedy.forEach(ressursSomSkalSlettes ->
                remedyIntegrasjonRepositoryService.markRessursForDeletion(ressursSomSkalSlettes.getNavident(), initiatedBy)
            );
        }

        logger.info("Deleted following resssurser from remedy: {}", ressurserToDeleteFromRemedy.stream().map(RessursState::getNavident).toList());
    }

    protected void triggerDelayedEvents() {
        var delayedEventsReadyForHandling = delayedEventRepository.findByDelayedUntilLessThanEqual(LocalDate.now());

        delayedEventsReadyForHandling.forEach(this::handleDelayedEvent);

    }

    private void handleDelayedEvent(DelayedEventJpa delayedEventJpa) {
        var nomEvent = delayedEventJpa.getEvent();

        boolean deleteEvent = true;
        try {
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
        } catch (RequiredValueMissingException e) {
            logger.error("ALERT: Failed to handle delayed event: {}", delayedEventJpa, e);
            deleteEvent = false;
        }

        if (deleteEvent) {
            delayedEventRepository.deleteById(delayedEventJpa.getDelayedEventId());
        }
    }

    private void createRessurs(RessursOpprettetEventV1 ressursOpprettetEventV1) {
        var ressursOpprettetEventData = ressursOpprettetEventV1.getRessursOpprettetEventData();
        var ressursOpprettetAdditionalData = ressursOpprettetEventV1.getRessursOpprettetAdditionalData();
        var inputInterfaceMethodArguments = ressursOpprettetEventV1.getMetadata().inputMetadata().inputInterfaceMethodArguments();
        Map<String, Object> inputInterfaceMethodArgumentsMap = !isBlank(inputInterfaceMethodArguments.toString()) ? objectMapper.convertValue(
            inputInterfaceMethodArguments,
            new TypeReference<>() {}
        ) : Map.of();

        logger.info("Creating ressurs for RessursOpprettetEventV1 with fid: {}", ressursOpprettetEventV1.getFid());

        var remedyEnhetsId = getRemedyEnhetId(
            Optional.ofNullable((String) inputInterfaceMethodArgumentsMap.get("orgEnhetId"))
                .orElseThrow(() -> RequiredValueMissingException.forField("orgEnhetId"))
        );

        var sluttDato = Optional.ofNullable((String) inputInterfaceMethodArgumentsMap.get("sluttDato"))
            .filter(ressursSluttdato -> ressursOpprettetEventV1.getRessursOpprettetEventData().sektor().equals(Sektor.EKSTERN))
            .map(LocalDate::parse)
            .orElse(null);

        var fysiskBrukerstedNr = Optional.ofNullable((String) inputInterfaceMethodArgumentsMap.get("fysiskBrukerstedNr"))
            .orElseThrow(() -> RequiredValueMissingException.forField("fysiskBrukerstedNr"));

        var telefoner = getTelefonerFromNom(ressursOpprettetAdditionalData.navident().orElseThrow(() -> RequiredValueMissingException.forField("navId")));
        var mobil = telefoner.isEmpty() ? null : mapAndTrimNumber(telefoner.getFirst());

        NomIntegrasjonEntity newNomIntegrasjonEntity = NomIntegrasjonEntity.builder()
            .enhetsId(remedyEnhetsId)
            .etternavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().etternavn()).trim())
            .fodselsnummer(ressursOpprettetEventData.personident())
            .fornavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().getFornavnMellomnavn()).trim())
            .fysiskBrukerstedId(fysiskBrukerstedNr)
            .navId(ressursOpprettetAdditionalData.navident().get())
            .sektor(nomSektorToRemedyCode(ressursOpprettetEventData.sektor()))
            .tiltredelsesdato(ressursOpprettetAdditionalData.startdato()
                .map(Utils::epochSecondsNorwegianTimeNoon)
                .orElseThrow(() -> RequiredValueMissingException.forField("tiltredelsesdato")))
            .utlopsdato(ressursOpprettetEventData.sektor().erEkstern() && !isNull(sluttDato)? epochSecondsNorwegianTimeEndOfDay(sluttDato) : null)
            .mobil(mobil)
            .build();

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
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

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
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

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
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
        var telefoner = getTelefonerFromNom(ressursState.getNavident());
        var mobil = telefoner.isEmpty() ? null : mapAndTrimNumber(telefoner.getFirst());
        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .mobil(mobil)
            .build();

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            ressursPrimaryTelefonnummerOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy()
        );
    }

    private void updateRessursTelefonnummer(RessursJobbtelefonOppdatertEventV1 ressursJobbtelefonOppdatertEventV1) {
        logger.info(
            "Updating remedy data for {} with fid {}",
            ressursJobbtelefonOppdatertEventV1.getClass().getSimpleName(),
            ressursJobbtelefonOppdatertEventV1.getFid()
        );

        var ressursState = nomApiClient.getRessursByFid(ressursJobbtelefonOppdatertEventV1.getFid());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);
        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .mobil(ressursJobbtelefonOppdatertEventV1.getJobbtelefonOppdatertEventData().jobbtelefon().nummer())
            .build();

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            ressursJobbtelefonOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy()
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

        var telefoner = getTelefonerFromNom(ressurs.getNavident());
        var mobil = telefoner.isEmpty() ? null : mapAndTrimNumber(telefoner.getFirst());

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
            .mobil(mobil)
            .masseflytting(null)
            .build();
    }

    private boolean isOrgtilknytningDOL(String fid) {
        return nomApiClient.getOrgtilknytning(fid).getErDagligOppfolging();
    }

    private String getRemedyEnhetId(String orgEnhetId) {
        return Optional.ofNullable(nomApiClient.getOrgEnhet(orgEnhetId).getRemedyEnhetId())
            .orElseThrow(() -> new RequiredValueMissingException(String.format("No remedyEnhetId set for orgEnhet with id %s", orgEnhetId)));
    }

    public List<NomIntegrasjonEntity> getNomIntegrasjonEntities() {
        return remedyIntegrasjonRepositoryService.findAll().stream()
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

        return inputInterfaceMethodArguments.containsKey("beholdTilganger") ?
            (boolean) inputInterfaceMethodArguments.get("beholdTilganger") : (boolean) inputInterfaceMethodArguments.get("beholdTilgang");
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
            logger.info("Handling mastered ressurs event for navident {}, masterer at: {}",
                navIdent, nomMasteredRessursEvent);
            // there is no user authentification context here, so we use the default user "nom-admin"
            remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(nomIntegrasjonEntityWithDefaultDataFromNom, "nom-admin");
        }
    }

    public void lesStatePaaNytt(String entryBestiller, String navident) {
        var ressursState = nomApiClient.getRessursByNavident(navident);
        if (!ressursState.getIsNomMastered()) {
            logger.info("Ressurs with navident {} is not mastered, skipping", navident);
            return;
        }
        if (!isNull(ressursState.getSluttdato()) && ressursState.getSluttdato().isAfter(LocalDate.now())) {
            delayEventUpdate(
                new RessursSluttetEventV1(
                    ressursState.getFid(),
                    new RessursSluttetEventV1.RessursSluttetEventData(ressursState.getSluttdato()),
                    new InputMetadata(
                        "AdminController - NomIntegrasjon",
                        "lesOgEksekverRessursState",
                        null,
                        new Origination(entryBestiller, "lesOgEksekverRessursState")
                    )
                ),
                ressursState.getSluttdato().plusDays(1)
            );
        }
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);
        logger.info("Leser og eksekverer ressursState for navident {}", navident);
        // there is no user authentification context here, so we use the default user "nom-admin"
        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(nomIntegrasjonEntityWithDefaultDataFromNom, entryBestiller);
    }

    private List<Telefon> getTelefonerFromNom(String navident) {
        return nomApiClient.getTelefonnummere(navident).stream().sorted(
            (o1, o2) -> {
                if (o1.getType().equals(o2.getType())) return 0;
                if (o1.getType().equals(TelefonType.NAV_TJENESTE_TELEFON)) return -1;
                if (o2.getType().equals(TelefonType.NAV_TJENESTE_TELEFON)) return 1;
                if (o1.getType().equals(TelefonType.NAV_KONTOR_TELEFON)) return -1;
                if (o2.getType().equals(TelefonType.NAV_KONTOR_TELEFON)) return 1;
                if (o1.getType().equals(TelefonType.PRIVAT_TELEFON)) return -1;
                if (o2.getType().equals(TelefonType.PRIVAT_TELEFON)) return 1;
                return 0;
            }).toList();
    }

    private String mapAndTrimNumber(Telefon telefon) {
        if (telefon.getType().equals(TelefonType.PRIVAT_TELEFON)) return telefon.getNummer();
        var nummer = telefon.getNummer();
        if (nummer.length() >= 10) {
            if (nummer.startsWith("47")) {
                return nummer.substring(2);
            }
            if (nummer.startsWith("+47")) {
                return nummer.substring(3);
            }
        }
        return nummer;
    }
}
