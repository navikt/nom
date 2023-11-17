package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.inputMetadata;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetAdditionalData;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetEventData;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSeconds;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;
import static org.assertj.core.api.Assertions.assertThat;

public class NomIntegrasjonServiceTest extends TestBase {

    @Autowired
    private NomIntegrasjonService nomIntegrasjonService;

    @BeforeEach
    void clearRepository() {
        nomIntegrasjonRepository.deleteAll();
    }

    @Test
    @DisplayName("Should populate nom_integrasjon table as intended")
    void testCreateRessurs() {

        var navident = "X123456";
        var initiatedBy = "Z123456";
        var etternavn = "etternavn";
        var personident = "11111111111";
        var fornavn = "fornavn";
        var startDato = LocalDate.of(2023, 12, 12);

        var ressursOpprettetEventV1 = new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData().toBuilder()
                .personident(personident)
                .visningsnavn(new Visningsnavn(fornavn, Optional.empty(), etternavn))
                .sektor(Sektor.EKSTERN)
                .build(),
            inputMetadata().toBuilder()
                .origination(new Origination(initiatedBy, ""))
                .build(),
            ressursOpprettetAdditionalData().toBuilder()
                .navident(Optional.of(navident))
                .startdato(Optional.of(startDato))
                .build()
        );

        nomIntegrasjonService.handleRessursEvent(ressursOpprettetEventV1);

        var nomIntegrasjonEntry = nomIntegrasjonRepository.findTopByNavIdEqualsOrderByEntryIdDesc(navident);

        assertThat(nomIntegrasjonEntry)
            .contains(
                NomIntegrasjonEntity.builder()
                    .entryId(fifteenCharLengthZeroPaddedNumber(0L))
                    .entryBestiller(initiatedBy)
                    .entryOperasjon("Ny")
                    .entryStatus("Ubehandlet")
                    .enhetsId("TODO")
                    .etternavn(capitalizeNavn(etternavn))
                    .fodselsnummer(personident)
                    .fornavn(capitalizeNavn(fornavn))
                    .fysiskBrukerstedId("TODO")
                    .mobil("TODO")
                    .navId(navident)
                    .sektor(2)
                    .tiltredelsesdato(epochSeconds(startDato))
                    .utlopsdato(0L)
                    .build()
            );
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
