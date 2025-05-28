package no.nav.nom.apps.api.common;

import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class TestData {

    public static OrgtilknytningState orgtilknytningState() {
        return OrgtilknytningState.builder()
            .fid(UUID.randomUUID().toString())
            .navident("X123456")
            .orgEnhetId("orgEnhet123")
            .sektor(Sektor.NAV_STATLIG)
            .erDagligOppfolging(false)
            .gyldigFom(LocalDate.now().minusMonths(1))
            .gyldigTom(Optional.empty())
            .orgtilknytningType(Optional.of(Tilknytningtype.FAST))
            .masseflyttetFraFid(Optional.empty())
            .build();
    }

    public static OrgEnhetState orgEnhetState() {
        return new OrgEnhetState(
            UUID.randomUUID().toString(),
            "orgEnhet123",
            "Test enhet",
            LocalDate.now().minusMonths(1),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }

    public static OrgEnhetState orgEnhetState(String orgEnhetId) {
        return new OrgEnhetState(
            UUID.randomUUID().toString(),
            orgEnhetId,
            "Test enhet",
            LocalDate.now().minusMonths(1),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }

    public static RessursState ressursState() {
        return new RessursState(
            UUID.randomUUID().toString(),
            "11111111111",
            "X123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn("Nav", Optional.empty(), "Navesen"),
            new Jobbtelefon("12345678", TelefonType.NAV_TJENESTE_TELEFON),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );
    }

    public static OrganiseringState organiseringState() {
        return OrganiseringState.builder()
            .fid(UUID.randomUUID().toString())
            .id("nomId")
            .idOver("nomIdOver")
            .navn("orgEnhetNavn")
            .navnOver("orgEnhetOverNavn")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();
    }

    public static OrgEnhet orgEnhet() {
        return new OrgEnhet(orgEnhetState());
    }

    public static OrgEnhet orgEnhet(String orgEnhetId) {
        return new OrgEnhet(orgEnhetState(orgEnhetId));
    }
}
