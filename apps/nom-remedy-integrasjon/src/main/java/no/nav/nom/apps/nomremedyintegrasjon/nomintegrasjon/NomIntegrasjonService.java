package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeEndOfDay;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;

@Service
public class NomIntegrasjonService {

    private final Logger logger = LoggerFactory.getLogger(NomIntegrasjonService.class);

    private final NomIntegrasjonRepository nomIntegrasjonRepository;

    private final NomApiClient nomApiClient;

    private final ObjectMapper objectMapper;

    public NomIntegrasjonService(NomIntegrasjonRepository nomIntegrasjonRepository, NomApiClient nomApiClient, ObjectMapper objectMapper) {
        this.nomIntegrasjonRepository = nomIntegrasjonRepository;
        this.nomApiClient = nomApiClient;
        this.objectMapper = objectMapper;
    }

    public void handleRessursEvent(RessursEvent ressursEvent) {
        if (ressursEvent instanceof RessursOpprettetEventV1 ressursOpprettetEventV1
            && ressursOpprettetEventV1.getRessursOpprettetEventData().sektor().equals(Sektor.EKSTERN)) {
            createRessurs(ressursOpprettetEventV1);
        } else if (ressursEvent instanceof RessursSluttetEventV1 ressursSluttetEventV1
            && isFidEksternRessurs(ressursEvent.getFid())
        ) {
            avsluttRessurs(ressursSluttetEventV1);
        } else if (ressursEvent instanceof RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertV1
            && isFidEksternRessurs(ressursEvent.getFid())
        ) {
            updateRessurs(ressursPersonaliaOppdatertV1);
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
        var coherentOrgtilknytning = nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid());

        nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(coherentOrgtilknytning.getNavident())
            .ifPresentOrElse(
                latestNomIntegrasjonEntityForNavident -> {
                    var gyldigTom = orgtilknytningDatoEndretEventData.gyldigTom()
                        .orElseThrow(() -> new NoSuchElementException("Missing value for 'sluttDato'"));

                    var newNomIntegrasjonEntity = latestNomIntegrasjonEntityForNavident.toBuilder()
                        .tiltredelsesdato(epochSecondsNorwegianTimeNoon(orgtilknytningDatoEndretEventData.gyldigFom()))
                        .utlopsdato(epochSecondsNorwegianTimeEndOfDay(gyldigTom))
                        .build();

                    if (!newNomIntegrasjonEntity.equals(latestNomIntegrasjonEntityForNavident)) {
                        saveNewNomIntegrasjonEntity(
                            newNomIntegrasjonEntity,
                            orgtilknytningDatoEndretEventV2.getMetadata().inputMetadata().origination().initiatedBy()
                        );
                        logger.info("Finished updating remedy data for event with fid {}", orgtilknytningDatoEndretEventV2.getFid());
                    } else {
                        logger.info("No new data in event with fid {}. Skipping update", orgtilknytningDatoEndretEventV2.getFid());
                    }

                },
                () -> logger.info(
                    "No existing entry found for navident associated with event with fid {}. No data written to remedy",
                    orgtilknytningDatoEndretEventV2.getFid()
                )
            );
    }

    private void updateOrgtilknytning(OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1) {
        logger.info("Updating remedy data for OrgtilknytningDagligOppfolgingEndretEventV1 with fid {}", orgtilknytningDagligOppfolgingEndretEventV1.getFid());

        var orgtilknytningDagligOppfolgingEndretEventData = orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData();

        var newRemedyEnhetId = getRemedyEnhetId(orgtilknytningDagligOppfolgingEndretEventData.orgEnhetId());

        nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(orgtilknytningDagligOppfolgingEndretEventData.navident())
            .ifPresentOrElse(
                latestNomIntegrasjonEntityForNavident -> {
                    var gyldigTom = orgtilknytningDagligOppfolgingEndretEventData.gyldigTom()
                        .orElseThrow(() -> new NoSuchElementException("Missing value for 'sluttDato'"));

                    var newNomIntegrasjonEntity = latestNomIntegrasjonEntityForNavident.toBuilder()
                        .enhetsId(newRemedyEnhetId)
                        .tiltredelsesdato(epochSecondsNorwegianTimeNoon(orgtilknytningDagligOppfolgingEndretEventData.gyldigFom()))
                        .utlopsdato(epochSecondsNorwegianTimeEndOfDay(gyldigTom))
                        .build();

                    if (!newNomIntegrasjonEntity.equals(latestNomIntegrasjonEntityForNavident)) {
                        saveNewNomIntegrasjonEntity(
                            newNomIntegrasjonEntity,
                            orgtilknytningDagligOppfolgingEndretEventV1.getMetadata().inputMetadata().origination().initiatedBy()
                        );
                        logger.info("Finished updating remedy data for event with fid {}", orgtilknytningDagligOppfolgingEndretEventV1.getFid());
                    } else {
                        logger.info("No new data in event with fid {}. Skipping update", orgtilknytningDagligOppfolgingEndretEventV1.getFid());
                    }
                },
                () -> logger.info(
                    "No existing entry found for navident associated with event with fid {}. No data written to remedy",
                    orgtilknytningDagligOppfolgingEndretEventV1.getFid()
                )
            );
    }

    private void avsluttRessurs(RessursSluttetEventV1 ressursSluttetEventV1) {
        logger.info("Updating remedy data for RessursSluttetEventV1 with fid {}", ressursSluttetEventV1.getFid());

        var coherentRessurs = nomApiClient.getRessursByFid(ressursSluttetEventV1.getFid());

        nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(coherentRessurs.getNavident()).ifPresentOrElse(
            latestNomIntegrasjonEntityForNavident -> {

                var newNomIntegrasjonEntity = latestNomIntegrasjonEntityForNavident.toBuilder()
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(ressursSluttetEventV1.getRessursSluttetEventData().sluttdato()))
                    .build();

                if (!newNomIntegrasjonEntity.equals(latestNomIntegrasjonEntityForNavident)) {
                    saveNewNomIntegrasjonEntity(
                        newNomIntegrasjonEntity.toBuilder()
                            .utlopsdato(epochSecondsNorwegianTimeEndOfDay(ressursSluttetEventV1.getRessursSluttetEventData().sluttdato()))
                            .build(),
                        ressursSluttetEventV1.getMetadata().inputMetadata().origination().initiatedBy()
                    );
                }
                else {
                    logger.info("Resource associated with event with fid {} is already tagged for deletion. Skipping update", ressursSluttetEventV1.getFid());
                }
            },
            () -> logger.info(
                "No existing entry found for navident associated with event with fid {}. No data written to remedy",
                ressursSluttetEventV1.getFid()
            )
        );

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
                .orElseThrow(() -> new NoSuchElementException("Missing value for 'orgEnhetId'"))
        );

        var sluttDato = Optional.ofNullable((String) inputInterfaceMethodArguments.get("sluttDato"))
            .map(LocalDate::parse)
            .orElseThrow(() -> new NoSuchElementException("Missing value for 'sluttDato'"));

        var fysiskBrukerstedNr = Optional.ofNullable((String) inputInterfaceMethodArguments.get("fysiskBrukerstedNr"))
            .orElseThrow(() -> new NoSuchElementException("Missing value for 'fysiskBrukerstedNr'"));

        NomIntegrasjonEntity newNomIntegrasjonEntity = NomIntegrasjonEntity.builder()
            .enhetsId(remedyEnhetsId)
            .etternavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().etternavn()))
            .fodselsnummer(ressursOpprettetEventData.personident())
            .fornavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().getFornavnMellomnavn()))
            .fysiskBrukerstedId(fysiskBrukerstedNr)
            .navId(ressursOpprettetAdditionalData.navident().orElseThrow(() -> new NoSuchElementException("Missing value for 'navident'")))
            .sektor(nomSektorToRemedyCode(ressursOpprettetEventData.sektor()))
            .tiltredelsesdato(ressursOpprettetAdditionalData.startdato()
                .map(Utils::epochSecondsNorwegianTimeNoon)
                .orElseThrow(() -> new NoSuchElementException("Missing value for 'startDato'")))
            .utlopsdato(epochSecondsNorwegianTimeEndOfDay(sluttDato))
            .build();

        saveNewNomIntegrasjonEntity(
            newNomIntegrasjonEntity,
            ressursOpprettetEventV1.getMetadata().inputMetadata().origination().initiatedBy()
        );

        logger.info("Created ressurs for fid: " + ressursOpprettetEventV1.getFid());
    }

    private void updateRessurs(RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertV1) {
        logger.info("Updating remedy data for OrgtilknytningDagligOppfolgingEndretEventV1 with fid {}", ressursPersonaliaOppdatertV1.getFid());

        var coherentRessurs = nomApiClient.getRessursByFid(ressursPersonaliaOppdatertV1.getFid());

        nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(coherentRessurs.getNavident())
            .ifPresentOrElse(
                latestNomIntegrasjonEntityForNavident -> {
                    var newVisningsnavn = ressursPersonaliaOppdatertV1.getPersonaliaOppdatertEventData().visningsnavn();

                    var newNomIntegrasjonEntity = latestNomIntegrasjonEntityForNavident.toBuilder()
                        .fornavn(capitalizeNavn(newVisningsnavn.getFornavnMellomnavn()))
                        .etternavn(capitalizeNavn(newVisningsnavn.etternavn()))
                        .build();

                    if (!newNomIntegrasjonEntity.equals(latestNomIntegrasjonEntityForNavident)) {

                        saveNewNomIntegrasjonEntity(
                            newNomIntegrasjonEntity,
                            ressursPersonaliaOppdatertV1.getMetadata().inputMetadata().origination().initiatedBy()
                        );

                        logger.info("Finished updating remedy data for event with fid {}", ressursPersonaliaOppdatertV1.getFid());
                    } else {

                        logger.info("No new data in event with fid {}. Skipping update", ressursPersonaliaOppdatertV1.getFid());
                    }
                },
                () -> logger.info(
                    "No existing entry found for navident associated with event with fid {}. No data written to remedy",
                    ressursPersonaliaOppdatertV1.getFid()
                )
            );
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

    private void saveNewNomIntegrasjonEntity(NomIntegrasjonEntity newNomIntegrasjonEntity, String initiatedByNavident) {
        nomIntegrasjonRepository.save(
            newNomIntegrasjonEntity.toBuilder()
                .entryId(nextId())
                .entryOperasjon("Ny")
                .entryStatus("Ubehandlet")
                .entryBestiller(initiatedByNavident)
                .build()
        );
    }

    private String getRemedyEnhetId(String orgEnhetId) {
        return Optional.ofNullable(nomApiClient.getOrgEnhet(orgEnhetId).getRemedyEnhetId())
            .orElseThrow(() -> new NoSuchElementException(String.format("No remedyEnhetId set for orgEnhet with id %s", orgEnhetId)));
    }

    // https://nav-it.slack.com/archives/C040RV7KCH5/p1696498426739059
    protected String nextId() {
        return nomIntegrasjonRepository.findTopByOrderByEntryIdDesc()
            .map(latestEntry -> Long.parseLong(latestEntry.getEntryId()))
            .map(latestId -> fifteenCharLengthZeroPaddedNumber(latestId + 1))
            .orElse(fifteenCharLengthZeroPaddedNumber(0L));
    }

    protected Integer nomSektorToRemedyCode(Sektor sektor) {
        return switch (sektor) {
            case NAV_STATLIG -> 0;
            case NAV_KOMMUNAL -> 1;
            case EKSTERN -> 2;
        };
    }
}
