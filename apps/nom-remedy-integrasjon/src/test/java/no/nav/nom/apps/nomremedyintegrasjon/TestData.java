package no.nav.nom.apps.nomremedyintegrasjon;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class TestData {

    public static RessursOpprettetEventV1 ressursOpprettetEventV1() {
        return new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData(),
            inputMetadata(),
            ressursOpprettetAdditionalData()
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
}
