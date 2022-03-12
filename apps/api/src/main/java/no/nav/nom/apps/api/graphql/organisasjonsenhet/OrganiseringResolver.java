package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.toUnmodifiableList;

@Component
@RequiredArgsConstructor
public class OrganiseringResolver implements GraphQLResolver<Organisasjonsenhet> {

    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<Organisering>> organiseringer(Organisasjonsenhet orgEnhet, OrganiseringRetning retning) {
        val dvhOrganiseringer = dvhRepository.getOrganiseringer(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv(), orgEnhet.getNavn()).stream()
                .map(o -> convert(o, orgEnhet))
                .filter(o -> retning == null || o.getRetning() == retning)
                .collect(toUnmodifiableList());

        return DataFetcherResult.<List<Organisering>>newResult()
                .data(dvhOrganiseringer)
                .build();
    }

    private Organisering convert(DvhOrganisering organisering, Organisasjonsenhet orgEnhet) {
        val retning = organisering.getAgressoIdOver().equals(orgEnhet.getAgressoId()) && organisering.getOrgNivOver().equals(orgEnhet.getOrgNiv()) && organisering.getNavnOver().equals(orgEnhet.getNavn())
                ? OrganiseringRetning.under : OrganiseringRetning.over;

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
