package no.nav.nom.apps.api.dvh.orgtilknytning;

import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.dvh.DvhTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;


public class OrgtilknytningTest extends DvhTestBase {

    @Test
    @DisplayName("Leser orgtilknytning fra DVH")
    public void test1() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").build();

        final var ressursState = createRessursHendelseEventSource("");
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhOrgtilknytningMockUtils.addToResponse(
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2099-12-31")
            )
        );
        loadFromDvh();

        Assertions.assertEquals(1, orgtilknytningService.getKoblingerByNavIdent(ressursState.navident()).size());
    }

    @Test
    @DisplayName("Overlappende orgtilknytninger blir slått sammen")
    public void test2() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").build();

        final var ressursState = createRessursHendelseEventSource("");
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhOrgtilknytningMockUtils.addToResponse(
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2099-12-31")
            ),
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2099-12-31")
            )
        );
        loadFromDvh();

        Assertions.assertEquals(1, orgtilknytningService.getKoblingerByNavIdent(ressursState.navident()).size());
    }

    @Test
    @DisplayName("Ikke Overlappende orgtilknytninger blir ikke slått sammen")
    public void test3() {
        dvhRepository.clearRepository();

        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").build();

        final var ressursState = createRessursHendelseEventSource("");
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhOrgtilknytningMockUtils.addToResponse(
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2099-12-31")
            ),
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2019-12-31")
            )
        );
        loadFromDvh();

        Assertions.assertEquals(2, orgtilknytningService.getKoblingerByNavIdent(ressursState.navident()).size());
    }
}
