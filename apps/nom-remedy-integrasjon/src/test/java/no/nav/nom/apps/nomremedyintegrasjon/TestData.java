package no.nav.nom.apps.nomremedyintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPrimaryTelefonnummerOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.generated.model.OrgEnhet;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.Ressurs;

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
            Optional.of("12345678"),
            List.of("2021"),
            "2021"
        );
    }

    public static RessursSluttetEventV1 ressursSluttetEventV1(LocalDate sluttdato) {
        return new RessursSluttetEventV1(
            UUID.randomUUID().toString(),
            new RessursSluttetEventV1.RessursSluttetEventData(sluttdato),
            inputMetadata()
        );
    }

    public static RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1(LocalDate sluttdato) {
        return new RessursSluttdatoOppdatertEventV1(
            UUID.randomUUID().toString(),
            new RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData(Optional.of(sluttdato)),
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

    public static RessursPrimaryTelefonnummerOppdatertEventV1 ressursPrimaryTelefonnummerOppdatertV1(RessursPrimaryTelefonnummerOppdatertEventV1.PrimaryTelefonnummerOppdatertEventData primaryTelefonnummerOppdatertEventData) {
        return new RessursPrimaryTelefonnummerOppdatertEventV1(
            UUID.randomUUID().toString(),
            primaryTelefonnummerOppdatertEventData,
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
            .mobil("12345678")
            .build();
    }

    public static OrgEnhet orgenhet() {
        return new OrgEnhet()
            .remedyEnhetId("0123");
    }

    public static Ressurs ressurs() {
        return new Ressurs()
            .navident("X123456")
            .personident("12345678912")
            .visningsnavn(new no.nav.nom.generated.model.Visningsnavn()
                .fornavn("fornavn")
                .etternavn("etternavn")
            )
            .primaryTelefonnummer("12345678")
            .primaryFysiskBrukersted("1234")
            .sektor(no.nav.nom.generated.model.Sektor.EKSTERN);
    }

    public static RessursState ressursState(RessursOpprettetEventV1 v1) {
        return new RessursState(
            v1.getFid(),
            v1.getRessursOpprettetEventData().personident(),
            v1.getRessursOpprettetAdditionalData().navident().get(),
            v1.getRessursOpprettetEventData().sektor(),
            v1.getRessursOpprettetEventData().visningsnavn(),
            v1.getRessursOpprettetEventData().primaryTelefonnummer(),
            Optional.empty(),
            v1.getRessursOpprettetAdditionalData().startdato(),
            Optional.of(v1.getRessursOpprettetEventData().primaryFysiskBrukersted()),
            v1.getRessursOpprettetEventData().fysiskeBrukersteder(),
            IdentType.STANDARD
        );
    }

    public static OrgtilknytningState orgtilknytningState() {
        return new OrgtilknytningState()
            .navident("X123456")
            .orgEnhetId("orgenhetId")
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(10))
            .erDagligOppfolging(true);
    }

    public static no.nav.nom.contract.nom.state.OrgtilknytningState orgtilknytningState(OrgtilknytningDagligOppfolgingEndretEventV1 v1) {
        return new no.nav.nom.contract.nom.state.OrgtilknytningState(
            v1.getFid(),
            v1.getOrgtilknytningDagligOppfolgingEndretEventData().navident(),
            v1.getOrgtilknytningDagligOppfolgingEndretEventData().orgEnhetId(),
            v1.getOrgtilknytningDagligOppfolgingEndretEventData().dagligOppfolging(),
            v1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigFom(),
            v1.getOrgtilknytningDagligOppfolgingEndretEventData().gyldigTom(),
            Optional.empty()
        );
    }
}