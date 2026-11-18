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
    private final RessursStateService ressursStateService;
    private final RessursEvtsrcMsgService ressursEvtsrcMsgService;
    private final OrganiseringStateService organiseringStateService;
    private final OrganiseringEvtsrcMsgService organiseringEvtsrcMsgService;
    private final OrgEnhetLederStateService orgEnhetLederStateService;
    private final OrgEnhetLederEvtsrcMsgService orgEnhetLederEvtsrcMsgService;
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final OrgtilknytningEvtsrcMsgService orgtilknytningEvtsrcMsgService;
    private final TestSettPessimisticLocking testSettPessimisticLocking;
    private final AuditService auditService;

    public TestSettService(
        TestSettRepository testSettRepository, OrgenhetStateService stateService,
        OrgenhetEvtsrcMsgService evtsrcMsgService,
        RessursStateService ressursStateService, RessursEvtsrcMsgService ressursEvtsrcMsgService,
        OrganiseringStateService organiseringStateService, OrganiseringEvtsrcMsgService organiseringEvtsrcMsgService,
        OrgEnhetLederStateService orgEnhetLederStateService, OrgEnhetLederEvtsrcMsgService orgEnhetLederEvtsrcMsgService,
        OrgtilknytningStateService orgtilknytningStateService, OrgtilknytningEvtsrcMsgService orgtilknytningEvtsrcMsgService,
        TestSettPessimisticLocking testSettPessimisticLocking,
        AuditService auditService
    ) {
        this.testSettRepository = testSettRepository;
        this.stateService = stateService;
        this.evtsrcMsgService = evtsrcMsgService;
        this.ressursStateService = ressursStateService;
        this.ressursEvtsrcMsgService = ressursEvtsrcMsgService;
        this.organiseringStateService = organiseringStateService;
        this.organiseringEvtsrcMsgService = organiseringEvtsrcMsgService;
        this.orgEnhetLederStateService = orgEnhetLederStateService;
        this.orgEnhetLederEvtsrcMsgService = orgEnhetLederEvtsrcMsgService;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.orgtilknytningEvtsrcMsgService = orgtilknytningEvtsrcMsgService;
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
        return new KomplettTestSett(
            hent(id),
            stateService.finnAlle(id),
            evtsrcMsgService.finnAlle(id),
            ressursStateService.finnAlle(id),
            ressursEvtsrcMsgService.finnAlle(id),
            organiseringStateService.finnAlle(id),
            organiseringEvtsrcMsgService.finnAlle(id),
            orgEnhetLederStateService.finnAlle(id),
            orgEnhetLederEvtsrcMsgService.finnAlle(id),
            orgtilknytningStateService.finnAlle(id),
            orgtilknytningEvtsrcMsgService.finnAlle(id)
        );
    }

    @Transactional
    public void slett(long id) {
        testSettPessimisticLocking.lockTestSett(id);
        evtsrcMsgService.slettAlle(id);
        ressursEvtsrcMsgService.slettAlle(id);
        organiseringEvtsrcMsgService.slettAlle(id);
        orgEnhetLederEvtsrcMsgService.slettAlle(id);
        orgtilknytningEvtsrcMsgService.slettAlle(id);
        stateService.slettAlle(id, true);
        ressursStateService.slettAlle(id, true);
        organiseringStateService.slettAlle(id, true);
        orgEnhetLederStateService.slettAlle(id, true);
        orgtilknytningStateService.slettAlle(id, true);
        testSettRepository.delete(id);
    }
}
