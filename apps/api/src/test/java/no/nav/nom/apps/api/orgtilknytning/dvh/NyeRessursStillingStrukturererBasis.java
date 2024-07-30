package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.Optional;

@SpringBootTest(classes = { NyeRessursStillingStrukturerer.class })
class NyeRessursStillingStrukturererBasis {
    static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);
    static final String A000001 = "A000001";

    @Autowired
    NyeRessursStillingStrukturerer CUT; // Class under test
    @MockBean
    OrgenhetService orgenhetService;
    @MockBean
    RessursStateRepository ressursStateRepository;
    @MockBean
    RessursStillingLoadStatus ressursStillingLoadStatus;

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
            .build();
    }

    DvhRessursStilling dvhRessursStilling(String navid, String stillingAgressoId, String stillingOrgenhetNavn, LocalDate from, LocalDate to) {
        return dvhRessursStilling(navid, stillingAgressoId, stillingOrgenhetNavn, stillingAgressoId, "ORGENHET", from, to);
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
}
