package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RequiredValueMissingException;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.Ressurs;
import no.nav.nom.generated.model.RessursState;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeEndOfDay;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.formatFornavnWithMellomnavn;

@Service
public class NomIntegrasjonService {

    private final Logger logger = LoggerFactory.getLogger(NomIntegrasjonService.class);

    private final NomIntegrasjonRepository nomIntegrasjonRepository;

    private final NomApiClient nomApiClient;

    private final LeaderElection leaderElection;

    private final Unleash unleash;

    private final ObjectMapper objectMapper;


    public NomIntegrasjonService(
        NomIntegrasjonRepository nomIntegrasjonRepository, NomApiClient nomApiClient, LeaderElection leaderElection,
        Unleash unleash,
        ObjectMapper objectMapper
    ) {
        this.nomIntegrasjonRepository = nomIntegrasjonRepository;
        this.nomApiClient = nomApiClient;
        this.leaderElection = leaderElection;
        this.unleash = unleash;
        this.objectMapper = objectMapper;
    }

    // Run at 00:15 every night
    @Scheduled(cron = "0 15 0 * * *")
    public void findRessurserWithSluttdatoYesterdayAndDeleteThemFromRemedy() {
        if (leaderElection.isLeader()) {
            var yesterday = LocalDate.now().minusDays(1);
            logger.info("Sjekker om det finnes eksterne ressurser som sluttet på dato %s".formatted(yesterday));
            findRessurserWithSluttdatoAndMarkForDeletion(yesterday, "nom-job");
        }
    }

    public void handleRessursEvent(RessursEvent ressursEvent) {

        if (ressursEvent instanceof RessursOpprettetEventV1 ressursOpprettetEventV1
            && ressursOpprettetEventV1.getRessursOpprettetEventData().sektor().equals(Sektor.EKSTERN)) {
            createRessurs(ressursOpprettetEventV1);

        } else if (ressursEvent instanceof RessursSluttetEventV1 ressursSluttetEventV1
            && isFidEksternRessurs(ressursEvent.getFid())
        ) {
            handleRessursSluttetEventV1(ressursSluttetEventV1);

        } else if (ressursEvent instanceof RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1) {
            handleRessursSluttdatoOppdatertEventV1(ressursSluttdatoOppdatertEventV1);

        } else if (ressursEvent instanceof RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertV1
            && isFidEksternRessurs(ressursEvent.getFid())
        ) {
            updateRessursPersonalia(ressursPersonaliaOppdatertV1);

        } else if (ressursEvent instanceof RessursFysiskeBrukerstederOppdatertEventV1 ressursFysiskeBrukerstederOppdatertEventV1
            && isFidEksternRessurs(ressursEvent.getFid())) {
            updateRessursFysiskeBrukersteder(ressursFysiskeBrukerstederOppdatertEventV1);
        }
    }

    public void handleOrgtilknytningEvent(OrgtilknytningEvent orgtilknytningEvent) {

        if (orgtilknytningEvent instanceof OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1
            && orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().dagligOppfolging()
            && isNavidentEksternRessurs(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().navident())) {

            updateOrgtilknytning(orgtilknytningDagligOppfolgingEndretEventV1);
        } else if (orgtilknytningEvent instanceof OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2
            && isOrgtilknytningEksternAndDagligOppfolgning(orgtilknytningDatoEndretEventV2.getFid())) {

            updateOrgtilknytning(orgtilknytningDatoEndretEventV2);
        }
    }

    private void updateOrgtilknytning(OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2) {
        logger.info("Updating remedy data for OrgtilknytningDatoEndretEventV2 with fid {}", orgtilknytningDatoEndretEventV2.getFid());

        var orgtilknytningDatoEndretEventData = orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData();
        var orgtilknytningState = nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid());

        var ressursState = nomApiClient.getRessursByNavident(orgtilknytningState.getNavident());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        var gyldigTom = orgtilknytningDatoEndretEventData.gyldigTom()
            .orElseThrow(() -> new RequiredValueMissingException("Missing value for 'utlopsdato'"));

        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .tiltredelsesdato(epochSecondsNorwegianTimeNoon(orgtilknytningDatoEndretEventData.gyldigFom()))
            .utlopsdato(epochSecondsNorwegianTimeEndOfDay(gyldigTom))
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            orgtilknytningDatoEndretEventV2.getMetadata().inputMetadata().origination().initiatedBy()
        );
    }

    private void updateOrgtilknytning(OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1) {
        logger.info("Updating remedy data for OrgtilknytningDagligOppfolgingEndretEventV1 with fid {}", orgtilknytningDagligOppfolgingEndretEventV1.getFid());

        var orgtilknytningDagligOppfolgingEndretEventData = orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData();

        var ressursState = nomApiClient.getRessursByNavident(orgtilknytningDagligOppfolgingEndretEventData.navident());
        var nomIntegrasjonEntityWithDefaultDataFromNom = nomIntegrasjonEntityFromNomData(ressursState);

        var newRemedyEnhetId = getRemedyEnhetId(orgtilknytningDagligOppfolgingEndretEventData.orgEnhetId());

        var gyldigTom = orgtilknytningDagligOppfolgingEndretEventData.gyldigTom()
            .orElseThrow(() -> RequiredValueMissingException.forField("sluttDato"));

        var newNomIntegrasjonEntity = nomIntegrasjonEntityWithDefaultDataFromNom.toBuilder()
            .enhetsId(newRemedyEnhetId)
            .tiltredelsesdato(epochSecondsNorwegianTimeNoon(orgtilknytningDagligOppfolgingEndretEventData.gyldigFom()))
            .utlopsdato(epochSecondsNorwegianTimeEndOfDay(gyldigTom))
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            orgtilknytningDagligOppfolgingEndretEventV1.getMetadata().inputMetadata().origination().initiatedBy()
        );

    }

    private void handleRessursSluttetEventV1(RessursSluttetEventV1 ressursSluttetEventV1) {

        logger.info("Updating remedy data for RessursSluttetEventV1 with fid {}", ressursSluttetEventV1.getFid());

        var sluttdatoForRessurs = ressursSluttetEventV1.getRessursSluttetEventData().sluttdato();

        if (sluttdatoForRessurs.isBefore(LocalDate.now())) {
            var ressursState = nomApiClient.getRessursByFid(ressursSluttetEventV1.getFid());

            markRessursForDeletion(
                ressursState.getNavident(),
                ressursSluttetEventV1.getMetadata().inputMetadata().origination().initiatedBy()
            );

            logger.info("Deleted ressurs with fid {} and with sluttdato {}", ressursSluttetEventV1.getFid(), sluttdatoForRessurs);
        }
    }


    private void handleRessursSluttdatoOppdatertEventV1(RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1) {

        logger.info(
            "Updating remedy data for {} with fid {}",
            ressursSluttdatoOppdatertEventV1.getClass().getSimpleName(),
            ressursSluttdatoOppdatertEventV1.getFid()
        );

        ressursSluttdatoOppdatertEventV1.getRessursSluttdatoOppdatertEventData().sluttdato().ifPresent(
            sluttdatoForRessurs -> avsluttRessursInRemedy(
                ressursSluttdatoOppdatertEventV1.getFid(),
                sluttdatoForRessurs,
                ressursSluttdatoOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy()
                )
        );
    }

    private void avsluttRessursInRemedy(String ressursFid, LocalDate sluttdatoForRessurs, String initiatedBy) {

        if (!unleash.isEnabled("nom-remedy-integrasjon.delete_avsluttede_ressurser")) {
            return;
        }

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

        if (!unleash.isEnabled("nom-remedy-integrasjon.delete_avsluttede_ressurser")) {
            return;
        }

        var ressurserWithSluttdato = nomApiClient.getRessurser(
            no.nav.nom.generated.model.Sektor.EKSTERN,
            sluttdato
        );

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
            .map(LocalDate::parse)
            .orElseThrow(() -> RequiredValueMissingException.forField("sluttDato"));

        var fysiskBrukerstedNr = Optional.ofNullable((String) inputInterfaceMethodArguments.get("fysiskBrukerstedNr"))
            .orElseThrow(() -> RequiredValueMissingException.forField("fysiskBrukerstedNr"));

        NomIntegrasjonEntity newNomIntegrasjonEntity = NomIntegrasjonEntity.builder()
            .enhetsId(remedyEnhetsId)
            .etternavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().etternavn()))
            .fodselsnummer(ressursOpprettetEventData.personident())
            .fornavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().getFornavnMellomnavn()))
            .fysiskBrukerstedId(fysiskBrukerstedNr)
            .navId(ressursOpprettetAdditionalData.navident().orElseThrow(() -> RequiredValueMissingException.forField("navId")))
            .sektor(nomSektorToRemedyCode(ressursOpprettetEventData.sektor()))
            .tiltredelsesdato(ressursOpprettetAdditionalData.startdato()
                .map(Utils::epochSecondsNorwegianTimeNoon)
                .orElseThrow(() -> RequiredValueMissingException.forField("tiltredelsesdato")))
            .utlopsdato(epochSecondsNorwegianTimeEndOfDay(sluttDato))
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
            .fornavn(capitalizeNavn(newVisningsnavn.getFornavnMellomnavn()))
            .etternavn(capitalizeNavn(newVisningsnavn.etternavn()))
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

    private NomIntegrasjonEntity nomIntegrasjonEntityFromNomData(Ressurs ressurs) {

        var dagligOppfolgningOrgTilknytning = nomApiClient.getOrgtilknytningerForNavident(ressurs.getNavident()).stream()
            .filter(OrgtilknytningState::getErDagligOppfolging)
            .findFirst();

        return NomIntegrasjonEntity.builder()
            .enhetsId(getRemedyEnhetId(dagligOppfolgningOrgTilknytning.map(OrgtilknytningState::getOrgEnhetId).orElse(null)))
            .etternavn(capitalizeNavn(ressurs.getVisningsnavn().getEtternavn()))
            .fodselsnummer(ressurs.getPersonident())
            .fornavn(capitalizeNavn(
                formatFornavnWithMellomnavn(ressurs.getVisningsnavn())
            ))
            .fysiskBrukerstedId(ressurs.getPrimaryFysiskBrukersted())
            .navId(ressurs.getNavident())
            .sektor(nomSektorToRemedyCode(ressurs.getSektor()))
            .tiltredelsesdato(
                dagligOppfolgningOrgTilknytning.map(OrgtilknytningState::getGyldigFom).map(Utils::epochSecondsNorwegianTimeNoon).orElse(null)
            )
            .utlopsdato(
                dagligOppfolgningOrgTilknytning.map(OrgtilknytningState::getGyldigTom).map(Utils::epochSecondsNorwegianTimeEndOfDay).orElse(null)
            )
            .build();
    }

    private boolean isNavidentEksternRessurs(String navident) {
        return nomApiClient.getRessursByNavident(navident).getSektor()
            .equals(no.nav.nom.generated.model.Sektor.EKSTERN);
    }

    private boolean isFidEksternRessurs(String fid) {
        return nomApiClient.getRessursByFid(fid).getSektor()
            .equals(no.nav.nom.generated.model.Sektor.EKSTERN);
    }

    private boolean isOrgtilknytningEksternAndDagligOppfolgning(String orgtilknytningFid) {
        var orgtilknytning = nomApiClient.getOrgtilknytning(orgtilknytningFid);

        return orgtilknytning.getErDagligOppfolging() && isNavidentEksternRessurs(orgtilknytning.getNavident());
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
                latestEntityForNavident.getEnhetsId().equals(newNomIntegrasjonEntity.getEnhetsId())
                    && latestEntityForNavident.getEtternavn().equals(newNomIntegrasjonEntity.getEtternavn())
                    && latestEntityForNavident.getFodselsnummer().equals(newNomIntegrasjonEntity.getFodselsnummer())
                    && latestEntityForNavident.getFornavn().equals(newNomIntegrasjonEntity.getFornavn())
                    && latestEntityForNavident.getFysiskBrukerstedId().equals(newNomIntegrasjonEntity.getFysiskBrukerstedId())
                    && latestEntityForNavident.getNavId().equals(newNomIntegrasjonEntity.getNavId())
                    && latestEntityForNavident.getSektor().equals(newNomIntegrasjonEntity.getSektor())
                    && latestEntityForNavident.getTiltredelsesdato().equals(newNomIntegrasjonEntity.getTiltredelsesdato())
                    && latestEntityForNavident.getUtlopsdato().equals(newNomIntegrasjonEntity.getUtlopsdato())
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
        if (nomIntegrasjonEntity.getUtlopsdato() == null) {
            throw RequiredValueMissingException.forField("utlopsdato");
        }

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
}
