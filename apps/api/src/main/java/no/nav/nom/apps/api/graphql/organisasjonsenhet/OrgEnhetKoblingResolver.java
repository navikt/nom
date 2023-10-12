package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrgEnhetKoblingResolver {
    private final OrgtilknytningService orgtilknytningStateService;
    private final RessursStateRepository ressursStateRepository;

    public OrgEnhetKoblingResolver(OrgtilknytningService orgtilknytningStateService, RessursStateRepository ressursStateRepository) {
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.ressursStateRepository = ressursStateRepository;
    }

    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet) {
        return orgtilknytningStateService.getByEnhet(orgEnhet.getId())
            .stream()
            .map(this::tilOrgEnhetsKobling)
            .toList();
    }

    private OrgEnhetsKobling tilOrgEnhetsKobling(OrgtilknytningState orgtilknytning) {
        return OrgEnhetsKobling.builder()
            .ressurs(MappingUtils.toRessurs(ressursStateRepository.findByNavident(orgtilknytning.navident()).get()))
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom().orElse(null))
            .build();
    }
}
