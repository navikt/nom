package no.nav.nom.apps.api.orgtilknytning.u4diff;


import no.nav.nom.apps.api.u4diff.U4Diff;
import no.nav.nom.apps.api.u4diff.U4DiffKilde;
import no.nav.nom.apps.api.u4diff.U4DiffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static no.nav.nom.apps.api.u4diff.U4DiffType.MANGLENDE_U4_PERIODE;
import static no.nav.nom.apps.api.u4diff.U4DiffType.ULIK_SEKTOR_RESSURS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class U4DiffRepositoryTest {

    @Autowired
    U4DiffRepository u4DiffRepository;

    @BeforeEach
    public void setUp() {
        u4DiffRepository.deleteAll();
    }

    @Test
    public void testInsertSelectDelete() {
        u4DiffRepository.save(U4Diff.builder().kilde(U4DiffKilde.STILLINGER).navident("A00001").type(MANGLENDE_U4_PERIODE).u4OrgenhetId("a123bc").u4GyldigFom(LocalDate.now()).build());
        u4DiffRepository.save(U4Diff.builder().kilde(U4DiffKilde.RESSURS).navident("A00001").type(ULIK_SEKTOR_RESSURS).build());
        u4DiffRepository.save(U4Diff.builder().kilde(U4DiffKilde.STILLINGER).navident("A00002").type(MANGLENDE_U4_PERIODE).build());

        assertEquals(1, u4DiffRepository.findByNavident("A00002").size());
        assertEquals(2, u4DiffRepository.findByNavident("A00001").size());
        assertEquals(1, u4DiffRepository.findByKildeAndNavident(U4DiffKilde.RESSURS, "A00001").size());
        assertEquals(LocalDate.now(), u4DiffRepository.findByNavident("A00001").getFirst().getU4GyldigFom());

        u4DiffRepository.deleteByKildeAndNavident(U4DiffKilde.RESSURS, "A00001");
        assertEquals(1, u4DiffRepository.findByNavident("A00001").size());
        assertEquals(0, u4DiffRepository.findByKildeAndNavident(U4DiffKilde.RESSURS, "A00001").size());
    }
}
