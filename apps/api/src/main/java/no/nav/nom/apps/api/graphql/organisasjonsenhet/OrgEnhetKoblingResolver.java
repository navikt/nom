package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrgEnhetKoblingResolver {
    private final OrgtilknytningService orgtilknytningStateService;
    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;

    public static final String UNLEASH_ORGTILKNYTNING_BRUK_EVTSRC_STATE = "nom.orgtilknytning.bruk_evtsrc_state";
    private final Unleash unleash;


    public OrgEnhetKoblingResolver(OrgtilknytningService orgtilknytningStateService, DvhRepository dvhRepository, IRessursService ressursService, Unleash unleash) {
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.dvhRepository = dvhRepository;
        this.ressursService = ressursService;
        this.unleash = unleash;
    }

    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet) {
        if (unleash.isEnabled(UNLEASH_ORGTILKNYTNING_BRUK_EVTSRC_STATE)) {
            return orgtilknytningStateService.getOrgtilknytningerByEnhet(orgEnhet.getId())
                .stream()
                .map(this::tilOrgEnhetsKobling)
                .toList();
        } else {
            return dvhRepository.getKoblingerByAgressoId(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv())
                .stream()
                .map(kobling -> ressursService.findByNavident(kobling.getNavIdent(), ressurs -> tilOrgEnhetsKobling(ressurs, kobling)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        }
    }

    private OrgEnhetsKobling tilOrgEnhetsKobling(OrgtilknytningState orgtilknytning) {
        return OrgEnhetsKobling.builder()
            .ressurs(MappingUtils.toRessurs(ressursService.findByNavident(orgtilknytning.navident()).get()))
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom().orElse(null))
            .build();
    }

    private OrgEnhetsKobling tilOrgEnhetsKobling(RessursState ressurs, Orgtilknytning orgtilknytning) {
        return OrgEnhetsKobling.builder()
                .ressurs(MappingUtils.toRessurs(ressurs))
                .gyldigFom(orgtilknytning.getGyldigFom())
                .gyldigTom(orgtilknytning.getGyldigTom().orElse(null))
                .build();
    }
}
