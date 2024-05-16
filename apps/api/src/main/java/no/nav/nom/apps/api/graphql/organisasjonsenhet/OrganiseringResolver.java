package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static no.nav.nom.apps.api.organisering.OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE;

@Component
public class OrganiseringResolver {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DvhRepository dvhRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final Unleash unleash;


    public OrganiseringResolver(DvhRepository dvhRepository, OrganiseringStateRepository organiseringStateRepository, Unleash unleash) {
        this.dvhRepository = dvhRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.unleash = unleash;
    }

    public List<Organisering> organiseringer(OrgEnhet orgEnhet, OrganiseringRetning retning) {
        if (orgEnhet.endedOnStartDate()) return Collections.emptyList();

        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
            logger.debug("Henter organiseringer for {} retning '{}' med ny implementasjon", orgEnhet.getId(), retning);
            return organiseringStateRepository.getOrganiseringerContainingId(orgEnhet.getId()).stream()
                .map(organiseringState -> convert(organiseringState, orgEnhet))
                .filter(organisering -> retning == null || organisering.getRetning() == retning)
                .toList();

        } else {
            logger.debug("Henter organiseringer for {} retning '{}' med gammel implementasjon", orgEnhet.getId(), retning);
            return dvhRepository.getOrganiseringer(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv(), orgEnhet.getNavn())
                .stream()
                .map(organisering -> convert(organisering, orgEnhet))
                .filter(organisering -> retning == null || organisering.getRetning() == retning)
                .toList();
        }
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
            .gyldigTom(organisering.gyldigTom().orElse(null))
            .build();
    }

    private Organisering convert(DvhOrganisering organisering, OrgEnhet orgEnhet) {
        var retning = OrganiseringRetning.over;

        if (organisering.getAgressoIdOver().equals(orgEnhet.getAgressoId()) && organisering.getOrgNivOver()
            .equals(orgEnhet.getOrgNiv()) && organisering.getNavnOver().equals(orgEnhet.getNavn())) {
            retning = OrganiseringRetning.under;
        }

        return Organisering.builder()
            .id("")
            .retning(retning)
            .orgEnhetNr(retning == OrganiseringRetning.over ? organisering.getAgressoIdOver() : organisering.getAgressoIdUnder())
            .orgNiv(retning == OrganiseringRetning.over ? organisering.getOrgNivOver() : organisering.getOrgNivUnder())
            .orgEnhetNavn(retning == OrganiseringRetning.over ? organisering.getNavnOver() : organisering.getNavnUnder())
            .gyldigFom(organisering.getGyldigFom())
            .gyldigTom(organisering.getGyldigTom())
            .build();
    }
}