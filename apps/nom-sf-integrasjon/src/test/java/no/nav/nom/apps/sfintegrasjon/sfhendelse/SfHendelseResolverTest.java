package no.nav.nom.apps.sfintegrasjon.sfhendelse;

import no.nav.nom.contract.nom.Tilknytningtype;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SfHendelseResolverTest {

    private String hendelse = """
                              {
                                      "navident": "A000001",
                                      "personident": "01460057589",
                                      "fornavn": "Kjell",
                                      "etternavn": "Nordmann",
                                      "startdato": "2026-12-01",
                                      "orgtilknytninger": [
                                          {
                                              "orgenhet": "ka923g",
                                              "type": "FAST",
                                              "erDagligOppfolging": true,
                                              "gyldigFom": "2026-12-02",
                                              "gyldigTom": "2027-12-01",
                                              "primaryFysiskBrukersted": "2800"
                                          },
                                          {
                                              "orgenhet": "ka923f",
                                              "type": "FAST",
                                              "erDagligOppfolging": true,
                                              "gyldigFom": "2026-10-01",
                                              "gyldigTom": "2026-12-01",
                                              "primaryFysiskBrukersted": "2700"
                                          }
                                      ]
                                  }
                              """.strip();

    private SfHendelseResolver resolver = new SfHendelseResolver(new ObjectMapper()).from(hendelse);

    @Test
    public void henterPersonident() {
        assertEquals("01460057589", resolver.personident());
    }

    @Test
    public void henterVisningsnavn() {
        assertEquals("Kjell Nordmann", resolver.visningsnavn());
    }

    @Test
    public void henterRessursStartdato() {
        assertEquals(LocalDate.of(2026, 12, 1), resolver.startdato());
    }

    @Test
    public void henterPrimaryFsiskBrukersted() {
        assertEquals("2700", resolver.primaryFysiskBrukersted());
    }

    @Test
    public void henterDolOt() {
        assertEquals("ka923f", resolver.dolOt());
    }

    @Test
    public void mapperOterTilStillinger() {
        var stillinger = resolver.sfStillinger();

        assertEquals(2, stillinger.size());

        var stilling = stillinger.stream().filter(s -> s.orgenhetId().equals("ka923g")).findFirst();

        assert(stilling.isPresent());
        assertEquals(LocalDate.parse("2026-12-02"), stilling.get().gyldigFom());
        assertEquals(LocalDate.parse("2027-12-01"), stilling.get().gyldigTom());
        assertEquals(Tilknytningtype.FAST, stilling.get().tilknytningtype());
        assertThat(stilling.get().erDol()).isTrue();
        assertEquals("2800", stilling.get().primaryFysiskBrukersted());
    }
}
