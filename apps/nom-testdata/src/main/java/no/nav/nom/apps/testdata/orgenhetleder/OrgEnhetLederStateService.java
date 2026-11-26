package no.nav.nom.apps.testdata.orgenhetleder;

import no.nav.nom.apps.testdata.common.AuditService;
import no.nav.nom.apps.testdata.common.RekkefolgeValidator;
import no.nav.nom.apps.testdata.orgenhetleder.repository.OrgEnhetLederStateRepository;
import no.nav.nom.apps.testdata.testsett.locking.TestSettPessimisticLocking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class OrgEnhetLederStateService {
    private final OrgEnhetLederStateRepository stateRepository;
    private final OrgEnhetLederEvtsrcMsgService evtsrcMsgService;
    private final TestSettPessimisticLocking testSettPessimisticLocking;
    private final AuditService auditService;

    public OrgEnhetLederStateService(
        OrgEnhetLederStateRepository stateRepository, OrgEnhetLederEvtsrcMsgService evtsrcMsgService,
        TestSettPessimisticLocking testSettPessimisticLocking, AuditService auditService
    ) {
        this.evtsrcMsgService = evtsrcMsgService;
        this.stateRepository = stateRepository;
        this.testSettPessimisticLocking = testSettPessimisticLocking;
        this.auditService = auditService;
    }

    @Transactional
    public OrgEnhetLederState opprett(long testSettId, OrgEnhetLederState.Data data) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        return stateRepository.create(testSettId, data, auditService.endring());
    }

    public OrgEnhetLederState hent(long testSettId, long id) {
        return stateRepository.findById(testSettId, id)
            .orElseThrow(() -> new NoSuchElementException("Fant ikke orgenhet-leder-state i testsettet"));
    }

    public List<OrgEnhetLederState> finnAlle(long testSettId) {
        testSettPessimisticLocking.requireExists(testSettId); // kaster NoSuchElementException hvis testsettet ikke finnes
        return stateRepository.findAll(testSettId);
    }

    public List<OrgEnhetLederState> finnForFid(long testSettId, String fid) {
        testSettPessimisticLocking.requireExists(testSettId); // kaster NoSuchElementException hvis testsettet ikke finnes
        return stateRepository.findByTestSetIdAndFid(testSettId, fid);
    }

    @Transactional
    public OrgEnhetLederState endre(long testSettId, long id, OrgEnhetLederState.Data data) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        return stateRepository.update(testSettId, id, data, auditService.endring())
            .orElseThrow(() -> new NoSuchElementException("Fant ikke orgenhet-leder-state i testsettet"));
    }

    @Transactional
    public void slett(long testSettId, long id, boolean slettTilknyttedeMeldinger) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        hent(testSettId, id);
        if (slettTilknyttedeMeldinger) {
            evtsrcMsgService.slettForState(testSettId, id);
        } else {
            evtsrcMsgService.frikobleFraState(testSettId, id, auditService.endring());
        }
        stateRepository.delete(testSettId, id);
    }

    @Transactional
    public int slettAlle(long testSettId, boolean slettTilknyttedeMeldinger) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        if (slettTilknyttedeMeldinger) {
            evtsrcMsgService.slettAlleMedStateReferanse(testSettId);
        } else {
            evtsrcMsgService.frikobleAlleFraState(testSettId, auditService.endring());
        }
        return stateRepository.deleteAllByTestSetId(testSettId);
    }

    @Transactional
    public void endreRekkefolge(long testSettId, List<Long> ids) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        var rekkefolge = RekkefolgeValidator.kontrollerRekkefolge(
            ids, stateRepository.findAll(testSettId), OrgEnhetLederState::id
        );
        stateRepository.deferSortOrderConstraint();
        var endring = auditService.endring();
        for (int i = 0; i < rekkefolge.size(); i++) {
            stateRepository.updateSortIndex(testSettId, rekkefolge.get(i), i, endring);
        }
    }

}
