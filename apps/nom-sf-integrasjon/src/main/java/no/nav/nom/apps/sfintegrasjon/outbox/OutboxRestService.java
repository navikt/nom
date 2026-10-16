package no.nav.nom.apps.sfintegrasjon.outbox;

import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import no.nav.nom.apps.sfintegrasjon.outbox.repository.SfOutboxRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class OutboxRestService {

    private final SfOutboxRepository sfOutboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxRestService(SfOutboxRepository sfOutboxRepository, ObjectMapper objectMapper) {
        this.sfOutboxRepository = sfOutboxRepository;
        this.objectMapper = objectMapper;
    }

    public List<SfOutboxResponse> getAllSfOutBoxRows() {
        return tilResponse(sfOutboxRepository.findAll());
    }

    public List<SfOutboxResponse> getAllByOrgEnhetId(String orgEnhetId) {
        return tilResponse(sfOutboxRepository.getAllByOrgEnhetId(orgEnhetId));
    }

    public List<SfOutboxResponse> getAllBySfStatus(SfStatus sfStatus) {
        return tilResponse(sfOutboxRepository.getAllBySfStatus(sfStatus));
    }

    public List<SfOutboxResponse> getAllRejected() {
        return tilResponse(sfOutboxRepository.getAllBySfStatus(SfStatus.AVVIST));
    }

    public List<SfOutboxResponse> getAllFailed() {
        return tilResponse(sfOutboxRepository.getAllBySfStatus(SfStatus.FEILET));
    }

    private List<SfOutboxResponse> tilResponse(List<SfOutbox> rader) {
        return rader.stream().map(this::tilResponse).toList();
    }

    // readTree, ikke SfOrgEnhet: AVVIST-rader kan ha JSON som ikke passer SfOrgEnhet, og jsonb er alltid gyldig JSON
    private SfOutboxResponse tilResponse(SfOutbox rad) {
        return new SfOutboxResponse(
            rad.getSporingsId(),
            rad.getSfStatus(),
            rad.getFeilmelding(),
            rad.getOrgEnhetId(),
            rad.getEndretAv(),
            rad.getEndringshendelser(),
            objectMapper.readTree(rad.getOrgenhet())
        );
    }
}
