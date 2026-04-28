package no.nav.nom.apps.api.orgenhetleder.api.app;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgenhetleder.api.app.dto.OpprettOrgEnhetLederRequest;
import no.nav.nom.apps.api.orgenhetleder.api.app.dto.OrgEnhetLederResponse;
import no.nav.nom.apps.api.orgenhetleder.api.app.exception.OrgEnhetLederNotFoundException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrgEnhetLederRestService {
    private final OrgEnhetLederService orgEnhetLederService;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;

    public List<OrgEnhetLederResponse> getLederByNavident(String navident) {
        return orgEnhetLederService.getOrgEnheterNavidentErLederFor(navident).stream()
            .map(this::enrichWithData)
            .toList();
    }

    public List<OrgEnhetLederResponse> getLederByOrgEnhetId(String orgEnhetId) {
        return orgEnhetLederService.getLedereForOrgEnhet(orgEnhetId).stream()
            .map(this::enrichWithData)
            .toList();
    }

    public Optional<OrgEnhetLederResponse> getActivLederForOrgEnhet(String orgEnhetId) {
        return orgEnhetLederService.getAktivLederForOrgEnhet(orgEnhetId)
            .map(this::enrichWithData);
    }

    private OrgEnhetLederResponse enrichWithData(OrgEnhetLederState orgEnhetLederState) {
        var ressursVisningsnavn = ressursStateRepository.findByNavident(orgEnhetLederState.navident()).map(RessursState::visningsnavn).orElse(null);
        var orgEnhetNavn = orgenhetService.getById(orgEnhetLederState.orgEnhetId()).map(IOrgenhet::getNavn).orElse(null);

        return OrgEnhetLederResponse.builder()
            .fid(orgEnhetLederState.fid())
            .navident(orgEnhetLederState.navident())
            .visningsnavn(ressursVisningsnavn)
            .orgEnhetId(orgEnhetLederState.orgEnhetId())
            .orgEnhetNavn(orgEnhetNavn)
            .startdato(orgEnhetLederState.startdato())
            .sluttdato(orgEnhetLederState.sluttdato())
            .build();
    }

    public OrgEnhetLederResponse opprettOrgEnhetLeder(OpprettOrgEnhetLederRequest opprettOrgEnhetLederRequest) {
        var ressurs = ressursStateRepository.findByNavident(opprettOrgEnhetLederRequest.navident())
            .orElseThrow(() -> RessursNotFoundException.navident(opprettOrgEnhetLederRequest.navident()));

        var orgEnhet = orgenhetService.getById(opprettOrgEnhetLederRequest.orgEnhetId())
            .orElseThrow(() -> OrgEnhetNotFoundException.id(opprettOrgEnhetLederRequest.orgEnhetId()));

        var newOrgEnhetLederState = orgEnhetLederService.persisterLederForOrgEnhet(opprettOrgEnhetLederRequest)
            .orElseThrow(() -> OrgEnhetLederNotFoundException.orgEnhetId(opprettOrgEnhetLederRequest.orgEnhetId()));

        return new OrgEnhetLederResponse(
            newOrgEnhetLederState.fid(),
            newOrgEnhetLederState.navident(),
            ressurs.visningsnavn(),
            newOrgEnhetLederState.orgEnhetId(),
            orgEnhet.getNavn(),
            newOrgEnhetLederState.startdato(),
            newOrgEnhetLederState.sluttdato()
        );
    }
}
