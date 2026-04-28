package no.nav.nom.apps.nomremedyintegrasjon;

import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class TestData {

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
            new RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData(sluttdato),
            inputMetadata()
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
            .gyldigTom(LocalDate.now().plusMonths(11))
            .dagligOppfolging(true)
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

    public static RessursState ressursState(RessursOpprettetEventV1 v1) {
        return new RessursState(
            v1.getFid(),
            v1.getRessursOpprettetEventData().personident(),
            v1.getRessursOpprettetAdditionalData().navident(),
            v1.getRessursOpprettetEventData().sektor(),
            v1.getRessursOpprettetEventData().visningsnavn(),
            null,
            v1.getRessursOpprettetEventData().primaryTelefonnummer(),
            null,
            v1.getRessursOpprettetAdditionalData().startdato(),
            v1.getRessursOpprettetEventData().primaryFysiskBrukersted(),
            v1.getRessursOpprettetEventData().fysiskeBrukersteder(),
            IdentType.STANDARD,
            null,
            null
        );
    }

    public static RessursState ressursState() {
        return new RessursState(
            UUID.randomUUID().toString(),
            "12345688888",
            "S123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn("Nav", Optional.empty(), "Navnesen"),
            new Jobbtelefon("12345678", TelefonType.NAV_TJENESTE_TELEFON),
            null,
            null,
            LocalDate.now(),
            "2700",
            new ArrayList<>(),
            IdentType.STANDARD,
            null,
            null
        );
    }

    public static no.nav.nom.contract.nom.state.OrgtilknytningState orgtilknytningStateContract() {
        return new no.nav.nom.contract.nom.state.OrgtilknytningState(
            UUID.randomUUID().toString(),
            "S123456",
            "ab123c",
            Sektor.NAV_STATLIG,
            true,
            LocalDate.now(),
            LocalDate.now().plusMonths(10),
            Tilknytningtype.FAST,
            "2700",
            Collections.emptyList(),
            null,
            null
        );
    }
}