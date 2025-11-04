package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.Util;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.List;

import static no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg.AKTIVE;

@Component
public class OrgtilknytningResolver {
    private final OrgtilknytningService orgtilknytningService;
    private final RessursStateRepository ressursStateRepository;

    public OrgtilknytningResolver(OrgtilknytningService orgtilknytningService, RessursStateRepository ressursStateRepository) {
        this.orgtilknytningService = orgtilknytningService;
        this.ressursStateRepository = ressursStateRepository;
    }

    public List<OrgTilknytning> orgtilknytninger(OrgEnhet orgEnhet, PeriodiskUtvalgArg periodiskUtvalg) {
        return orgtilknytningService.getByEnhet(orgEnhet.getId()).stream()
            .filter(o -> Util.filterByPeriodiskUtvalg(o, periodiskUtvalg))
            .map(this::tilOrgTilknytning)
            .toList();
    }

    private OrgTilknytning tilOrgTilknytning(OrgtilknytningState orgtilknytning) {
        return OrgTilknytning.builder()
            .ressurs(MappingUtils.toRessurs(ressursStateRepository.findByNavident(orgtilknytning.navident()).get()))
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom())
            .sektor(orgtilknytning.sektor())
            .tilknytningType(orgtilknytning.orgtilknytningType())
            .build();
    }

    // Deprecated - 2025-09-15
    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet) {
        return orgtilknytningService.getByEnhet(orgEnhet.getId())
            .stream()
            .map(this::tilOrgEnhetsKobling)
            .toList();
    }

    // Deprecated - 2025-09-15
    private OrgEnhetsKobling tilOrgEnhetsKobling(OrgtilknytningState orgtilknytning) {
        return OrgEnhetsKobling.builder()
            .ressurs(MappingUtils.toRessurs(ressursStateRepository.findByNavident(orgtilknytning.navident()).get()))
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom())
            .build();
    }
}
