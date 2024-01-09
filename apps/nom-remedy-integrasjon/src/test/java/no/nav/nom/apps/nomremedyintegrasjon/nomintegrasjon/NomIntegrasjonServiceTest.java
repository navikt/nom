package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.generated.model.OrgEnhet;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.Ressurs;
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

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.inputMetadata;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.nomIntegrasjonEntity;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgenhet;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDatoEndretEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDatoEndretEventV2;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningState;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressurs;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursFysiskeBrukerstederOppdatertEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursFysiskeBrukerstederOppdatertEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetAdditionalData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursPersonaliaOppdatertV1;
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

    @BeforeEach
    void clearRepository() {
        nomIntegrasjonRepository.deleteAll();
    }

    @BeforeEach
    void mockNomApiClientDefault() {
        when(nomApiClient.getRessursByNavident(anyString()))
            .thenReturn(
                new Ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN)
            );
    }

    @Test
    @DisplayName("Should create new entry with correct data for RessursOpprettetEventV1")
    void testShouldCreateForRessursOpprettetEventV1() {

        var navident = "X123456";
        var initiatedBy = "Z123456";
        var etternavn = "etternavn";
        var personident = "11111111111";
        var fornavn = "fornavn";
        var startDato = LocalDate.of(2023, 12, 12);
        var sluttDato = LocalDate.of(2024, 12, 12);
        var orgEnhetId = "some-orgenhet";
        var remedyEnhetId = "some-remedy-id";
        var fysiskBrukerstedNr = "0123";

        var ressursOpprettetEventV1 = new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData().toBuilder()
                .personident(personident)
                .visningsnavn(new Visningsnavn(fornavn, Optional.empty(), etternavn))
                .sektor(Sektor.EKSTERN)
                .build(),
            inputMetadata().toBuilder()
                .origination(new Origination(initiatedBy, ""))
                .inputInterfaceMethodArguments(Map.of(
                    "orgEnhetId", orgEnhetId,
                    "sluttDato", sluttDato.toString(),
                    "fysiskBrukerstedNr", fysiskBrukerstedNr
                ))

                .build(),
            ressursOpprettetAdditionalData().toBuilder()
                .navident(Optional.of(navident))
                .startdato(Optional.of(startDato))
                .build()
        );

        when(nomApiClient.getOrgEnhet(orgEnhetId))
            .thenReturn(new OrgEnhet().remedyEnhetId(remedyEnhetId));

        nomIntegrasjonService.handleRessursEvent(ressursOpprettetEventV1);

        var nomIntegrasjonEntry = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navident);

        assertThat(nomIntegrasjonEntry)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(initiatedBy)
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(remedyEnhetId)
                    .etternavn(capitalizeNavn(etternavn))
                    .fodselsnummer(personident)
                    .fornavn(capitalizeNavn(fornavn))
                    .fysiskBrukerstedId(fysiskBrukerstedNr)
                    .navId(navident)
                    .sektor(2)
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(startDato))
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(sluttDato))
                    .build()
            ));

    }

    @Test
    @DisplayName("Should create new entry with utlopsdato from RessursSluttetEventV1")
    void testShoulSetUtlopsdatoFromRessursSluttetEventV1() {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState().erDagligOppfolging(true);
        var dagligOppfolgingOrgenhet = orgenhet();

        var ressursSluttetEventV1 = ressursSluttetEventV1();

        when(nomApiClient.getRessursByFid(ressursSluttetEventV1.getFid())).thenReturn(existingRessurs);
        when(nomApiClient.getOrgtilknytningerForNavident(existingRessurs.getNavident())).thenReturn(List.of(dagligOppfolgingOrgtilknytning));
        when(nomApiClient.getOrgEnhet(dagligOppfolgingOrgtilknytning.getOrgEnhetId())).thenReturn(dagligOppfolgingOrgenhet);

        nomIntegrasjonService.handleRessursEvent(ressursSluttetEventV1);

        var nomIntegrasjonEntry = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(existingRessurs.getNavident());

        assertThat(nomIntegrasjonEntry)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(ressursSluttetEventV1.getMetadata().inputMetadata().origination().initiatedBy())
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId(dagligOppfolgingOrgenhet.getRemedyEnhetId())
                    .etternavn(capitalizeNavn(existingRessurs.getVisningsnavn().getEtternavn()))
                    .fodselsnummer(existingRessurs.getPersonident())
                    .fornavn(capitalizeNavn(formatFornavnWithMellomnavn(existingRessurs.getVisningsnavn())))
                    .fysiskBrukerstedId(existingRessurs.getPrimaryFysiskBrukersted())
                    .navId(existingRessurs.getNavident())
                    .sektor(2)
                    .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dagligOppfolgingOrgtilknytning.getGyldigFom()))
                    .utlopsdato(epochSecondsNorwegianTimeEndOfDay(ressursSluttetEventV1.getRessursSluttetEventData().sluttdato()))
                    .build()
            ));
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
                    .build()

            ));
    }

    @ParameterizedTest(name = "Should not handle RessursEvent if ressurs is not EKSTERN")
    @MethodSource("testShouldNotHandleRessursEventIfNotEksternInput")
    void testShouldNotHandleRessursEventIfNotEkstern(RessursEvent ressursEvent) {

        when(nomApiClient.getRessursByFid(ressursEvent.getFid()))
            .thenReturn(new Ressurs().sektor(no.nav.nom.generated.model.Sektor.NAV_STATLIG));

        nomIntegrasjonService.handleRessursEvent(ressursEvent);

        assertThat(nomIntegrasjonRepository.findAll()).isEmpty();
    }

    private static Stream<Arguments> testShouldNotHandleRessursEventIfNotEksternInput() {
        return Stream.of(
            Arguments.of(new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                ressursOpprettetEventData().toBuilder().sektor(Sektor.NAV_STATLIG).build(),
                inputMetadata(),
                ressursOpprettetAdditionalData()
            )),
            Arguments.of(new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                ressursOpprettetEventData().toBuilder().sektor(Sektor.NAV_KOMMUNAL).build(),
                inputMetadata(),
                ressursOpprettetAdditionalData()
            )),
            Arguments.of(ressursSluttetEventV1()),
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
                .gyldigFom(LocalDate.now().plusDays(1))
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
                        orgtilknytningDagligOppfolgingEndretEventV1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigTom().get())
                    )
                    .build()
            ));
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
    @DisplayName("Should write new entity with correctly updated values for OrgtilknytningDatoEndretEventV2")
    void testShouldUpdateForOrgtilknytningDatoEndretEventV2() {

        var existingRessurs = ressurs().sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
        var dagligOppfolgingOrgtilknytning = orgtilknytningState()
            .navident(existingRessurs.getNavident())
            .erDagligOppfolging(true);
        var dagligOppfolgingOrgenhet = orgenhet();

        var orgtilknytningDatoEndretEventV2 = orgtilknytningDatoEndretEventV2(
            orgtilknytningDatoEndretEventData().toBuilder()
                .gyldigFom(dagligOppfolgingOrgtilknytning.getGyldigFom().plusDays(1))
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
                        orgtilknytningDatoEndretEventV2.getOrgtilknytningDatoEndretEventData().gyldigTom().get())
                    )
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
}
