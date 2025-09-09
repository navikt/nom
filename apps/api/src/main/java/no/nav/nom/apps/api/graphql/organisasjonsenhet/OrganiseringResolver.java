package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class OrganiseringResolver {
    private final OrganiseringStateRepository organiseringStateRepository;

    public OrganiseringResolver(OrganiseringStateRepository organiseringStateRepository) {
        this.organiseringStateRepository = organiseringStateRepository;
    }

    public List<Organisering> organiseringer(OrgEnhet orgEnhet, OrganiseringRetning retning) {
        if (orgEnhet.endedOnStartDate()) return Collections.emptyList();

        log.debug("Henter organiseringer for {} retning '{}' med ny implementasjon", orgEnhet.getId(), retning);
        return organiseringStateRepository.getOrganiseringerContainingId(orgEnhet.getId()).stream()
            .map(organiseringState -> convert(organiseringState, orgEnhet))
            .filter(organisering -> retning == null || organisering.getRetning() == retning)
            .toList();
    }

    private Organisering convert(OrganiseringState organisering, OrgEnhet orgEnhet) {
        var retning = OrganiseringRetning.over;

        if (organisering.idOver().equals(orgEnhet.getId())) {
            retning = OrganiseringRetning.under;
        }

        return Organisering.builder()
            .id(retning == OrganiseringRetning.over ? organisering.idOver() : organisering.id())
            .retning(retning)
            .orgEnhetNr(retning == OrganiseringRetning.over ? organisering.idOver() : organisering.id())
            .orgNiv("")
            .orgEnhetNavn(retning == OrganiseringRetning.over ? organisering.navnOver() : organisering.navn())
            .gyldigFom(organisering.gyldigFom())
            .gyldigTom(organisering.gyldigTom())
            .build();
    }
}