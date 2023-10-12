package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrgEnhetKoblingResolver {
    private final OrgtilknytningService orgtilknytningService;
    private final RessursStateRepository ressursStateRepository;

    public OrgEnhetKoblingResolver(OrgtilknytningService orgtilknytningService, RessursStateRepository ressursStateRepository) {
        this.orgtilknytningService = orgtilknytningService;
        this.ressursStateRepository = ressursStateRepository;
    }

    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet) {
        return orgtilknytningService.getByEnhet(orgEnhet.getId())
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
