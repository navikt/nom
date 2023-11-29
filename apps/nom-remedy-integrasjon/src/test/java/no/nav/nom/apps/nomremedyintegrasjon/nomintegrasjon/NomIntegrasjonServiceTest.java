package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.generated.model.OrgEnhet;
import no.nav.nom.generated.model.Ressurs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.inputMetadata;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.nomIntegrasjonEntity;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetAdditionalData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSeconds;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;
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
        when(nomApiClient.getRessurs(anyString()))
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
                    .tiltredelsesdato(epochSeconds(startDato))
                    .utlopsdato(epochSeconds(sluttDato))
                    .build()
            ));

    }

    @Test
    @DisplayName("Should skip write to remedy_integrasjon table if sektor is NOT EKSTERN")
    void testShouldSkipIfNotEkstern() {

        List.of(Sektor.NAV_KOMMUNAL, Sektor.NAV_STATLIG).forEach(
            sektor -> nomIntegrasjonService.handleRessursEvent(
                new RessursOpprettetEventV1(
                    UUID.randomUUID().toString(),
                    ressursOpprettetEventData().toBuilder().sektor(sektor).build(),
                    inputMetadata(),
                    ressursOpprettetAdditionalData()
                )
            )
        );

        var nomIntegrasjonEntries = nomIntegrasjonRepository.findAll();

        assertThat(nomIntegrasjonEntries).isEmpty();
    }

    @Test
    @DisplayName("Should write new entity with correctly updated values for OrgtilknytningDagligOppfolgingEndretEventV1")
    void testShouldUpdateForOrgtilknytningDagligOppfolgingEndretEventV1() {
        var navident = "X123456";

        var latestNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.save(
            nomIntegrasjonEntity().toBuilder()
                .entryId(nomIntegrasjonService.nextId())
                .navId(navident).build()
        );

        var newGyldigFom = LocalDate.now().plusDays(1);
        var newGyldigTom = newGyldigFom.plusMonths(1);
        var newOrgenhetId = "new-orgenhet";
        var newRemedyEnhetId = "new-remedy-enhet-id";

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(latestNomIntegrasjonEntityForNavident.navId)
                .dagligOppfolging(true)
                .gyldigFom(newGyldigFom)
                .gyldigTom(Optional.of(newGyldigTom))
                .orgEnhetId(newOrgenhetId)
                .build()
        );

        when(nomApiClient.getOrgEnhet(newOrgenhetId))
            .thenReturn(new OrgEnhet().remedyEnhetId(newRemedyEnhetId));

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        var newNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navident);

        assertThat(newNomIntegrasjonEntityForNavident)
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(
                latestNomIntegrasjonEntityForNavident.toBuilder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(1L))
                    .entryBestiller(
                        orgtilknytningDagligOppfolgingEndretEventV1.getMetadata().inputMetadata().origination().initiatedBy()
                    )
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .tiltredelsesdato(epochSeconds(newGyldigFom))
                    .utlopsdato(epochSeconds(newGyldigTom))
                    .enhetsId(newRemedyEnhetId)
                    .build()

            ));
    }

    @Test
    @DisplayName("Should NOT write new entity with updated values for OrgtilknytningDagligOppfolgingEndretEventV1 if new data are same as old")
    void testShouldNotUpdateForOrgtilknytningDagligOppfolgingEndretEventV1IfNoChanges() {
        var navident = "X123456";

        var initialLatestNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.save(
            nomIntegrasjonEntity().toBuilder()
                .entryId(nomIntegrasjonService.nextId())
                .navId(navident).build()
        );

        var newGyldigFomSameAsOld = Instant.ofEpochSecond(initialLatestNomIntegrasjonEntityForNavident.tiltredelsesdato)
            .atZone(ZoneOffset.UTC).toLocalDate();
        var newGyldigTomSameAsOld = Instant.ofEpochSecond(initialLatestNomIntegrasjonEntityForNavident.utlopsdato)
            .atZone(ZoneOffset.UTC).toLocalDate();
        var newOrgenhetId = "new-orgenhet";

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(initialLatestNomIntegrasjonEntityForNavident.navId)
                .dagligOppfolging(true)
                .gyldigFom(newGyldigFomSameAsOld)
                .gyldigTom(Optional.of(newGyldigTomSameAsOld))
                .orgEnhetId(newOrgenhetId)
                .build()
        );

        when(nomApiClient.getOrgEnhet(newOrgenhetId))
            .thenReturn(new OrgEnhet().remedyEnhetId(initialLatestNomIntegrasjonEntityForNavident.enhetsId));

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        assertThat(nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navident))
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(initialLatestNomIntegrasjonEntityForNavident));
    }

    @Test
    @DisplayName("Should skip updating values for OrgtilknytningDagligOppfolgingEndretEventV1 if ressurs is not EKSTERN")
    void testSkipUpdateForOrgtilknytningDagligOppfolgingEndretEventV1IfRessursIsNotEkstern() {
        var navident = "X123456";

        var initialLatestNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.save(
            nomIntegrasjonEntity().toBuilder()
                .entryId(nomIntegrasjonService.nextId())
                .navId(navident).build()
        );

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(initialLatestNomIntegrasjonEntityForNavident.navId)
                .dagligOppfolging(true)
                .gyldigFom(LocalDate.now().plusDays(1))
                .build()
        );

        when(nomApiClient.getRessurs(navident)).thenReturn(
            new Ressurs().sektor(no.nav.nom.generated.model.Sektor.NAV_STATLIG)
        );

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        assertThat(nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navident))
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(initialLatestNomIntegrasjonEntityForNavident));
    }

    @Test
    @DisplayName("Should skip updating values for OrgtilknytningDagligOppfolgingEndretEventV1 if orgtilknytning is not dagligOppfolgning")
    void testSkipUpdateForOrgtilknytningDagligOppfolgingEndretEventV1IfRessursIsNotDagligOppfolgning() {
        var navident = "X123456";

        var initialLatestNomIntegrasjonEntityForNavident = nomIntegrasjonRepository.save(
            nomIntegrasjonEntity().toBuilder()
                .entryId(nomIntegrasjonService.nextId())
                .navId(navident).build()
        );

        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(
            orgtilknytningDagligOppfolgingEndretEventData().toBuilder()
                .navident(initialLatestNomIntegrasjonEntityForNavident.navId)
                .dagligOppfolging(false)
                .gyldigFom(LocalDate.now().plusDays(1))
                .build()
        );

        nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        assertThat(nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navident))
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(initialLatestNomIntegrasjonEntityForNavident));
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
