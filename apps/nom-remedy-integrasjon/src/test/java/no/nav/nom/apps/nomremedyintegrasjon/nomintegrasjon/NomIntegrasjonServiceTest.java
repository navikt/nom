package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.DelayedEvent;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPrimaryTelefonnummerOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.generated.model.OrgEnhet;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.Ressurs;
import no.nav.nom.generated.model.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.inputMetadata;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.nomIntegrasjonEntity;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgenhet;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDatoEndretEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDatoEndretEventV2;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningFjernetEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningState;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressurs;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursFysiskeBrukerstederOppdatertEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursFysiskeBrukerstederOppdatertEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetAdditionalData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursPersonaliaOppdatertV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursPrimaryTelefonnummerOppdatertV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursSluttdatoOppdatertEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursSluttetEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeEndOfDay;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.formatFornavnWithMellomnavn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class NomIntegrasjonServiceTest extends TestBase {

    @Autowired
    private NomIntegrasjonService nomIntegrasjonService;

    @MockBean
    private NomApiClient nomApiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearRepositories() {
        nomIntegrasjonRepository.deleteAll();
        delayedEventRepository.deleteAll();
    }

    @BeforeEach
    void mockNomApiClientDefault() {
        when(nomApiClient.getRessursByNavident(anyString()))
            .thenReturn(
                new Ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN)
            );
    }

    @ParameterizedTest(name = "Test should create ressurs for non NAV_STATLIG sektor")
    @MethodSource(value = "testShouldHandleRessursOpprettetEventIfNonStatligInput")
    @DisplayName("Should create new entry with correct data for RessursOpprettetEventV1")
    void testShouldCreateForRessursOpprettetEventV1(RessursOpprettetEventV1 ressursOpprettetEventV1) {
        Map<String, Object> inputInterfaceMethodArguments = objectMapper.convertValue(
            ressursOpprettetEventV1.getMetadata().inputMetadata().inputInterfaceMethodArguments(),
            new TypeReference<>() {}
        );
        var remedyEnhetId = "some-remedy-id";
        var fysiskBrukerstedNr = "0123";
        var sluttDato = Optional.ofNullable((String) inputInterfaceMethodArguments.get("sluttDato"))
            .map(LocalDate::parse)
            .orElse(null);

        var ressursOpprettetEventData = ressursOpprettetEventV1.getRessursOpprettetEventData();
        var ressursOpprettetAdditionalData = ressursOpprettetEventV1.getRessursOpprettetAdditionalData();
        var inputMetadata = ressursOpprettetEventV1.getMetadata();

        when(nomApiClient.getOrgEnhet(ressursOpprettetAdditionalData.orgEnhet().get()))
            .thenReturn(new OrgEnhet().remedyEnhetId(remedyEnhetId));

        nomIntegrasjonService.handleRessursEvent(ressursOpprettetEventV1);

        var nomIntegrasjonEntry = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(ressursOpprettetAdditionalData.navident().get());

        assertThat(nomIntegrasjonEntry)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(inputMetadata.inputMetadata().origination().initiatedBy())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(remedyEnhetId)
                    .etternavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().etternavn()))
                    .fodselsnummer(ressursOpprettetEventData.personident())
                    .fornavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().fornavn()))
                    .fysiskBrukerstedId(fysiskBrukerstedNr)
                    .navId(ressursOpprettetAdditionalData.navident().get())
                    .sektor(nomSektorToRemedyCode(ressursOpprettetEventV1.getRessursOpprettetEventData().sektor()))
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(ressursOpprettetAdditionalData.startdato().get()))
                    .utlopsdato((!ressursOpprettetEventV1.getRessursOpprettetEventData().sektor().equals(Sektor.EKSTERN) || isNull(sluttDato)) ? null : epochSecondsNorwegianTimeEndOfDay(sluttDato))
                    .mobil(ressursOpprettetEventData.primaryTelefonnummer().get())
                    .build()
            ));

    }

    private static Stream<Arguments> testShouldHandleRessursOpprettetEventIfNonStatligInput() {
        var navident = "X123456";
        var initiatedBy = "Z123456";
        var etternavn = "etternavn";
        var personident = "11111111111";
        var fornavn = "fornavn";
        var startDato = LocalDate.of(2023, 12, 12);
        var sluttDato = LocalDate.of(2024, 12, 12);
        var orgEnhetId = "some-orgenhet";
        var fysiskBrukerstedNr = "0123";
        var primaryTelefonnnummer = "12345678";

        var ressursOpprettetEventV1 = new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData().toBuilder()
                .personident(personident)
                .visningsnavn(new Visningsnavn(fornavn, Optional.empty(), etternavn))
                .sektor(Sektor.EKSTERN)
                .primaryTelefonnummer(Optional.of(primaryTelefonnnummer))
                .build(),
            inputMetadata().toBuilder()
                .origination(new Origination(initiatedBy, ""))
                .build(),
            ressursOpprettetAdditionalData().toBuilder()
                .orgEnhet(Optional.of(orgEnhetId))
                .navident(Optional.of(navident))
                .startdato(Optional.of(startDato))
                .build()
        );
        return Stream.of(
            Arguments.of(new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                ressursOpprettetEventV1.getRessursOpprettetEventData().toBuilder().sektor(Sektor.EKSTERN).build(),
                ressursOpprettetEventV1.getMetadata().inputMetadata().toBuilder().inputInterfaceMethodArguments(Map.of(
                    "orgEnhetId", orgEnhetId,
                    "sluttDato", sluttDato.toString(),
                    "fysiskBrukerstedNr", fysiskBrukerstedNr
                )).build(),
                ressursOpprettetEventV1.getRessursOpprettetAdditionalData()
            )),
            Arguments.of(new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                ressursOpprettetEventV1.getRessursOpprettetEventData().toBuilder().sektor(Sektor.NAV_KOMMUNAL).build(),
                ressursOpprettetEventV1.getMetadata().inputMetadata().toBuilder().inputInterfaceMethodArguments(Map.of(
                    "orgEnhetId", orgEnhetId,
                    "fysiskBrukerstedNr", fysiskBrukerstedNr,
                    "sluttDato", sluttDato.toString()
                )).build(),
                ressursOpprettetEventV1.getRessursOpprettetAdditionalData()
            ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testShouldDeleteRessursInput")
    @DisplayName("Should mark ressurs for deletion on RessursSluttetEventV1 and RessursSluttdatoOppdatertEventV1 with past sluttdato")
    void testShouldDeleteRessurs(RessursEvent ressursEvent) {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);

        when(nomApiClient.getRessursByFid(ressursEvent.getFid())).thenReturn(existingRessurs);

        nomIntegrasjonService.handleRessursEvent(ressursEvent);

        var nomIntegrasjonEntry = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(nomIntegrasjonEntry)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .navId(existingRessurs.getNavident())
                    .entryOperasjon("Slette")
                    .entryStatus("Ubehandlet")
                    .entryBestiller(ressursEvent.getMetadata().inputMetadata().origination().initiatedBy())
                    .build()
            ));
    }

    private static Stream<Arguments> testShouldDeleteRessursInput() {
        var yesterday = LocalDate.now().minusDays(1);

        return Stream.of(
            Arguments.of(ressursSluttetEventV1(yesterday)),
            Arguments.of(ressursSluttdatoOppdatertEventV1(yesterday))
        );
    }

    @MethodSource("testShouldDelayRessursEventsInput")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Should save delayed RessursEvent when future sluttdato")
    void testShouldDelayRessursEvents(RessursEvent ressursEvent, DelayedEvent expectedDelayedEvent) {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);

        when(nomApiClient.getRessursByFid(ressursEvent.getFid())).thenReturn(existingRessurs);

        nomIntegrasjonService.handleRessursEvent(ressursEvent);

        assertThat(nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident())).isEmpty();

        assertThat(delayedEventRepository.findAll())
            .hasSize(1)
            .first()
            .usingRecursiveComparison().ignoringFieldsOfTypes(UUID.class)
            .isEqualTo(expectedDelayedEvent);
    }

    private static Stream<Arguments> testShouldDelayRessursEventsInput() {
        var futureDate = LocalDate.now().plusDays(1);
        var ressursSluttetEvent = ressursSluttetEventV1(futureDate);
        var ressursSluttdatoOppdatertEventV1 = ressursSluttdatoOppdatertEventV1(futureDate);
        return Stream.of(
            Arguments.of(
                ressursSluttetEvent,
                DelayedEvent.builder()
                    .delayedUntil(futureDate.plusDays(1))
                    .event(ressursSluttetEvent).build()
            ),
            Arguments.of(
                ressursSluttdatoOppdatertEventV1,
                DelayedEvent.builder()
                    .delayedUntil(futureDate.plusDays(1))
                    .event(ressursSluttdatoOppdatertEventV1).build()
            )
        );
    }

    @Test
    @DisplayName("Should write new entity with correctly updated values for RessursPersonaliaOppdatertV1")
    void testShouldUpdateForRessursPersonaliaOppdatertV1() {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursPersonaliaOppdatertV1 = ressursPersonaliaOppdatertV1(
            new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
                new Visningsnavn("Charles", Optional.empty(), "Torgrimsson")
            )
        );

        when(nomApiClient.getRessursByFid(ressursPersonaliaOppdatertV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);

        nomIntegrasjonService.handleRessursEvent(ressursPersonaliaOppdatertV1);

        var newNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(newNomIntegrasjonEntityForNavident)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(ressursPersonaliaOppdatertV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(dagligOppfolgingOrgenhet.getRemedyEnhetId())
                    .etternavn(capitalizeNavn(ressursPersonaliaOppdatertV1.getPersonaliaOppdatertEventData().visningsnavn().etternavn()))
                    .fodselsnummer(existingRessurs.getPersonident())
                    .fornavn(capitalizeNavn(ressursPersonaliaOppdatertV1.getPersonaliaOppdatertEventData().visningsnavn().getFornavnMellomnavn()))
                    .fysiskBrukerstedId(existingRessurs.getPrimaryFysiskBrukersted())
                    .navId(existingRessurs.getNavident())
                    .sektor(2)
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(dagligOppfolgingOrgtilknytning.getGyldigTom()))
                    .mobil(existingRessurs.getPrimaryTelefonnummer())
                    .build()

            ));
    }

    @Test
    @DisplayName("Should write new entity with correctly updated values for RessursPersonaliaOppdatertV1 with no active DOL, but future DOL")
    void testShouldUpdateForWithNoActiveDOLRessursPersonaliaOppdatertV1() {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().gyldigFom(LocalDate.now().plusDays(2)).erDagligOppfolging(true);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursPersonaliaOppdatertV1 = ressursPersonaliaOppdatertV1(
            new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
                new Visningsnavn("Charles", Optional.empty(), "Torgrimsson")
            )
        );

        when(nomApiClient.getRessursByFid(ressursPersonaliaOppdatertV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);

        nomIntegrasjonService.handleRessursEvent(ressursPersonaliaOppdatertV1);

        var newNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(newNomIntegrasjonEntityForNavident)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(ressursPersonaliaOppdatertV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(dagligOppfolgingOrgenhet.getRemedyEnhetId())
                    .etternavn(capitalizeNavn(ressursPersonaliaOppdatertV1.getPersonaliaOppdatertEventData().visningsnavn().etternavn()))
                    .fodselsnummer(existingRessurs.getPersonident())
                    .fornavn(capitalizeNavn(ressursPersonaliaOppdatertV1.getPersonaliaOppdatertEventData().visningsnavn().getFornavnMellomnavn()))
                    .fysiskBrukerstedId(existingRessurs.getPrimaryFysiskBrukersted())
                    .navId(existingRessurs.getNavident())
                    .sektor(2)
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(dagligOppfolgingOrgtilknytning.getGyldigTom()))
                    .mobil(existingRessurs.getPrimaryTelefonnummer())
                    .build()

            ));
    }

    @Test
    @DisplayName("Should write new entity with correctly updated values for RessursPrimaryTelefonnummerOppdatertV1")
    void testShouldUpdateForRessursPrimaryTelefonnummerOppdatertV1() {
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursPrimaryTelefonnummerOppdatertV1 = ressursPrimaryTelefonnummerOppdatertV1(
            new RessursPrimaryTelefonnummerOppdatertEventV1.PrimaryTelefonnummerOppdatertEventData("87654321")
        );

        when(nomApiClient.getRessursByFid(ressursPrimaryTelefonnummerOppdatertV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);

        nomIntegrasjonService.handleRessursEvent(ressursPrimaryTelefonnummerOppdatertV1);

        var newNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(newNomIntegrasjonEntityForNavident)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(ressursPrimaryTelefonnummerOppdatertV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(dagligOppfolgingOrgenhet.getRemedyEnhetId())
                    .etternavn(capitalizeNavn(existingRessurs.getVisningsnavn().getEtternavn()))
                    .fodselsnummer(existingRessurs.getPersonident())
                    .fornavn(capitalizeNavn(existingRessurs.getVisningsnavn().getFornavn()))
                    .fysiskBrukerstedId(existingRessurs.getPrimaryFysiskBrukersted())
                    .navId(existingRessurs.getNavident())
                    .sektor(2)
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(dagligOppfolgingOrgtilknytning.getGyldigTom()))
                    .mobil(ressursPrimaryTelefonnummerOppdatertV1.getMobilnummerOppdatertEventData().primaryTelefonnummer())
                    .build()

            ));
    }

    @ParameterizedTest(name = "Should not handle RessursEvent if ressurs is NAV_STATLIG")
    @MethodSource("testShouldNotHandleRessursEventIfStatligInput")
    void testShouldNotHandleRessursEventIfNotEkstern(RessursEvent ressursEvent) {

        when(nomApiClient.getRessursByFid(ressursEvent.getFid()))
            .thenReturn(new Ressurs().sektor(no.nav.nom.generated.model.Sektor.NAV_STATLIG));

        nomIntegrasjonService.handleRessursEvent(ressursEvent);

        assertThat(nomIntegrasjonRepository.findAll()).isEmpty();
    }

    private static Stream<Arguments> testShouldNotHandleRessursEventIfStatligInput() {
        return Stream.of(
            Arguments.of(new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                ressursOpprettetEventData().toBuilder().sektor(Sektor.NAV_STATLIG).build(),
                inputMetadata(),
                ressursOpprettetAdditionalData()
            )),
            Arguments.of(ressursPersonaliaOppdatertV1()),
            Arguments.of(ressursFysiskeBrukerstederOppdatertEventV1())
        );
    }

    @Test
    @DisplayName("Should write new entity with correctly updated values for RessursFysiskeBrukerstederOppdatertEventV1")
    void testShouldUpdateForRessursFysiskeBrukerstederOppdatertEventV1() {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursFysiskeBrukerstederOppdatertEventV1 = ressursFysiskeBrukerstederOppdatertEventV1(
            ressursFysiskeBrukerstederOppdatertEventData().toBuilder()
                .primaryFysiskBrukersted("finse")
                .build()
        );

        when(nomApiClient.getRessursByFid(ressursFysiskeBrukerstederOppdatertEventV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);

        nomIntegrasjonService.handleRessursEvent(ressursFysiskeBrukerstederOppdatertEventV1);

        var newNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(newNomIntegrasjonEntityForNavident)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(ressursFysiskeBrukerstederOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(dagligOppfolgingOrgenhet.getRemedyEnhetId())
                    .etternavn(capitalizeNavn(existingRessurs.getVisningsnavn().getEtternavn()))
                    .fodselsnummer(existingRessurs.getPersonident())
                    .fornavn(capitalizeNavn(formatFornavnWithMellomnavn(existingRessurs.getVisningsnavn())))
                    .fysiskBrukerstedId(ressursFysiskeBrukerstederOppdatertEventV1.getRessursFysiskeBrukerstederOppdatertEventData().primaryFysiskBrukersted())
                    .navId(existingRessurs.getNavident())
                    .sektor(2)
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(dagligOppfolgingOrgtilknytning.getGyldigTom()))
                    .mobil(existingRessurs.getPrimaryTelefonnummer())
                    .build()
            ));
    }

    @Test
    @DisplayName("Should write new entity with correctly updated values for OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldUpdateForOrgtilknytningDagligOppfolgingEndretEventV1() {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var previousDagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true);
        var previousDagligOppfolgingOrgenhet = orgenhet();

        var newDagligOppfolgningRemedyEnhetId = "new-remedy-enhet-id";

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(existingRessurs.getNavident())
                .dagligOppfolging(true)
                .gyldigFom(LocalDate.now())
                .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
                .orgEnhetId("new-orgenhet")
                .build()
        );

        when(nomApiClient.getRessursByNavident(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().navident())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(previousDagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(previousDagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(previousDagligOppfolgingOrgenhet);

        when(nomApiClient.getOrgEnhet(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().orgEnhetId()))
            .thenReturn(new OrgEnhet().remedyEnhetId(newDagligOppfolgningRemedyEnhetId));

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        var newNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(newNomIntegrasjonEntityForNavident)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(orgtilknytningDagligOppfolgingEndretEventV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(newDagligOppfolgningRemedyEnhetId)
                    .etternavn(capitalizeNavn(existingRessurs.getVisningsnavn().getEtternavn()))
                    .fodselsnummer(existingRessurs.getPersonident())
                    .fornavn(capitalizeNavn(formatFornavnWithMellomnavn(existingRessurs.getVisningsnavn())))
                    .fysiskBrukerstedId(existingRessurs.getPrimaryFysiskBrukersted())
                    .navId(existingRessurs.getNavident())
                    .sektor(2)
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(
                        orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigFom())
                    )
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(
                        orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigTom().orElseThrow())
                    )
                    .mobil(existingRessurs.getPrimaryTelefonnummer())
                    .build()
            ));
    }

    @Test
    @DisplayName("Should delete OrgtilknytningEvents from local db with deleted orgtilknytning fid")
    void testShouldDeleteOrgtilknytningEventsFromDb() {
        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1();
        var orgtilknytningFjernetEventV1 = orgtilknytningFjernetEventV1(orgtilknytningDagligOppfolgingEndretEventV1.getFid());

        delayedEventRepository.save(
            DelayedEvent.builder()
                .delayedEventId(UUID.randomUUID())
                .delayedUntil(LocalDate.now())
                .event(orgtilknytningDagligOppfolgingEndretEventV1)
                .build()
        );

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningFjernetEventV1);
       assertThat(delayedEventRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should write new entity with correctly updated values for OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldNotSendEventToRemedyTable() {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var previousDagligOppfolgingOrgtilknytning = orgtilknytningState()
            .gyldigFom(LocalDate.now().minusYears(1))
            .gyldigTom(LocalDate.now().minusMonths(2));
        var previousDagligOppfolgingOrgenhet = orgenhet();

        var newDagligOppfolgningRemedyEnhetId = "new-remedy-enhet-id";

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(existingRessurs.getNavident())
                .dagligOppfolging(true)
                .gyldigFom(LocalDate.now().minusYears(1))
                .gyldigTom(Optional.of(LocalDate.now().minusMonths(1)))
                .orgEnhetId("new-orgenhet")
                .build()
        );

        when(nomApiClient.getRessursByNavident(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().navident())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(previousDagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(previousDagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(previousDagligOppfolgingOrgenhet);

        when(nomApiClient.getOrgEnhet(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().orgEnhetId()))
            .thenReturn(new OrgEnhet().remedyEnhetId(newDagligOppfolgningRemedyEnhetId));

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        var newNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(newNomIntegrasjonEntityForNavident).isEmpty();
    }

    @Test
    @DisplayName("Should skip handling OrgtilknytningDagligOppfolgingEndretEventV1 if ressurs is not EKSTERN")
    void testSkipUpdateForOrgtilknytningDagligOppfolgingEndretEventV1IfRessursIsNotEkstern() {

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder().dagligOppfolging(true).build()
        );

        when(nomApiClient.getRessursByNavident(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().navident()))
            .thenReturn(new Ressurs().sektor(no.nav.nom.generated.model.Sektor.NAV_STATLIG));

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        assertThat(nomIntegrasjonRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should skip handling OrgtilknytningDagligOppfolgingEndretEventV1 if orgtilknytning is not dagligOppfolgning")
    void testSkipUpdateForOrgtilknytningDagligOppfolgingEndretEventV1IfRessursIsNotDagligOppfolgning() {

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder().dagligOppfolging(false).build()
        );

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        assertThat(nomIntegrasjonRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should delay remedy update for OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldDelayUpdateForOrgtilknytningDagligOppfolgingEndretEventV1() {
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(existingRessurs.getNavident())
                .gyldigFom(LocalDate.now().plusDays(1))
                .orgEnhetId("new-orgenhet")
                .build()
        );

        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident()))
            .thenReturn(List.of(orgtilknytningState()
                .navident(existingRessurs.getNavident())
                .orgEnhetId("old-orgenhet")
                .gyldigFom(LocalDate.now().minusDays(1))
                .gyldigTom(LocalDate.now())
            ));

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);
        assertThat(delayedEventRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Should delay remedy update for OrgtilknytningDatoEndretEventV2")
    void testShouldDelayUpdateForOrgtilknytningDatoEndretEventV2 () {
        var orgtilknytningDatoEndretEventV2 = orgtilknytningDatoEndretEventV2(
            orgtilknytningDatoEndretEventData().toBuilder()
                .gyldigFom(LocalDate.now().plusDays(1))
                .build());

        when(nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid())).thenReturn(orgtilknytningState().gyldigFom(LocalDate.now().minusDays(1)).gyldigTom(LocalDate.now()));

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDatoEndretEventV2);

        assertThat(delayedEventRepository.findAll()).hasSize(1);
        assertThat(nomIntegrasjonRepository.findAll()).hasSize(0);
    }

    @Test
    @DisplayName("Should update for delayed OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldUpdateForDelayedOrgtilknytningDagligOppfolgingEndretEventV1() {
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var orgtilknytning = orgtilknytningState();
        var orgtilknytningEvent = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(existingRessurs.getNavident())
                .orgEnhetId(orgtilknytning.getOrgEnhetId())
                .gyldigTom(Optional.of(orgtilknytning.getGyldigTom()))
                .build()
        );

        delayedEventRepository.save(
            DelayedEvent.builder()
                .delayedEventId(UUID.randomUUID())
                .delayedUntil(LocalDate.now())
                .event(orgtilknytningEvent)
                .build());

        when(nomApiClient.getOrgtilknytning(orgtilknytningEvent.getFid())).thenReturn(orgtilknytning);
        when(nomApiClient.getRessursByNavident(existingRessurs.getNavident())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(orgtilknytning));
        when(nomApiClient.getOrgEnhet(orgtilknytning.getOrgEnhetId())).thenReturn(orgenhet());

        nomIntegrasjonService.triggerDelayedEvents();

        assertThat(nomIntegrasjonRepository.findAll()).hasSize(1);
        assertThat(delayedEventRepository.findAll()).hasSize(0);
    }

    @Test
    @DisplayName("Should update for delayed OrgtilknytningDatoEndretEventV2")
    void testShouldUpdateForDelayedOrgtilknytningDatoEndretEventV2() {
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var orgtilknytning = orgtilknytningState();
        var orgtilknytningEvent = orgtilknytningDatoEndretEventV2(
            orgtilknytningDatoEndretEventData().toBuilder()
                .gyldigTom(Optional.of(orgtilknytning.getGyldigTom()))
                .build()
        );

        delayedEventRepository.save(
            DelayedEvent.builder()
                .delayedEventId(UUID.randomUUID())
                .delayedUntil(LocalDate.now())
                .event(orgtilknytningEvent)
                .build());

        when(nomApiClient.getOrgtilknytning(orgtilknytningEvent.getFid())).thenReturn(orgtilknytning);
        when(nomApiClient.getRessursByNavident(existingRessurs.getNavident())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(orgtilknytning));
        when(nomApiClient.getOrgEnhet(orgtilknytning.getOrgEnhetId())).thenReturn(orgenhet());

        nomIntegrasjonService.triggerDelayedEvents();

        assertThat(nomIntegrasjonRepository.findAll()).hasSize(1);
        assertThat(delayedEventRepository.findAll()).hasSize(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testShouldNotUpdateOrgtilknytningDolEventInput")
    @DisplayName("Should not update remedy for delayed OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldNotUpdateForDelayedOrgtilknytningDagligOppfolgingEndretEventV1(
        DelayedEvent delayedEvent, OrgtilknytningState orgtilknytning, Ressurs existingRessurs
    ) {
        var orgtilknytningDolEvent = (OrgtilknytningDagligOppfolgingEndretEventV1)delayedEvent.getEvent();
        delayedEventRepository.save(delayedEvent);

        when(nomApiClient.getOrgtilknytning(orgtilknytningDolEvent.getFid())).thenReturn(orgtilknytning);

        nomIntegrasjonService.triggerDelayedEvents();

        assertThat(delayedEventRepository.findAll()).hasSize(0);
        assertThat(delayedEventRepository.findAll()).hasSize(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testShouldNotUpdateOrgtilknytningEventInput")
    @DisplayName("Should not update remedy for delayed OrgtilknytningDatoEndretEventV2")
    void testShouldNotUpdateForDelayedOrgtilknytningEndretEventV1(
        DelayedEvent delayedEvent, OrgtilknytningState orgtilknytning, Ressurs existingRessurs
    ) {
        var orgtilknytningDolEvent = (OrgtilknytningDatoEndretEventV2)delayedEvent.getEvent();
        delayedEventRepository.save(delayedEvent);

        when(nomApiClient.getOrgtilknytning(orgtilknytningDolEvent.getFid())).thenReturn(orgtilknytning);

        nomIntegrasjonService.triggerDelayedEvents();

        assertThat(delayedEventRepository.findAll()).hasSize(0);
        assertThat(delayedEventRepository.findAll()).hasSize(0);
    }

    private static Stream<Arguments> testShouldNotUpdateOrgtilknytningDolEventInput() {
        var orgtilknytning = orgtilknytningState();
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);

        return Stream.of(
            Arguments.of(
                DelayedEvent.builder()
                    .delayedEventId(UUID.randomUUID())
                    .delayedUntil(LocalDate.now())
                    .event(
                        orgtilknytningDagligOppfolgingEndretEventV1(
                            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                                .navident(existingRessurs.getNavident())
                                .orgEnhetId(orgtilknytning.getOrgEnhetId())
                                .gyldigTom(Optional.of(orgtilknytning.getGyldigTom().plusDays(1)))
                                .build()))
                    .build(),
                orgtilknytning,
                existingRessurs
            ),
            Arguments.of(
                DelayedEvent.builder()
                    .delayedEventId(UUID.randomUUID())
                    .delayedUntil(LocalDate.now())
                    .event(
                        orgtilknytningDagligOppfolgingEndretEventV1(
                            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                                .navident(existingRessurs.getNavident())
                                .orgEnhetId(orgtilknytning.getOrgEnhetId())
                                .gyldigTom(Optional.of(orgtilknytning.getGyldigTom().minusDays(1)))
                                .build()))
                    .build(),
                orgtilknytning,
                existingRessurs
            )
        );
    }

    private static Stream<Arguments> testShouldNotUpdateOrgtilknytningEventInput() {
        var orgtilknytning = orgtilknytningState();
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);

        return Stream.of(
            Arguments.of(
                DelayedEvent.builder()
                    .delayedEventId(UUID.randomUUID())
                    .delayedUntil(LocalDate.now())
                    .event(
                        orgtilknytningDatoEndretEventV2(
                            orgtilknytningDatoEndretEventData().toBuilder()
                                .gyldigTom(Optional.of(orgtilknytning.getGyldigTom().plusDays(1)))
                                .build()))
                    .build(),
                orgtilknytning,
                existingRessurs
            ),
            Arguments.of(
                DelayedEvent.builder()
                    .delayedEventId(UUID.randomUUID())
                    .delayedUntil(LocalDate.now())
                    .event(
                        orgtilknytningDatoEndretEventV2(
                            orgtilknytningDatoEndretEventData().toBuilder()
                                .gyldigTom(Optional.of(orgtilknytning.getGyldigTom().minusDays(1)))
                                .build()))
                    .build(),
                orgtilknytning,
                existingRessurs
            )
        );
    }

    @Test
    @DisplayName("Should write new entity with correctly updated values for OrgtilknytningDatoEndretEventV2")
    void testShouldUpdateForOrgtilknytningDatoEndretEventV2() {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState()
            .navident(existingRessurs.getNavident())
            .erDagligOppfolging(true);
        var dagligOppfolgingOrgenhet = orgenhet();

        var orgtilknytningDatoEndretEventV2 = orgtilknytningDatoEndretEventV2(
            orgtilknytningDatoEndretEventData().toBuilder()
                .gyldigFom(dagligOppfolgingOrgtilknytning.getGyldigFom())
                .gyldigTom(Optional.of(dagligOppfolgingOrgtilknytning.getGyldigTom().plusDays(1)))
                .build()
        );

        when(nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid()))
            .thenReturn(dagligOppfolgingOrgtilknytning);
        when(nomApiClient.getOrgtilknytningerForNavident(dagligOppfolgingOrgtilknytning.getNavident()))
            .thenReturn(List.of(dagligOppfolgingOrgtilknytning));

        when(nomApiClient.getRessursByNavident(dagligOppfolgingOrgtilknytning.getNavident()))
            .thenReturn(existingRessurs);

        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId()))
            .thenReturn(dagligOppfolgingOrgenhet);

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDatoEndretEventV2);

        var newNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(
            dagligOppfolgingOrgtilknytning.getNavident()
        );

        assertThat(newNomIntegrasjonEntityForNavident)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(orgtilknytningDatoEndretEventV2.getMetadata().inputMetadata().origination().initiatedBy())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(dagligOppfolgingOrgenhet.getRemedyEnhetId())
                    .etternavn(capitalizeNavn(existingRessurs.getVisningsnavn().getEtternavn()))
                    .fodselsnummer(existingRessurs.getPersonident())
                    .fornavn(capitalizeNavn(formatFornavnWithMellomnavn(existingRessurs.getVisningsnavn())))
                    .fysiskBrukerstedId(existingRessurs.getPrimaryFysiskBrukersted())
                    .navId(existingRessurs.getNavident())
                    .sektor(2)
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(
                        orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigFom())
                    )
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(
                        orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigTom().orElseThrow())
                    )
                    .mobil(ressurs().getPrimaryTelefonnummer())
                    .build()
            ));
    }

    @Test
    @DisplayName("Should skip handling OrgtilknytningDatoEndretEventV2 if ressurs is not EKSTERN")
    void testSkipUpdateForOrgtilknytningDatoEndretEventV2IfRessursIsNotEkstern() {
        var navident = "X123456";

        var orgtilknytningDatoEndretEventV2 = orgtilknytningDatoEndretEventV2();

        when(nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid()))
            .thenReturn(new OrgtilknytningState().navident(navident).erDagligOppfolging(true));

        when(nomApiClient.getRessursByNavident(navident)).thenReturn(
            new Ressurs().sektor(no.nav.nom.generated.model.Sektor.NAV_STATLIG)
        );

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDatoEndretEventV2);

        assertThat(nomIntegrasjonRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should skip updating values for OrgtilknytningDatoEndretEventV2 if orgtilknytning is not dagligOppfolgning")
    void testSkipUpdateForOrgtilknytningDatoEndretEventV2IfRessursIsNotDagligOppfolgning() {

        var orgtilknytningDatoEndretEventV2 = orgtilknytningDatoEndretEventV2();

        when(nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid()))
            .thenReturn(new OrgtilknytningState().erDagligOppfolging(false));

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDatoEndretEventV2);

        assertThat(nomIntegrasjonRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should not write new NomIntegrasjonEntity if it provides no new data from nom")
    void testWriteIgnoredIfNoNewDataFromNom() {
        var navident = "X654321";

        var existingNomIntegrasjonEntity = nomIntegrasjonEntity().toBuilder()
            .navId(navident)
            .entryStatus("Behandlet")
            .build();

        nomIntegrasjonRepository.save(existingNomIntegrasjonEntity);

        var newNomIntegrasjonEntity = existingNomIntegrasjonEntity.toBuilder()
            .entryStatus("Ubehandlet")
            .build();

        nomIntegrasjonService.saveNewNomIntegrasjonEntity(newNomIntegrasjonEntity, navident);

        assertThat(nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navident))
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(existingNomIntegrasjonEntity));

    }

    @Test
    @DisplayName("Should find ressurser with sluttdato and mark for deletion")
    void testFindRessurserWithSluttdatoAndMarkForDeletion() {
        var sluttdato = LocalDate.now();
        var navidentForRessursToDelete = "X12345";
        var initiatedBy = "initiator";

        when(nomApiClient.getRessurser(no.nav.nom.generated.model.Sektor.EKSTERN, sluttdato))
            .thenReturn(List.of(
                new RessursState().navident(navidentForRessursToDelete)
            ));

        nomIntegrasjonService.findRessurserWithSluttdatoAndMarkForDeletion(sluttdato, initiatedBy);

        assertThat(nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navidentForRessursToDelete))
            .contains(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .navId(navidentForRessursToDelete)
                    .entryOperasjon("Slette")
                    .entryStatus("Ubehandlet")
                    .entryBestiller(initiatedBy)
                    .build()
            );
    }

    @ParameterizedTest(name = "Delayed event: {0} \nfor ressurs: {1} \nresults in nom integrasjon entity: {2}")
    @DisplayName("Should handle delayed RessursEvent")
    @MethodSource("testDelayedDeleteRessursInput")
    void testDelayedDeleteRessurs(
        DelayedEvent delayedEvent, Ressurs existingRessurs, NomIntegrasjonEntity expectedNomIntegrasjonEntity
    ) {

        when(nomApiClient.getRessursByFid(delayedEvent.getEvent().getFid())).thenReturn(existingRessurs);

        delayedEventRepository.saveAll(List.of(delayedEvent));

        nomIntegrasjonService.triggerDelayedEvents();

        assertThat(nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident()).orElse(null))
            .usingRecursiveComparison().ignoringFields("entryId")
            .isEqualTo(expectedNomIntegrasjonEntity);

        assertThat(delayedEventRepository.findAll()).isEmpty();
    }

    private static Stream<Arguments> testDelayedDeleteRessursInput() {
        var yesterday = LocalDate.now().minusDays(1);
        var anotherDay = LocalDate.now().plusDays(5);

        var existingRessursWithSluttdatoYesterday = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN).sluttdato(yesterday);
        var existingRessursWithSluttdatoAnotherDay = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN).sluttdato(anotherDay);

        var ressursSluttetEventV1 = ressursSluttetEventV1(yesterday);
        var ressursSluttdatoOppdatertEventV1 = ressursSluttdatoOppdatertEventV1(yesterday);

        var nomIntegrasjonEntityForDeletingRessurs = NomIntegrasjonEntity.builder()
            .navId(existingRessursWithSluttdatoYesterday.getNavident())
            .entryOperasjon("Slette")
            .entryStatus("Ubehandlet")
            .build();

        return Stream.of(
            Arguments.of(
                DelayedEvent.builder().delayedUntil(yesterday).event(ressursSluttetEventV1).build(),
                existingRessursWithSluttdatoYesterday,
                nomIntegrasjonEntityForDeletingRessurs.toBuilder()
                    .entryBestiller(ressursSluttetEventV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .build()
            ),
            Arguments.of(
                DelayedEvent.builder().delayedUntil(yesterday).event(ressursSluttdatoOppdatertEventV1).build(),
                existingRessursWithSluttdatoYesterday,
                nomIntegrasjonEntityForDeletingRessurs.toBuilder()
                    .entryBestiller(ressursSluttdatoOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .build()
            ),
            // Tester at man ikke sletter ressurs dersom ressursen har en annen sluttdato enn spesifisert i den forsinkede hendelsen
            Arguments.of(
                DelayedEvent.builder().delayedUntil(yesterday).event(ressursSluttetEventV1).build(),
                existingRessursWithSluttdatoAnotherDay,
                null
            ),
            Arguments.of(
                DelayedEvent.builder().delayedUntil(yesterday).event(ressursSluttdatoOppdatertEventV1).build(),
                existingRessursWithSluttdatoAnotherDay,
                null
            )
        );
    }

    @ParameterizedTest(name = "latestId: {0} expectedNextId: {1}")
    @MethodSource("testNextIdArguments")
    void testNextId(String latestId, String expectedNextId) {
        if (latestId != null) {
            nomIntegrasjonRepository.save(NomIntegrasjonEntity.builder().entryId(latestId).build());
        }
        assertThat(nomIntegrasjonService.nextId()).isEqualTo(expectedNextId);
    }

    private static Stream<Arguments> testNextIdArguments() {
        return Stream.of(
            Arguments.of("000000000000001", "000000000000002"),
            Arguments.of("000000000000002", "000000000000003"),
            Arguments.of("000000000000010", "000000000000011"),
            Arguments.of("010000000000000", "010000000000001"),
            Arguments.of(null, "000000000000000")
        );
    }

    @ParameterizedTest(name = "Should capitalize {0} correctly")
    @DisplayName("Should capitalize navn correctly")
    @MethodSource("testCapitalizeNavnArguments")
    void testCapitalizeNavn(String input, String expected) {
        assertThat(capitalizeNavn(input)).isEqualTo(expected);
    }

    private static Stream<Arguments> testCapitalizeNavnArguments() {
        return Stream.of(
            Arguments.of("ola", "Ola"),
            Arguments.of("ola n", "Ola N"),
            Arguments.of("Anne-lise", "Anne-Lise"),
            Arguments.of("Anne-lise næset", "Anne-Lise Næset"),
            Arguments.of("anne-lise næSet", "Anne-Lise Næset")
        );
    }

    private Integer nomSektorToRemedyCode(Sektor sektor) {
        return switch (sektor) {
            case NAV_STATLIG -> 0;
            case NAV_KOMMUNAL -> 1;
            case EKSTERN -> 2;
        };
    }
}
