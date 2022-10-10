package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
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
        return dvhRepository.getKoblingerByAgressoId(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv()).stream()
                .map(kobling -> ressursService.findByNavIdent(kobling.getNavIdent(), ressurs -> tilOrgEnhetsKobling(ressurs, kobling)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private OrgEnhetsKobling tilOrgEnhetsKobling(IRessurs ressurs, Kobling kobling) {
        return OrgEnhetsKobling.builder()
                .ressurs(ressursService.convertToGraphQlResponse(ressurs))
                .gyldigFom(kobling.getGyldigFom())
                .gyldigTom(kobling.getGyldigTom().orElse(null))
                .build();
    }
}
