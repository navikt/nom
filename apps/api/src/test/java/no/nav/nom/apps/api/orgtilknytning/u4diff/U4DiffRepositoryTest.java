package no.nav.nom.apps.api.orgtilknytning.u4diff;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static no.nav.nom.apps.api.orgtilknytning.u4diff.U4DiffType.MANGLENDE_U4_PERIODE;
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
        u4DiffRepository.save(U4Diff.builder().navident("A00001").type(MANGLENDE_U4_PERIODE).u4OrgenhetId("a123bc").u4GyldigFom(LocalDate.now()).build());
        u4DiffRepository.save(U4Diff.builder().navident("A00001").type(MANGLENDE_U4_PERIODE).build());
        u4DiffRepository.save(U4Diff.builder().navident("A00002").type(MANGLENDE_U4_PERIODE).build());

        assertEquals(1, u4DiffRepository.findByNavident("A00002").size());
        assertEquals(2, u4DiffRepository.findByNavident("A00001").size());
        assertEquals(LocalDate.now(), u4DiffRepository.findByNavident("A00001").getFirst().getU4GyldigFom());

        u4DiffRepository.deleteByNavident("A00001");
        assertEquals(0, u4DiffRepository.findByNavident("A00001").size());
    }
}
