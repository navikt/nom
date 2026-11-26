package no.nav.nom.apps.testdata.testsett.locking;

import no.nav.nom.apps.testdata.testsett.repository.TestSettRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class TestSettPessimisticLocking {
    private final TestSettRepository testSettRepository;

    public TestSettPessimisticLocking(TestSettRepository testSettRepository) {
        this.testSettRepository = testSettRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockTestSett(long id) {
        testSettRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NoSuchElementException("Fant ikke testsett"));
    }

    @Transactional(readOnly = true)
    public void requireExists(long id) {
        testSettRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Fant ikke testsett"));
    }
}
