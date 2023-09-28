package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@Service
public class OrgEnhetDvhService {

    private final DvhRepository dvhRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public OrgEnhetDvhService(DvhRepository dvhRepository, OrgEnhetStateRepository orgEnhetStateRepository) {
        this.dvhRepository = dvhRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    public List<String> findAllUnderliggendeOrgenheter(String id) {
        Set<String> underliggendeAgressoIds = orgEnhetStateRepository.findById(id).map(parent -> {
            if (parent.agressoId().isEmpty() | parent.agressoOrgNivaa().isEmpty() ) {
                return Collections.<String>emptySet();
            }
            var organiseringer = dvhRepository.getOrganiseringer(parent.agressoId().get(), parent.agressoOrgNivaa().get(), parent.navn());
            var allUnderliggende = new java.util.ArrayList<String>(emptyList());

            var nextLevelChildren = organiseringer.stream().filter(organisering -> organisering.getAgressoIdOver().equals(parent.agressoId().get())).collect(
                Collectors.toSet());


            while (!nextLevelChildren.isEmpty()) {
                allUnderliggende.addAll(nextLevelChildren.stream().map(DvhOrganisering::getAgressoIdUnder).toList());
                nextLevelChildren = findChildOrganiseringer(nextLevelChildren);
            }

            return new HashSet<>(allUnderliggende);
        }).orElse(emptySet());

        return orgEnhetStateRepository.findAllByAgressoIdIn(underliggendeAgressoIds).stream().map(OrgEnhetState::id).toList();
    }

    private Set<DvhOrganisering> findChildOrganiseringer(Set<DvhOrganisering> parentOrganiseringer) {
        return parentOrganiseringer.stream()
            .map(organisering ->
                dvhRepository.getOrganiseringer(organisering.getAgressoIdUnder(), organisering.getOrgNivUnder(), organisering.getNavnUnder())
                .stream()
                    .filter(
                        lowerOrganisering ->
                            !lowerOrganisering.equals(organisering) &&
                                lowerOrganisering.getAgressoIdOver().equals(organisering.getAgressoIdUnder())
                    ).toList()
            ).flatMap(List::stream).collect(Collectors.toSet());
    }
}
