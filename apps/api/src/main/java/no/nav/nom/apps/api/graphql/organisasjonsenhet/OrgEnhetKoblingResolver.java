package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.orgTilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrgEnhetKoblingResolver {

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;

    public OrgEnhetKoblingResolver(DvhRepository dvhRepository, IRessursService ressursService) {
        this.dvhRepository = dvhRepository;
        this.ressursService = ressursService;
    }

    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet) {
        return dvhRepository.getKoblingerByAgressoId(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv())
                .stream()
                .map(kobling -> ressursService.findByNavident(kobling.getNavIdent(), ressurs -> tilOrgEnhetsKobling(ressurs, kobling)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private OrgEnhetsKobling tilOrgEnhetsKobling(RessursState ressurs, Orgtilknytning orgtilknytning) {
        return OrgEnhetsKobling.builder()
                .ressurs(MappingUtils.toRessurs(ressurs))
                .gyldigFom(orgtilknytning.getGyldigFom())
                .gyldigTom(orgtilknytning.getGyldigTom().orElse(null))
                .build();
    }
}
