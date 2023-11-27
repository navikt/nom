package no.nav.nom.apps.nomremedyintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSeconds;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;

public class TestData {

    public static RessursOpprettetEventV1 ressursOpprettetEventV1() {
        return new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData(),
            inputMetadata(),
            ressursOpprettetAdditionalData()
        );
    }

    public static OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1() {
        return orgtilknytningDagligOppfolgingEndretEventV1(orgtilknytningDagligOppfolgingEndretEventData());
    }

    public static OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1(
        OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData orgtilknytningDagligOppfolgingEndretEventData
    ) {
        return new OrgtilknytningDagligOppfolgingEndretEventV1(
            UUID.randomUUID().toString(),
            orgtilknytningDagligOppfolgingEndretEventData,
            inputMetadata(),
            null
        );
    }

    public static RessursOpprettetEventV1.RessursOpprettetEventData ressursOpprettetEventData() {
        return new RessursOpprettetEventV1.RessursOpprettetEventData(
            "11111111111",
            Sektor.EKSTERN,
            new Visningsnavn(
                "Fornavn", Optional.empty(), "Etternavn"
            )
        );
    }

    public static InputMetadata inputMetadata(){
        return new InputMetadata("", "", "", origination());
    }

    public static Origination origination() {
        return new Origination("X123456", "");
    }

    public static RessursOpprettetEventV1.RessursOpprettetAdditionalData ressursOpprettetAdditionalData() {
        return new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
            Optional.of("Z654321"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(LocalDate.now()),
            Optional.of("orgenhet"),
            Optional.empty()
        );
    }

    public static OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData orgtilknytningDagligOppfolgingEndretEventData() {
        return new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
            "Z654321",
            "orgenhet",
            LocalDate.now(),
            Optional.of(LocalDate.now().plusMonths(11)),
            true
        );
    }

    public static NomIntegrasjonEntity nomIntegrasjonEntity(){
        return NomIntegrasjonEntity.builder()
            .entryId(fifteenCharLengthZeroPaddedNumber(0L))
            .entryBestiller("Z654321")
            .entryOperasjon("Ny")
            .entryStatus("Ubehandlet")
            .enhetsId("0123")
            .etternavn("Etternavn")
            .fodselsnummer("11111111111")
            .fornavn("Fornavn")
            .fysiskBrukerstedId("4321")
            .navId("X654321")
            .sektor(2)
            .tiltredelsesdato(epochSeconds(LocalDate.now()))
            .utlopsdato(epochSeconds(LocalDate.now().plusMonths(1)))
            .build();
    }
}
