package no.nav.nom.apps.testdata.service;

import no.nav.nom.apps.testdata.model.KomplettTestSett;
import no.nav.nom.apps.testdata.model.TestSett;
import no.nav.nom.apps.testdata.repository.TestSettRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class TestSettService {
    private final TestSettRepository testSettRepository;
    private final OrgenhetStateService stateService;
    private final OrgenhetEvtsrcMsgService evtsrcMsgService;
    private final TestSettPessimisticLocking testSettPessimisticLocking;
    private final AuditService auditService;

    public TestSettService(
        TestSettRepository testSettRepository, OrgenhetStateService stateService,
        OrgenhetEvtsrcMsgService evtsrcMsgService, TestSettPessimisticLocking testSettPessimisticLocking,
        AuditService auditService
    ) {
        this.testSettRepository = testSettRepository;
        this.stateService = stateService;
        this.evtsrcMsgService = evtsrcMsgService;
        this.testSettPessimisticLocking = testSettPessimisticLocking;
        this.auditService = auditService;
    }

    @Transactional
    public TestSett opprett(TestSett.Data data) {
        return testSettRepository.create(data, auditService.endring());
    }

    public TestSett hent(long id) {
        return testSettRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Fant ikke testsett"));
    }

    public List<TestSett> finnAlle() {
        return testSettRepository.findAll();
    }

    @Transactional
    public TestSett endre(long id, TestSett.Data data) {
        testSettPessimisticLocking.lockTestSett(id);
        return testSettRepository.update(id, data, auditService.endring())
            .orElseThrow(() -> new NoSuchElementException("Fant ikke testsett"));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public KomplettTestSett hentKomplett(long id) {
        return new KomplettTestSett(hent(id), stateService.finnAlle(id), evtsrcMsgService.finnAlle(id));
    }

    @Transactional
    public void slett(long id) {
        testSettPessimisticLocking.lockTestSett(id);
        evtsrcMsgService.slettAlle(id);
        stateService.slettAlle(id, true);
        testSettRepository.delete(id);
    }
}
