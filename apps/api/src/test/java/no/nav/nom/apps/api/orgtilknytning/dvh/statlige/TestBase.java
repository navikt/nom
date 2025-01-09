package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.OrgtilknytningCmdServiceTestBase;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.u4diff.U4DiffPaaMastrede;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

@SpringBootTest(classes = {
    NyeRessursStillingStrukturerer.class,
    GrupperOgSlaaSammenDvhStillinger.class,
    MatchStrukturerteStillingPerioderTilNOM.class,
    SettDOLPerioderPaaStrukturerteStillinger.class
})
public class TestBase extends OrgtilknytningCmdServiceTestBase {
    static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);
    static final String A000001 = "A000001";

    @MockitoBean
    OrgenhetService orgenhetService;
    @MockitoBean
    RessursStateRepository ressursStateRepository;
    @MockitoBean
    OrgtilknytningStateRepository orgtilknytningStateRepository;
    @MockitoBean
    OrgtilknytningService orgtilknytningService;
    @MockitoBean
    NomMastered nomMastered;
    @MockitoBean
    U4DiffPaaMastrede u4DiffPaaMastrede;

    private Random rnd = new Random();

    OrgtilknytningState orgtilknytning(String fid, String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgtilknytning(fid, navident, orgEnhetId, gyldigFom, gyldigTom, true);
    }

    OrgtilknytningState orgtilknytning(String fid, String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom, boolean erDagligOppfolging) {
        return OrgtilknytningState.builder()
            .fid(fid)
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .gyldigFom(gyldigFom)
            .gyldigTom(Optional.ofNullable(gyldigTom))
            .erDagligOppfolging(erDagligOppfolging)
            .build();
    }

    OrgtilknytningState orgtilknytning(String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgtilknytning(navident, orgEnhetId, gyldigFom, gyldigTom, true);
    }

    OrgtilknytningState orgtilknytning(String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom, boolean erDagligOppfolging) {
        return orgtilknytning("FID_" + rnd.nextInt(1000), navident, orgEnhetId, gyldigFom, gyldigTom, erDagligOppfolging);
    }

    IOrgenhet orgenhet(String id, String navn, String agressoId) {
        return orgenhet(id, navn, agressoId, LocalDate.of(2010, 1, 1), null);
    }

    IOrgenhet orgenhet(String id, String navn, String agressoId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new OrgEnhet(OrgEnhetState.builder()
            .id(id)
            .navn(navn)
            .agressoId(Optional.ofNullable(agressoId))
            .agressoOrgNivaa(Optional.of("ORGENHET"))
            .gyldigFom(gyldigFom)
            .gyldigTom(Optional.ofNullable(gyldigTom))
            .build()
        );
    }

    RessursState ressurs(String navident) {
        return ressurs(navident, null, null);
    }

    RessursState ressurs(String navident, LocalDate startDato) {
        return ressurs(navident, startDato, null);
    }

    RessursState ressurs(String navident, LocalDate startDato, LocalDate sluttDato) {
        return RessursState.builder()
            .navident(navident)
            .startdato(Optional.ofNullable(startDato))
            .sluttdato(Optional.ofNullable(sluttDato))
            .sektor(Sektor.NAV_STATLIG)
            .build();
    }

    DvhRessursStilling dvhRessursStilling(String navid, String stillingAgressoId, String stillingOrgenhetNavn, LocalDate from, LocalDate to) {
        return dvhRessursStilling(navid, stillingAgressoId, stillingOrgenhetNavn, stillingAgressoId, "ORGENHET", from, to);
    }

    DvhRessursStilling dvhRessursStilling(String navid, String stillingAgressoId, String stillingOrgenhetNavn, String orgenhetPaaReessurs, LocalDate from, LocalDate to) {
        return dvhRessursStilling(navid, stillingAgressoId, stillingOrgenhetNavn, orgenhetPaaReessurs, "ORGENHET", from, to);
    }

    DvhRessursStilling dvhRessursStilling(
        String navid,
        String stillingAgressoId,
        String stillingOrgenhetNavn,
        String ressursAgressoId,
        String ressursOrgenhetNiv,
        LocalDate from,
        LocalDate to
    ) {
        return DvhRessursStilling.builder()
            .navident(Optional.of(navid))
            .stillingAgressoId(Optional.ofNullable(stillingAgressoId))
            .stillingOrgenhetNavn(stillingOrgenhetNavn)
            .ressursAgressoId(Optional.ofNullable(ressursAgressoId))
            .ressursOrgenhetNiv(Optional.ofNullable(ressursOrgenhetNiv))
            .stillingDatoFom(from)
            .stillingDatoTom(to)
            .build();
    }

    StillingPeriode stillingPeriode(String agressoId, String navn, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(
                Optional.of(agressoId),
                navn,
                Optional.of(agressoId),
                Optional.of("ORGENHET")
            ),
            gyldigFom,
            gyldigTom,
            null
        );
    }

    StillingPeriode stillingPeriode(String agressoId, String ressursAgressoId, String navn, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(
                Optional.ofNullable(agressoId),
                navn,
                Optional.ofNullable(ressursAgressoId),
                Optional.of("ORGENHET")
            ),
            gyldigFom,
            gyldigTom,
            null
        );
    }

    StillingPeriode stillingPeriode(IOrgenhet orgenhet, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                        Optional.ofNullable(orgenhet.getAgressoId()),
                        orgenhet.getNavn(),
                        Optional.ofNullable(orgenhet.getAgressoId()),
                        Optional.of("ORGENHET")
                ),
                gyldigFom,
                gyldigTom,
                orgenhet
        );
    }

    StillingPeriode stillingPeriode(IOrgenhet orgenhet, LocalDate gyldigFom, LocalDate gyldigTom, StillingPeriode.DagligOppfolging dol) {
        return new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(
                Optional.ofNullable(orgenhet.getAgressoId()),
                orgenhet.getNavn(),
                Optional.ofNullable(orgenhet.getAgressoId()),
                Optional.of("ORGENHET")
            ),
            gyldigFom,
            gyldigTom,
            orgenhet,
            dol
        );
    }

    StillingPeriode stillingPeriode(IOrgenhet orgenhet, LocalDate gyldigFom, LocalDate gyldigTom, String ressursAgressoId) {
        return new StillingPeriode(
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                        Optional.ofNullable(orgenhet.getAgressoId()),
                        orgenhet.getNavn(),
                        Optional.ofNullable(ressursAgressoId),
                        Optional.of("ORGENHET")
                ),
                gyldigFom,
                gyldigTom,
                orgenhet
        );
    }

}
