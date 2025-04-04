package no.nav.nom.apps.api.ressurs.api.admin;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Sektor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class MastreStatligeINomTest {

    @Test
    void getRessursRow() {
        MastreStatligeINom mastreStatligeINom = new MastreStatligeINom(null, null, null);
        var list = List.of(
            new MastreStatligeINom.Ressurs("123456789", Sektor.NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01")),
            new MastreStatligeINom.Ressurs("123456789", Sektor.NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01")),
            new MastreStatligeINom.Ressurs("123456789", Sektor.NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01"))
        );

        StringBuffer sb = new StringBuffer();
        list.forEach(MastreStatligeINom.getRessursRow(sb));

        log.info("Resultat: {}", sb);
        assertEquals("123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n", sb.toString());
    }
}