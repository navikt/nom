package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrganiseringResolver {

    private final DvhRepository dvhRepository;

    public OrganiseringResolver(DvhRepository dvhRepository) {
        this.dvhRepository = dvhRepository;
    }

    public List<Organisering> organiseringer(OrgEnhet orgEnhet, OrganiseringRetning retning) {
        return dvhRepository.getOrganiseringer(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv(), orgEnhet.getNavn())
                .stream()
                .map(organisering -> convert(organisering, orgEnhet))
                .filter(organisering -> retning == null || organisering.getRetning() == retning)
                .toList();
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
