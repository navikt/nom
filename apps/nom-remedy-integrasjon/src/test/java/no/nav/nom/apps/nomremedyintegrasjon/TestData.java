package no.nav.nom.apps.nomremedyintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeEndOfDay;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
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

    public static RessursOpprettetEventV1 ressursOpprettetEventV1(Origination origination) {
        return new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData(),
            inputMetadata(origination),
            ressursOpprettetAdditionalData()
        );
    }

    public static RessursOpprettetEventV1.RessursOpprettetEventData ressursOpprettetEventData() {
        return new RessursOpprettetEventV1.RessursOpprettetEventData(
            "11111111111",
            Sektor.EKSTERN,
            new Visningsnavn(
                "Fornavn", Optional.empty(), "Etternavn"
            ),
            List.of("2021"),
            "2021"
        );
    }

    public static RessursSluttetEventV1 ressursSluttetEventV1() {
        return new RessursSluttetEventV1(
            UUID.randomUUID().toString(),
            new RessursSluttetEventV1.RessursSluttetEventData(LocalDate.now()),
            inputMetadata()
        );
    }

    public static RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertV1() {
        return ressursPersonaliaOppdatertV1(
            new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(new Visningsnavn(
                "Fornavn", Optional.empty(), "Etternavn"
            ))
        );
    }

    public static RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertV1(RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData personaliaOppdatertEventData) {
        return new RessursPersonaliaOppdatertV1(
            UUID.randomUUID().toString(),
            personaliaOppdatertEventData,
            inputMetadata()
        );
    }

    public static RessursFysiskeBrukerstederOppdatertEventV1 ressursFysiskeBrukerstederOppdatertEventV1() {
        return ressursFysiskeBrukerstederOppdatertEventV1(ressursFysiskeBrukerstederOppdatertEventData());
    }

    public static RessursFysiskeBrukerstederOppdatertEventV1 ressursFysiskeBrukerstederOppdatertEventV1(
        RessursFysiskeBrukerstederOppdatertEventV1.RessursFysiskeBrukerstederOppdatertEventData ressursFysiskeBrukerstederOppdatertEventData
    ) {
        return new RessursFysiskeBrukerstederOppdatertEventV1(
            UUID.randomUUID().toString(),
            ressursFysiskeBrukerstederOppdatertEventData,
            inputMetadata()
        );
    }

    public static RessursFysiskeBrukerstederOppdatertEventV1.RessursFysiskeBrukerstederOppdatertEventData ressursFysiskeBrukerstederOppdatertEventData() {
        return RessursFysiskeBrukerstederOppdatertEventV1.RessursFysiskeBrukerstederOppdatertEventData.builder()
            .primaryFysiskBrukersted("leikanger")
            .fysiskeBrukersteder(List.of("leikanger"))
            .build();
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

    public static OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEventV1(
        Origination origination
    ) {
        return new OrgtilknytningDagligOppfolgingEndretEventV1(
            UUID.randomUUID().toString(),
            orgtilknytningDagligOppfolgingEndretEventData(),
            inputMetadata(origination),
            null
        );
    }

    public static OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData orgtilknytningDagligOppfolgingEndretEventData() {
        return OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData.builder()
            .navident("Z654321")
            .orgEnhetId("orgenhet")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(11)))
            .dagligOppfolging(true)
            .build();
    }

    public static OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2() {
        return orgtilknytningDatoEndretEventV2(orgtilknytningDatoEndretEventData());
    }

    public static OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2(
        OrgtilknytningDatoEndretEventV2.OrgtilknytningDatoEndretEventData orgtilknytningDatoEndretEventData
    ) {
        return new OrgtilknytningDatoEndretEventV2(
            UUID.randomUUID().toString(),
            orgtilknytningDatoEndretEventData,
            inputMetadata(),
            null
        );
    }

    public static OrgtilknytningDatoEndretEventV2.OrgtilknytningDatoEndretEventData orgtilknytningDatoEndretEventData() {
        return OrgtilknytningDatoEndretEventV2.OrgtilknytningDatoEndretEventData.builder()
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(11)))
            .build();
    }

    public static InputMetadata inputMetadata(){
        return inputMetadata(origination());
    }

    public static InputMetadata inputMetadata(Origination origination){
        return new InputMetadata("", "", "", origination);
    }

    public static Origination origination() {
        return new Origination("X123456", "");
    }

    public static RessursOpprettetEventV1.RessursOpprettetAdditionalData ressursOpprettetAdditionalData() {
        return RessursOpprettetEventV1.RessursOpprettetAdditionalData.builder()
            .navident(Optional.of("Z654321"))
            .startdato(Optional.of(LocalDate.now()))
            .orgEnhet(Optional.of("orgenhet"))
            .build();
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
            .tiltredelsesdato(epochSecondsNorwegianTimeNoon(LocalDate.now()))
            .utlopsdato(epochSecondsNorwegianTimeEndOfDay(LocalDate.now().plusMonths(1)))
            .build();
    }
}
