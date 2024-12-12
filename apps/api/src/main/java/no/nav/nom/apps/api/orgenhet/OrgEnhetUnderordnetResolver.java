package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class OrgEnhetUnderordnetResolver {
    private final Logger logger = LoggerFactory.getLogger(OrgEnhetUnderordnetResolver.class);
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;

    public OrgEnhetUnderordnetResolver(
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrganiseringStateRepository organiseringStateRepository
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
    }

    public List<OrgEnhetState> findUnderordnedeOrgEnheter(OrgEnhetState orgEnhetState) {
        // Ikke returner overordnet/organisering for enheter som er opprettet og avsluttet samme dag
        if (orgEnhetState.endedOnStartDate()) { return Collections.emptyList(); }

        return organiseringStateRepository.getOrganiseringerContainingId(orgEnhetState.id()).stream()
            .filter(organiseringState -> organiseringState.idOver().equals(orgEnhetState.id()))
            .peek(organiseringState -> {
                if (organiseringState.gyldigTom().toString().contains("2024-02-25") || organiseringState.id().equals("py458c")) {
                    logger.warn("Fant en utgåtte organisering med id: {}", organiseringState);
                }
            })
            .map(organiseringState -> orgEnhetStateRepository.findById(organiseringState.id()))
            .filter(matchedOrgEnhetState -> matchedOrgEnhetState.isPresent() && matchedOrgEnhetState.get().isActive()) // sjekker også orgnhet er aktiv!
            .peek(matchedOrgEnhetState -> {
                if (matchedOrgEnhetState.get().id().equals("py458c")) {
                    logger.warn("Fant en rart orgEnhetState : {}", matchedOrgEnhetState);
                }
            })
            .flatMap(Optional::stream)
            .distinct()
            .toList();
    }

    public List<OrgEnhetState> findUnderordnedeOrgEnheter(String fid) {
        var orgEnhetState = orgEnhetStateRepository.findByFid(fid);

        return orgEnhetState.isPresent() ? findUnderordnedeOrgEnheter(orgEnhetState.get()) : Collections.emptyList();
    }
}