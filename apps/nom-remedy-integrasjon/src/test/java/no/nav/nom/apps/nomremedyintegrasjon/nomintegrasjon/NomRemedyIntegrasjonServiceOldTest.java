package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateStateRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.jpa.DelayedEventJpa;
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
import no.nav.nom.generated.model.Telefon;
import no.nav.nom.generated.model.TelefonType;
import no.nav.nom.generated.model.Tilknytningtype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class NomRemedyIntegrasjonServiceOldTest extends TestBase {

    private final static String UNLEASH_TOGGLE = "nom-remedy.ny-remedy-integrasjon";

    @Autowired
    private NomRemedyIntegrasjonServiceOld nomRemedyIntegrasjonServiceOld;

    @Autowired
    RemedyIntegrasjonRepositoryService remedyIntegrasjonRepositoryService;

    @MockitoBean
    NomApiClient nomApiClient;

    @Autowired
    private AggregateStateRepository aggregateStateRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearRepositories() {
        remedyIntegrasjonRepositoryService.deleteAll();
        delayedEventRepository.deleteAll();
        when(unleash.isEnabled(UNLEASH_TOGGLE)).thenReturn(false);
    }

    @BeforeEach
    void mockNomApiClientDefault() {
        when(nomApiClient.getRessursByNavident(anyString()))
            .thenReturn(
                new Ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN)
            );
    }

    @ParameterizedTest(name = "Test should create ressurs for all Sektor")
    @MethodSource(value = "testShouldHandleRessursOpprettetEventIfNonStatligInput")
    @DisplayName("Should create new entry with correct data for RessursOpprettetEventV1")
    void testShouldCreateForRessursOpprettetEventV1(RessursOpprettetEventV1 ressursOpprettetEventV1, List<Telefon> telefoner) {
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

        when(nomApiClient.getOrgEnhet(ressursOpprettetAdditionalData.orgEnhet()))
            .thenReturn(new OrgEnhet().remedyEnhetId(remedyEnhetId));
        when(nomApiClient.getTelefonnummere(ressursOpprettetEventV1.getRessursOpprettetAdditionalData().navident()))
            .thenReturn(telefoner);

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursOpprettetEventV1);

        var nomIntegrasjonEntry = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(ressursOpprettetAdditionalData.navident());
        var mobil = telefoner.isEmpty() ? null : telefoner.getFirst().getNummer();
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
                    .navId(ressursOpprettetAdditionalData.navident())
                    .sektor(nomSektorToRemedyCode(ressursOpprettetEventV1.getRessursOpprettetEventData().sektor()))
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(ressursOpprettetAdditionalData.startdato()))
                    .utlopsdato((!ressursOpprettetEventV1.getRessursOpprettetEventData().sektor().equals(Sektor.EKSTERN) || isNull(sluttDato)) ? null : epochSecondsNorwegianTimeEndOfDay(sluttDato))
                    .mobil(mobil)
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
                .primaryTelefonnummer(primaryTelefonnnummer)
                .build(),
            inputMetadata().toBuilder()
                .origination(new Origination(initiatedBy, ""))
                .build(),
            ressursOpprettetAdditionalData().toBuilder()
                .orgEnhet(orgEnhetId)
                .navident(navident)
                .startdato(startDato)
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
            ), getPrivatTelefon()),
            Arguments.of(new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                ressursOpprettetEventV1.getRessursOpprettetEventData().toBuilder().sektor(Sektor.NAV_KOMMUNAL).build(),
                ressursOpprettetEventV1.getMetadata().inputMetadata().toBuilder().inputInterfaceMethodArguments(Map.of(
                    "orgEnhetId", orgEnhetId,
                    "fysiskBrukerstedNr", fysiskBrukerstedNr,
                    "sluttDato", sluttDato.toString()
                )).build(),
                ressursOpprettetEventV1.getRessursOpprettetAdditionalData()
            ), getKontorOgPrivatTelefon()),
            Arguments.of(new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                ressursOpprettetEventV1.getRessursOpprettetEventData().toBuilder().sektor(Sektor.NAV_STATLIG).build(),
                ressursOpprettetEventV1.getMetadata().inputMetadata().toBuilder().inputInterfaceMethodArguments(Map.of(
                    "orgEnhetId", orgEnhetId,
                    "fysiskBrukerstedNr", fysiskBrukerstedNr,
                    "sluttDato", sluttDato.toString()
                )).build(),
                ressursOpprettetEventV1.getRessursOpprettetAdditionalData()
            ), getTjenesteOgKontorTelefon())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testShouldDeleteRessursInput")
    @DisplayName("Should mark ressurs for deletion on RessursSluttetEventV1 and RessursSluttdatoOppdatertEventV1 with past sluttdato")
    void testShouldDeleteRessurs(RessursEvent ressursEvent) {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);

        when(nomApiClient.getRessursByFid(ressursEvent.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgEnhet(any())).thenReturn(orgenhet());
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(orgtilknytningState()));
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(getPrivatTelefon());

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursEvent);

        var nomIntegrasjonEntry = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

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
    void testShouldDelayRessursEvents(RessursEvent ressursEvent, DelayedEventJpa expectedDelayedEventJpa) {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);

        when(nomApiClient.getRessursByFid(ressursEvent.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgEnhet(any())).thenReturn(orgenhet());
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(orgtilknytningState()));
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(List.of());

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursEvent);

        assertThat(delayedEventRepository.findAll())
            .hasSize(1)
            .first()
            .usingRecursiveComparison().ignoringFieldsOfTypes(UUID.class)
            .isEqualTo(expectedDelayedEventJpa);
    }

    private static Stream<Arguments> testShouldDelayRessursEventsInput() {
        var futureDate = LocalDate.now().plusDays(1);
        var ressursSluttetEvent = ressursSluttetEventV1(futureDate);
        var ressursSluttdatoOppdatertEventV1 = ressursSluttdatoOppdatertEventV1(futureDate);
        return Stream.of(
            Arguments.of(
                ressursSluttetEvent,
                DelayedEventJpa.builder()
                    .delayedUntil(futureDate.plusDays(1))
                    .event(ressursSluttetEvent).build()
            ),
            Arguments.of(
                ressursSluttdatoOppdatertEventV1,
                DelayedEventJpa.builder()
                    .delayedUntil(futureDate.plusDays(1))
                    .event(ressursSluttdatoOppdatertEventV1).build()
            )
        );
    }

    @ParameterizedTest
    @MethodSource("testAllSektorsAndTelefonVariants")
    @DisplayName("Should write new entity with correctly updated values for RessursPersonaliaOppdatertV1")
    void testShouldUpdateForRessursPersonaliaOppdatertV1(no.nav.nom.generated.model.Sektor sektor, List<Telefon> telefoner) {

        var existingRessurs = ressurs().sektor(sektor);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true).sektor(sektor);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursPersonaliaOppdatertV1 = ressursPersonaliaOppdatertV1(
            new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
                new Visningsnavn("Charles", Optional.empty(), "Torgrimsson")
            )
        );

        when(nomApiClient.getRessursByFid(ressursPersonaliaOppdatertV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(telefoner);

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursPersonaliaOppdatertV1);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());
        var mobil = telefoner.isEmpty() ? null : telefoner.getFirst().getNummer();

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
                    .sektor(nomSektorToRemedyCode(existingRessurs.getSektor()))
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(existingRessurs.getSektor().equals(no.nav.nom.generated.model.Sektor.EKSTERN) ? epochSecondsNorwegianTimeEndOfDay(getGyldigTomOrMax(dagligOppfolgingOrgtilknytning)) : null)
                    .mobil(mobil)
                    .build()

            ));
    }

    private static LocalDate getGyldigTomOrMax(OrgtilknytningState dagligOppfolgingOrgtilknytning) {
        return isNull(dagligOppfolgingOrgtilknytning.getGyldigTom()) ? LocalDate.MAX : dagligOppfolgingOrgtilknytning.getGyldigTom();
    }

    @ParameterizedTest
    @MethodSource("testAllSektorsAndTelefonVariants")
    @DisplayName("Should write new entity with correctly updated values for RessursPersonaliaOppdatertV1 with no active DOL, but future DOL")
    void testShouldUpdateForWithNoActiveDOLRessursPersonaliaOppdatertV1(no.nav.nom.generated.model.Sektor sektor, List<Telefon> telefoner) {

        var existingRessurs = ressurs().sektor(sektor);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().gyldigFom(LocalDate.now().plusDays(2)).erDagligOppfolging(true).sektor(sektor);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursPersonaliaOppdatertV1 = ressursPersonaliaOppdatertV1(
            new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
                new Visningsnavn("Charles", Optional.empty(), "Torgrimsson")
            )
        );

        when(nomApiClient.getRessursByFid(ressursPersonaliaOppdatertV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(telefoner);

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursPersonaliaOppdatertV1);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());
        var mobil = telefoner.isEmpty() ? null : telefoner.getFirst().getNummer();

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
                    .sektor(nomSektorToRemedyCode(existingRessurs.getSektor()))
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(existingRessurs.getSektor().equals(no.nav.nom.generated.model.Sektor.EKSTERN) ? epochSecondsNorwegianTimeEndOfDay(getGyldigTomOrMax(dagligOppfolgingOrgtilknytning)) : null)
                    .mobil(mobil)
                    .build()

            ));
    }

    @ParameterizedTest
    @MethodSource("testAllSektorsAndTelefonVariants")
    @DisplayName("Should write new entity with correctly updated values for RessursPrimaryTelefonnummerOppdatertV1")
    void testShouldUpdateForRessursPrimaryTelefonnummerOppdatertV1(no.nav.nom.generated.model.Sektor sektor, List<Telefon> telefoner) {
        var existingRessurs = ressurs().sektor(sektor);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true).sektor(sektor);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursPrimaryTelefonnummerOppdatertV1 = ressursPrimaryTelefonnummerOppdatertV1(
            new RessursPrimaryTelefonnummerOppdatertEventV1.PrimaryTelefonnummerOppdatertEventData("87654321")
        );

        when(nomApiClient.getRessursByFid(ressursPrimaryTelefonnummerOppdatertV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(telefoner);

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursPrimaryTelefonnummerOppdatertV1);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());
        var mobil = telefoner.isEmpty() ? null : telefoner.getFirst().getNummer();
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
                    .sektor(nomSektorToRemedyCode(existingRessurs.getSektor()))
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(existingRessurs.getSektor().equals(no.nav.nom.generated.model.Sektor.EKSTERN) ? epochSecondsNorwegianTimeEndOfDay(getGyldigTomOrMax(dagligOppfolgingOrgtilknytning)) : null)
                    .mobil(mobil)
                    .build()

            ));
    }

    @ParameterizedTest
    @MethodSource("testAllSektorsAndTelefonVariants")
    @DisplayName("Should write new entity with correctly updated values for RessursFysiskeBrukerstederOppdatertEventV1")
    void testShouldUpdateForRessursFysiskeBrukerstederOppdatertEventV1(no.nav.nom.generated.model.Sektor sektor, List<Telefon> telefoner) {

        var existingRessurs = ressurs().sektor(sektor);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true).sektor(sektor);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursFysiskeBrukerstederOppdatertEventV1 = ressursFysiskeBrukerstederOppdatertEventV1(
            ressursFysiskeBrukerstederOppdatertEventData().toBuilder()
                .primaryFysiskBrukersted("finse")
                .build()
        );

        when(nomApiClient.getRessursByFid(ressursFysiskeBrukerstederOppdatertEventV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(telefoner);

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursFysiskeBrukerstederOppdatertEventV1);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());
        var mobil = telefoner.isEmpty() ? null : telefoner.getFirst().getNummer();

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
                    .sektor(nomSektorToRemedyCode(existingRessurs.getSektor()))
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(existingRessurs.getSektor().equals(no.nav.nom.generated.model.Sektor.EKSTERN) ? epochSecondsNorwegianTimeEndOfDay(getGyldigTomOrMax(dagligOppfolgingOrgtilknytning)) : null)
                    .mobil(mobil)
                    .build()
            ));
    }

    @ParameterizedTest
    @MethodSource("testAllSektorsAndTelefonVariants")
    @DisplayName("Should write new entity with correctly updated values for OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldUpdateForOrgtilknytningDagligOppfolgingEndretEventV1(no.nav.nom.generated.model.Sektor sektor, List<Telefon> telefoner) {

        var existingRessurs = ressurs().sektor(sektor);
        var previousDagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true).sektor(sektor);
        var previousDagligOppfolgingOrgenhet = orgenhet();

        var newDagligOppfolgningRemedyEnhetId = "new-remedy-enhet-id";

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(existingRessurs.getNavident())
                .dagligOppfolging(true)
                .gyldigFom(LocalDate.now())
                .gyldigTom(LocalDate.now().plusMonths(1))
                .orgEnhetId("new-orgenhet")
                .build()
        );

        when(nomApiClient.getRessursByNavident(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().navident())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(previousDagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(previousDagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(previousDagligOppfolgingOrgenhet);
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(telefoner);

        when(nomApiClient.getOrgEnhet(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().orgEnhetId()))
            .thenReturn(new OrgEnhet().remedyEnhetId(newDagligOppfolgningRemedyEnhetId));

        nomRemedyIntegrasjonServiceOld.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());
        var mobil = telefoner.isEmpty() ? null : telefoner.getFirst().getNummer();

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
                    .sektor(nomSektorToRemedyCode(existingRessurs.getSektor()))
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(
                        orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigFom())
                    )
                    .utlopsdato(existingRessurs.getSektor().equals(no.nav.nom.generated.model.Sektor.EKSTERN) ? epochSecondsNorwegianTimeEndOfDay(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigTom()) : null)
                    .mobil(mobil)
                    .build()
            ));
    }

    @Test
    @DisplayName("Should delete OrgtilknytningEvents from local db with deleted orgtilknytning fid")
    void testShouldDeleteOrgtilknytningEventsFromDb() {
        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1();
        var orgtilknytningFjernetEventV1 = orgtilknytningFjernetEventV1(orgtilknytningDagligOppfolgingEndretEventV1.getFid());

        delayedEventRepository.save(
            DelayedEventJpa.builder()
                .delayedUntil(LocalDate.now())
                .event(orgtilknytningDagligOppfolgingEndretEventV1)
                .build()
        );

        nomRemedyIntegrasjonServiceOld.handleOrgtilknytningEvent(orgtilknytningFjernetEventV1);
        assertThat(delayedEventRepository.findAll()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("testAllSektorsAndTelefonVariants")
    @DisplayName("Should write new entity with correctly updated values for OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldNotSendEventToRemedyTable(no.nav.nom.generated.model.Sektor sektor) {

        var existingRessurs = ressurs().sektor(sektor);
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
                .gyldigTom(LocalDate.now().minusMonths(1))
                .orgEnhetId("new-orgenhet")
                .build()
        );

        when(nomApiClient.getRessursByNavident(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().navident())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(previousDagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(previousDagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(previousDagligOppfolgingOrgenhet);

        when(nomApiClient.getOrgEnhet(orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().orgEnhetId()))
            .thenReturn(new OrgEnhet().remedyEnhetId(newDagligOppfolgningRemedyEnhetId));

        nomRemedyIntegrasjonServiceOld.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(newNomIntegrasjonEntityForNavident).isEmpty();
    }

    @Test
    @DisplayName("Should skip handling OrgtilknytningDagligOppfolgingEndretEventV1 if orgtilknytning is not dagligOppfolgning")
    void testSkipUpdateForOrgtilknytningDagligOppfolgingEndretEventV1IfRessursIsNotDagligOppfolgning() {

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder().dagligOppfolging(false).build()
        );

        nomRemedyIntegrasjonServiceOld.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        assertThat(remedyIntegrasjonRepositoryService.findAll()).isEmpty();
    }

    @ParameterizedTest(name = "Should test that all sektors can be delayed")
    @DisplayName("Should delay remedy update for OrgtilknytningDagligOppfolgingEndretEventV1")
    @MethodSource(value = "testAllSektorsAndTelefonVariants")
    void testShouldDelayUpdateForOrgtilknytningDagligOppfolgingEndretEventV1(no.nav.nom.generated.model.Sektor sektor) {
        var existingRessurs = ressurs().sektor(sektor);

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

        nomRemedyIntegrasjonServiceOld.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);
        assertThat(delayedEventRepository.findAll()).hasSize(1);
    }

    private static Stream<Arguments> testAllSektorsAndTelefonVariants() {
        return Stream.of(
            Arguments.of(no.nav.nom.generated.model.Sektor.EKSTERN, getPrivatTelefon()),
            Arguments.of(no.nav.nom.generated.model.Sektor.NAV_KOMMUNAL, getKontorOgPrivatTelefon()),
            Arguments.of(no.nav.nom.generated.model.Sektor.NAV_STATLIG, getTjenesteOgKontorTelefon()),
            Arguments.of(no.nav.nom.generated.model.Sektor.NAV_STATLIG, getTjenesteOgPrivatTelefon()),
            Arguments.of(no.nav.nom.generated.model.Sektor.NAV_STATLIG, Collections.emptyList())
        );
    }

    @ParameterizedTest
    @MethodSource("testAllSektorsAndTelefonVariants")
    @DisplayName("Should update for delayed OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldUpdateForDelayedOrgtilknytningDagligOppfolgingEndretEventV1(no.nav.nom.generated.model.Sektor sektor, List<Telefon> telefoner) {
        var existingRessurs = ressurs().sektor(sektor);
        var orgtilknytning = orgtilknytningState();
        var orgtilknytningEvent = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(existingRessurs.getNavident())
                .orgEnhetId(orgtilknytning.getOrgEnhetId())
                .gyldigTom(orgtilknytning.getGyldigTom())
                .build()
        );

        delayedEventRepository.save(
            DelayedEventJpa.builder()
                .delayedUntil(LocalDate.now())
                .event(orgtilknytningEvent)
                .build());

        when(nomApiClient.getOrgtilknytning(orgtilknytningEvent.getFid())).thenReturn(orgtilknytning);
        when(nomApiClient.getRessursByNavident(existingRessurs.getNavident())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(orgtilknytning));
        when(nomApiClient.getOrgEnhet(orgtilknytning.getOrgEnhetId())).thenReturn(orgenhet());
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(telefoner);

        nomRemedyIntegrasjonServiceOld.triggerDelayedEvents();

        assertThat(remedyIntegrasjonRepositoryService.findAll()).hasSize(1);
        assertThat(delayedEventRepository.findAll()).hasSize(0);
    }

    @ParameterizedTest
    @MethodSource("testAllSektorsAndTelefonVariants")
    @DisplayName("Should update for delayed OrgtilknytningDatoEndretEventV2")
    void testShouldUpdateForDelayedOrgtilknytningDatoEndretEventV2(no.nav.nom.generated.model.Sektor sektor, List<Telefon> telefoner) {
        var existingRessurs = ressurs().sektor(sektor);
        var orgtilknytning = orgtilknytningState().sektor(sektor).gyldigFom(LocalDate.now().plusMonths(1)).gyldigTom(LocalDate.now().plusMonths(3));
        var orgtilknytningEvent = orgtilknytningDatoEndretEventV2(
                orgtilknytningDatoEndretEventData().toBuilder()
                        .gyldigFom(LocalDate.now().plusMonths(1))
                        .gyldigTom(LocalDate.now().plusMonths(3))
                        .build()
        );

        delayedEventRepository.save(
            DelayedEventJpa.builder()
                .delayedUntil(LocalDate.now())
                .event(orgtilknytningEvent)
                .build());

        when(nomApiClient.getOrgtilknytning(orgtilknytningEvent.getFid())).thenReturn(orgtilknytning);
        when(nomApiClient.getRessursByNavident(existingRessurs.getNavident())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(orgtilknytning));
        when(nomApiClient.getOrgEnhet(orgtilknytning.getOrgEnhetId())).thenReturn(orgenhet());
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(telefoner);

        nomRemedyIntegrasjonServiceOld.triggerDelayedEvents();

        assertThat(remedyIntegrasjonRepositoryService.findAll()).hasSize(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testShouldNotUpdateOrgtilknytningDolEventInput")
    @DisplayName("Should not update remedy for delayed OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldNotUpdateForDelayedOrgtilknytningDagligOppfolgingEndretEventV1(
        DelayedEventJpa delayedEventJpa, OrgtilknytningState orgtilknytning) {
        var orgtilknytningDolEvent = (OrgtilknytningDagligOppfolgingEndretEventV1) delayedEventJpa.getEvent();
        delayedEventRepository.save(delayedEventJpa);

        when(nomApiClient.getOrgtilknytning(orgtilknytningDolEvent.getFid())).thenReturn(orgtilknytning);

        nomRemedyIntegrasjonServiceOld.triggerDelayedEvents();

        assertThat(delayedEventRepository.findAll()).hasSize(0);
        assertThat(delayedEventRepository.findAll()).hasSize(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testShouldNotUpdateOrgtilknytningEventInput")
    @DisplayName("Should not update remedy for delayed OrgtilknytningDatoEndretEventV2")
    void testShouldNotUpdateForDelayedOrgtilknytningEndretEventV1(
        DelayedEventJpa delayedEventJpa, OrgtilknytningState orgtilknytning) {
        var orgtilknytningDolEvent = (OrgtilknytningDatoEndretEventV2) delayedEventJpa.getEvent();
        delayedEventRepository.save(delayedEventJpa);

        when(nomApiClient.getOrgtilknytning(orgtilknytningDolEvent.getFid())).thenReturn(orgtilknytning);

        nomRemedyIntegrasjonServiceOld.triggerDelayedEvents();

        assertThat(delayedEventRepository.findAll()).hasSize(0);
        assertThat(delayedEventRepository.findAll()).hasSize(0);
    }

    private static Stream<Arguments> testShouldNotUpdateOrgtilknytningDolEventInput() {
        var orgtilknytning = orgtilknytningState();
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);

        return Stream.of(
            Arguments.of(
                DelayedEventJpa.builder()
                    .delayedUntil(LocalDate.now())
                    .event(
                        orgtilknytningDagligOppfolgingEndretEventV1(
                            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                                .navident(existingRessurs.getNavident())
                                .orgEnhetId(orgtilknytning.getOrgEnhetId())
                                .gyldigTom(orgtilknytning.getGyldigTom().plusDays(1))
                                .build()))
                    .build(),
                orgtilknytning,
                existingRessurs
            ),
            Arguments.of(
                DelayedEventJpa.builder()
                    .delayedUntil(LocalDate.now())
                    .event(
                        orgtilknytningDagligOppfolgingEndretEventV1(
                            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                                .navident(existingRessurs.getNavident())
                                .orgEnhetId(orgtilknytning.getOrgEnhetId())
                                .gyldigTom(orgtilknytning.getGyldigTom().minusDays(1))
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
                DelayedEventJpa.builder()
                    .delayedUntil(LocalDate.now())
                    .event(
                        orgtilknytningDatoEndretEventV2(
                            orgtilknytningDatoEndretEventData().toBuilder()
                                .gyldigTom(orgtilknytning.getGyldigTom().plusDays(1))
                                .build()))
                    .build(),
                orgtilknytning,
                existingRessurs
            ),
            Arguments.of(
                DelayedEventJpa.builder()
                    .delayedUntil(LocalDate.now())
                    .event(
                        orgtilknytningDatoEndretEventV2(
                            orgtilknytningDatoEndretEventData().toBuilder()
                                .gyldigTom(orgtilknytning.getGyldigTom().minusDays(1))
                                .build()))
                    .build(),
                orgtilknytning,
                existingRessurs
            )
        );
    }

    @Test
    @DisplayName("Should skip updating values for OrgtilknytningDatoEndretEventV2 if orgtilknytning is not dagligOppfolgning")
    void testSkipUpdateForOrgtilknytningDatoEndretEventV2IfRessursIsNotDagligOppfolgning() {
        var orgtilknytningDatoEndretEventV2 = orgtilknytningDatoEndretEventV2();

        when(nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid()))
            .thenReturn(new OrgtilknytningState().erDagligOppfolging(false));

        nomRemedyIntegrasjonServiceOld.handleOrgtilknytningEvent(orgtilknytningDatoEndretEventV2);

        assertThat(remedyIntegrasjonRepositoryService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should skip updating values for OrgtilknytningDatoEndretEventV2 if orgtilknytning is dagligOppfolgning but is not active")
    void testSkipUpdateForOrgtilknytningDatoEndretEventV2IfRessursIsDagligOppfolgningButNotActive() {
        var ressurs = ressurs();

        var orgtilknytningDatoEndretEventV2EventData = orgtilknytningDatoEndretEventV2()
            .getOrgtilknytningDatoEndretEventData()
            .toBuilder()
            .gyldigFom(LocalDate.now().minusYears(2))
            .gyldigTom(LocalDate.now().minusYears(1))
            .build();

        var orgtilknytningDatoEndretEventV2 = orgtilknytningDatoEndretEventV2(orgtilknytningDatoEndretEventV2EventData);

        var orgtilknytningState = new OrgtilknytningState()
            .fid(orgtilknytningDatoEndretEventV2.getFid())
            .orgEnhetId("org-enhet-id")
            .erDagligOppfolging(true)
            .sektor(no.nav.nom.generated.model.Sektor.NAV_KOMMUNAL)
            .gyldigFom(LocalDate.now().minusYears(2))
            .gyldigTom(LocalDate.now().minusYears(1))
            .orgtilknytningType(Tilknytningtype.INGEN);

        when(nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid()))
            .thenReturn(orgtilknytningState);

        when(nomApiClient.getRessursByNavident(orgtilknytningState.getNavident()))
            .thenReturn(ressurs);

        when(nomApiClient.getOrgEnhet(orgtilknytningState.getOrgEnhetId()))
            .thenReturn(orgenhet());

        nomRemedyIntegrasjonServiceOld.handleOrgtilknytningEvent(orgtilknytningDatoEndretEventV2);

        assertThat(remedyIntegrasjonRepositoryService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should not write new NomIntegrasjonEntity if it provides no new data from nom")
    void testWriteIgnoredIfNoNewDataFromNom() {
        var navident = "X654321";

        var existingNomIntegrasjonEntity = nomIntegrasjonEntity().toBuilder()
            .navId(navident)
            .entryStatus("Behandlet")
            .build();

        remedyIntegrasjonRepositoryService.save(existingNomIntegrasjonEntity);

        var newNomIntegrasjonEntity = existingNomIntegrasjonEntity.toBuilder()
            .entryStatus("Ubehandlet")
            .build();

        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntity(newNomIntegrasjonEntity, navident);

        assertThat(remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(navident))
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

        nomRemedyIntegrasjonServiceOld.findRessurserWithSluttdatoAndMarkForDeletion(sluttdato, initiatedBy);

        assertThat(remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(navidentForRessursToDelete))
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
        DelayedEventJpa delayedEventJpa, Ressurs existingRessurs, NomIntegrasjonEntity expectedNomIntegrasjonEntity
    ) {

        when(nomApiClient.getRessursByFid(delayedEventJpa.getEvent().getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgEnhet(any())).thenReturn(orgenhet());
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(orgtilknytningState()));
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident())).thenReturn(List.of());

        delayedEventRepository.saveAll(List.of(delayedEventJpa));

        nomRemedyIntegrasjonServiceOld.triggerDelayedEvents();

        assertThat(remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident()).orElse(null))
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
                DelayedEventJpa.builder().delayedUntil(yesterday).event(ressursSluttetEventV1).build(),
                existingRessursWithSluttdatoYesterday,
                nomIntegrasjonEntityForDeletingRessurs.toBuilder()
                    .entryBestiller(ressursSluttetEventV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .build()
            ),
            Arguments.of(
                DelayedEventJpa.builder().delayedUntil(yesterday).event(ressursSluttdatoOppdatertEventV1).build(),
                existingRessursWithSluttdatoYesterday,
                nomIntegrasjonEntityForDeletingRessurs.toBuilder()
                    .entryBestiller(ressursSluttdatoOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .build()
            ),
            // Tester at man ikke sletter ressurs dersom ressursen har en annen sluttdato enn spesifisert i den forsinkede hendelsen
            Arguments.of(
                DelayedEventJpa.builder().delayedUntil(yesterday).event(ressursSluttetEventV1).build(),
                existingRessursWithSluttdatoAnotherDay,
                null
            ),
            Arguments.of(
                DelayedEventJpa.builder().delayedUntil(yesterday).event(ressursSluttdatoOppdatertEventV1).build(),
                existingRessursWithSluttdatoAnotherDay,
                null
            )
        );
    }

    @Test
    @DisplayName("Should not write new gyldigTom in OrgtilknytningDatoEndretEventV2")
    void testShouldNotWriteNewGyldigTomInOrgtilknytningDatoEndretEventV2() {
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var orgtilknytning = orgtilknytningState();
        var orgtilknytningDatoEndretEventV2 = orgtilknytningDatoEndretEventV2(
            orgtilknytningDatoEndretEventData().toBuilder()
                .gyldigFom(LocalDate.now())
                .gyldigTom(orgtilknytning.getGyldigTom().plusMonths(1))
                .build()
        );

        when(nomApiClient.getOrgtilknytning(orgtilknytningDatoEndretEventV2.getFid()))
            .thenReturn(orgtilknytning);
        when(nomApiClient.getRessursByNavident(existingRessurs.getNavident()))
            .thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident()))
            .thenReturn(List.of(orgtilknytning));
        when(nomApiClient.getOrgEnhet(orgtilknytning.getOrgEnhetId()))
            .thenReturn(orgenhet());
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident()))
            .thenReturn(List.of());

        nomRemedyIntegrasjonServiceOld.handleOrgtilknytningEvent(orgtilknytningDatoEndretEventV2);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());
        assertThat(newNomIntegrasjonEntityForNavident)
                .usingRecursiveComparison()
                .isEqualTo(Optional.of(
                        NomIntegrasjonEntity.builder()
                                .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                                .entryBestiller(orgtilknytningDatoEndretEventV2.getMetadata().inputMetadata().origination().initiatedBy())
                                .entryOperasjon("Ny")
                                .entryStatus("Ubehandlet")
                                .enhetsId(orgenhet().getRemedyEnhetId())
                                .etternavn(capitalizeNavn(existingRessurs.getVisningsnavn().getEtternavn()))
                                .fodselsnummer(existingRessurs.getPersonident())
                                .fornavn(capitalizeNavn(formatFornavnWithMellomnavn(existingRessurs.getVisningsnavn())))
                                .fysiskBrukerstedId(existingRessurs.getPrimaryFysiskBrukersted())
                                .navId(existingRessurs.getNavident())
                                .sektor(nomSektorToRemedyCode(existingRessurs.getSektor()))
                                .tiltredelsesdato(epochSecondsNorwegianTimeNoon(
                                        orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigFom())
                                )
                                .utlopsdato(existingRessurs.getSektor().equals(no.nav.nom.generated.model.Sektor.EKSTERN) ? epochSecondsNorwegianTimeEndOfDay(orgtilknytning.getGyldigTom()) : null)
                                .direkteTelefon(null)
                                .mobil(null)
                                .build()
                ));
    }

    @Test
    @DisplayName("Should delay and update remedy for RessursSluttdatoOppdatertEventV1")
    void testShouldDelayAndUpdateForRessursSluttdatoOppdatertEventV1() {
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var ressursSluttdatoOppdatertEventV1 = ressursSluttdatoOppdatertEventV1(LocalDate.now().plusDays(4));

        var existingNomIntegrasjonEntity = nomIntegrasjonEntity().toBuilder()
            .navId(existingRessurs.getNavident())
            .entryStatus("Behandlet")
            .build();

        when(nomApiClient.getRessursByFid(ressursSluttdatoOppdatertEventV1.getFid()))
            .thenReturn(existingRessurs);
        when(nomApiClient.getOrgEnhet(any())).thenReturn(orgenhet());
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident()))
            .thenReturn(List.of(orgtilknytningState()));
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident()))
            .thenReturn(List.of());

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursSluttdatoOppdatertEventV1);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());
        assertThat(newNomIntegrasjonEntityForNavident)
                .usingRecursiveComparison()
                .isEqualTo(Optional.of(
                        NomIntegrasjonEntity.builder()
                                .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                                .entryBestiller(ressursSluttdatoOppdatertEventV1.getMetadata().inputMetadata().origination().initiatedBy())
                                .entryOperasjon("Ny")
                                .entryStatus("Ubehandlet")
                                .enhetsId(orgenhet().getRemedyEnhetId())
                                .etternavn(capitalizeNavn(existingRessurs.getVisningsnavn().getEtternavn()))
                                .fodselsnummer(existingRessurs.getPersonident())
                                .fornavn(capitalizeNavn(formatFornavnWithMellomnavn(existingRessurs.getVisningsnavn())))
                                .fysiskBrukerstedId(existingRessurs.getPrimaryFysiskBrukersted())
                                .navId(existingRessurs.getNavident())
                                .sektor(nomSektorToRemedyCode(existingRessurs.getSektor()))
                                .tiltredelsesdato(existingNomIntegrasjonEntity.getTiltredelsesdato())
                                .utlopsdato(existingRessurs.getSektor().equals(no.nav.nom.generated.model.Sektor.EKSTERN) ? epochSecondsNorwegianTimeEndOfDay(ressursSluttdatoOppdatertEventV1.getRessursSluttdatoOppdatertEventData().sluttdato()) : null)
                                .direkteTelefon(null)
                                .mobil(null)
                                .build()
                ));
        assertThat(delayedEventRepository.findAll()).size().isEqualTo(1);
    }

    @Test
    @DisplayName("Should delay and update remedy for RessursSluttdatoOppdatertEventV1")
    void testShouldDelayForRessursSluttdatoOppdatertEventV1() {
        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.NAV_STATLIG);
        var ressursSluttdatoOppdatertEventV1 = ressursSluttdatoOppdatertEventV1(LocalDate.now().plusDays(4));

        when(nomApiClient.getRessursByFid(ressursSluttdatoOppdatertEventV1.getFid()))
                .thenReturn(existingRessurs);
        when(nomApiClient.getOrgEnhet(any())).thenReturn(orgenhet());
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident()))
                .thenReturn(List.of(orgtilknytningState()));
        when(nomApiClient.getTelefonnummere(existingRessurs.getNavident()))
                .thenReturn(List.of());

        nomRemedyIntegrasjonServiceOld.handleRessursEvent(ressursSluttdatoOppdatertEventV1);

        var newNomIntegrasjonEntityForNavident = remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());
        assertThat(newNomIntegrasjonEntityForNavident)
                .usingRecursiveComparison()
                .isEqualTo(Optional.empty());
        assertThat(delayedEventRepository.findAll()).size().isEqualTo(1);
    }


    @ParameterizedTest(name = "latestId: {0} expectedNextId: {1}")
    @MethodSource("testNextIdArguments")
    void testNextId(String latestId, String expectedNextId) {
        if (latestId != null) {
            remedyIntegrasjonRepositoryService.save(NomIntegrasjonEntity.builder().entryId(latestId).build());
        }
        assertThat(remedyIntegrasjonRepositoryService.nextId()).isEqualTo(expectedNextId);
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

    @Test
    @DisplayName("Should return correct order of telefoner")
    void testGetTelefoner() {
        var telefonPrivat = new Telefon();
        telefonPrivat.setNummer("12348765");
        telefonPrivat.setType(TelefonType.PRIVAT_TELEFON);
        var telefonTjeneste = new Telefon();
        telefonTjeneste.setNummer("12345678");
        telefonTjeneste.setType(TelefonType.NAV_TJENESTE_TELEFON);
        var telefonKontor = new Telefon();
        telefonKontor.setNummer("87654321");
        telefonKontor.setType(TelefonType.NAV_KONTOR_TELEFON);
        var unorderedListOfTelefoner1 = Stream.of(telefonPrivat, telefonTjeneste, telefonTjeneste, telefonKontor).sorted(sortTelefonByType()).toList();
        var unorderedListOfTelefoner2 = Stream.of(telefonPrivat, telefonKontor, telefonTjeneste).sorted(sortTelefonByType()).toList();
        var unorderedListOfTelefoner3 = Stream.of(telefonKontor, telefonTjeneste, telefonPrivat).sorted(sortTelefonByType()).toList();

        assertThat(unorderedListOfTelefoner1).containsExactly(telefonTjeneste, telefonTjeneste, telefonKontor, telefonPrivat);
        assertThat(unorderedListOfTelefoner2).containsExactly(telefonTjeneste, telefonKontor, telefonPrivat);
        assertThat(unorderedListOfTelefoner3).containsExactly(telefonTjeneste, telefonKontor, telefonPrivat);
    }

    private Integer nomSektorToRemedyCode(Sektor sektor) {
        return switch (sektor) {
            case NAV_STATLIG -> 0;
            case NAV_KOMMUNAL -> 1;
            case EKSTERN -> 2;
        };
    }

    private Integer nomSektorToRemedyCode(no.nav.nom.generated.model.Sektor sektor) {
        return switch (sektor) {
            case NAV_STATLIG -> 0;
            case NAV_KOMMUNAL -> 1;
            case EKSTERN -> 2;
        };
    }

    private static List<Telefon> getPrivatTelefon() {
        var telefon = new Telefon();
        telefon.setNummer("12348765");
        telefon.setType(TelefonType.PRIVAT_TELEFON);
        return List.of(
            telefon
        );
    }

    private static List<Telefon> getTjenesteOgKontorTelefon() {
        var telefon1 = new Telefon();
        telefon1.setNummer("12345678");
        telefon1.setType(TelefonType.NAV_TJENESTE_TELEFON);
        var telefon2 = new Telefon();
        telefon2.setNummer("87654321");
        telefon2.setType(TelefonType.NAV_KONTOR_TELEFON);
        return List.of(
            telefon1,
            telefon2
        );
    }

    private static List<Telefon> getTjenesteOgPrivatTelefon() {
        var telefon1 = new Telefon();
        telefon1.setNummer("12345678");
        telefon1.setType(TelefonType.NAV_TJENESTE_TELEFON);
        var telefon2 = new Telefon();
        telefon2.setNummer("87654321");
        telefon2.setType(TelefonType.PRIVAT_TELEFON);
        return List.of(
            telefon1,
            telefon2
        );
    }

    private static List<Telefon> getKontorOgPrivatTelefon() {
        var telefon1 = new Telefon();
        telefon1.setNummer("87654321");
        telefon1.setType(TelefonType.NAV_KONTOR_TELEFON);
        var telefon2 = new Telefon();
        telefon2.setNummer("12348765");
        telefon2.setType(TelefonType.PRIVAT_TELEFON);
        return List.of(
            telefon1,
            telefon2
        );
    }

    private Comparator<Telefon> sortTelefonByType() {
        return (o1, o2) -> {
            if (o1.getType().equals(o2.getType())) return 0;
            if (o1.getType().equals(TelefonType.NAV_TJENESTE_TELEFON)) return -1;
            if (o2.getType().equals(TelefonType.NAV_TJENESTE_TELEFON)) return 1;
            if (o1.getType().equals(TelefonType.NAV_KONTOR_TELEFON)) return -1;
            if (o2.getType().equals(TelefonType.NAV_KONTOR_TELEFON)) return 1;
            if (o1.getType().equals(TelefonType.PRIVAT_TELEFON)) return -1;
            if (o2.getType().equals(TelefonType.PRIVAT_TELEFON)) return 1;
            return 0;
        };
    }
}
