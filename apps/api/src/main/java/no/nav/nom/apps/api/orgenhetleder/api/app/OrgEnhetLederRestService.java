package no.nav.nom.apps.api.orgenhetleder.api.app;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrgEnhetLederRestService {
    private final OrgEnhetLederService orgEnhetLederService;

    public List<OrgEnhetLederResponse> getLederByNavident(String navident) {
        return orgEnhetLederService.getLederByNavident(navident);
    }

    public List<OrgEnhetLederResponse> getLederByOrgEnhetId(String orgEnhetId) {
        return orgEnhetLederService.getLederByOrgEnhetId(orgEnhetId);
    }

    public Optional<OrgEnhetLederResponse> getActivLederForOrgEnhet(String orgEnhetId) {
        return orgEnhetLederService.getActivLederForOrgEnhet(orgEnhetId);
    }
}
