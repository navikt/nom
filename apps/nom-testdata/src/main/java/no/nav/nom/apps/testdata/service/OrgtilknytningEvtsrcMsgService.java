package no.nav.nom.apps.testdata.service;

import no.nav.nom.apps.testdata.model.Audit;
import no.nav.nom.apps.testdata.model.OrgtilknytningEvtsrcMsg;
import no.nav.nom.apps.testdata.repository.OrgtilknytningEvtsrcMsgRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class OrgtilknytningEvtsrcMsgService {
    private final OrgtilknytningEvtsrcMsgRepository evtsrcRepository;
    private final TestSettPessimisticLocking testSettPessimisticLocking;
    private final AuditService auditService;

    public OrgtilknytningEvtsrcMsgService(
        TestSettPessimisticLocking testSettPessimisticLocking,
        OrgtilknytningEvtsrcMsgRepository meldingRepository, AuditService auditService
    ) {
        this.testSettPessimisticLocking = testSettPessimisticLocking;
        this.evtsrcRepository = meldingRepository;
        this.auditService = auditService;
    }

    @Transactional
    public OrgtilknytningEvtsrcMsg opprett(long testSettId, OrgtilknytningEvtsrcMsg.Data data) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        return evtsrcRepository.create(testSettId, data, auditService.endring());
    }

    public OrgtilknytningEvtsrcMsg hent(long testSettId, long id) {
        return evtsrcRepository.findById(testSettId, id)
            .orElseThrow(() -> new NoSuchElementException("Fant ikke melding i testsettet"));
    }

    public List<OrgtilknytningEvtsrcMsg> finnAlle(long testSettId) {
        testSettPessimisticLocking.requireExists(testSettId); // kaster NoSuchElementException hvis testsettet ikke finnes
        return evtsrcRepository.findAll(testSettId);
    }

    @Transactional
    public OrgtilknytningEvtsrcMsg endre(long testSettId, long id, OrgtilknytningEvtsrcMsg.Data data) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        return evtsrcRepository.update(testSettId, id, data, auditService.endring())
            .orElseThrow(() -> new NoSuchElementException("Fant ikke melding i testsettet"));
    }

    @Transactional
    public void slett(long testSettId, long id) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        if (evtsrcRepository.delete(testSettId, id) == 0) {
            throw new NoSuchElementException("Fant ikke melding i testsettet");
        }
    }

    @Transactional
    public int slettForState(long testSettId, long stateId) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        return evtsrcRepository.deleteByTestSetIdAndStateId(testSettId, stateId);
    }

    @Transactional
    public int slettAlle(long testSettId) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        return evtsrcRepository.deleteAllByTestSetId(testSettId);
    }

    @Transactional
    public int slettAlleMedStateReferanse(long testSettId) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        return evtsrcRepository.deleteAllWithStateReferenceByTestSetId(testSettId);
    }

    @Transactional
    public void endreRekkefolge(long testSettId, List<Long> ids) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        var rekkefolge = RekkefolgeValidator.kontrollerRekkefolge(
            ids, evtsrcRepository.findAll(testSettId), OrgtilknytningEvtsrcMsg::id
        );
        evtsrcRepository.deferSortOrderConstraint();
        var endring = auditService.endring();
        for (int i = 0; i < rekkefolge.size(); i++) {
            evtsrcRepository.updateSortIndex(testSettId, rekkefolge.get(i), i, endring);
        }
    }

    @Transactional
    public void frikobleFraState(long testSettId, long id, Audit.Endring endring) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        evtsrcRepository.detachFromState(testSettId, id, endring);
    }

    @Transactional
    public void frikobleAlleFraState(long testSettId, Audit.Endring endring) {
        testSettPessimisticLocking.lockTestSett(testSettId);
        evtsrcRepository.detachAllStateReferences(testSettId, endring);
    }

}
