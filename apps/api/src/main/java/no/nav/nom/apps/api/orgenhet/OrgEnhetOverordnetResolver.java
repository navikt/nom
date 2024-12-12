package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrgEnhetOverordnetResolver {

    private final OrganiseringStateRepository organiseringStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public OrgEnhetOverordnetResolver(
        OrganiseringStateRepository organiseringStateRepository, OrgEnhetStateRepository orgEnhetStateRepository
    ) {
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    public Optional<OrgEnhetState> findOverordnetOrgEnhet(OrgEnhetState orgEnhetState) {
        // Ikke returner overordnet/organisering for enheter som er opprettet og avsluttet samme dag
        if (orgEnhetState.endedOnStartDate()) { return Optional.empty(); }

        return organiseringStateRepository
            .findActiveById(orgEnhetState.id())
            .flatMap(o -> orgEnhetStateRepository.findById(o.idOver()));
    }


    public Optional<OrgEnhetState> findOverordnetOrgEnhet(String fid) {
        return orgEnhetStateRepository.findByFid(fid)
            .flatMap(this::findOverordnetOrgEnhet);
    }
}