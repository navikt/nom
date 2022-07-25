package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrgTilknytningResolver implements GraphQLResolver<Ressurs> {

    private final IOrgenhetService orgenhetService;
    private final KoblingService koblingService;

    public DataFetcherResult<List<RessursOrgTilknytning>> orgTilknytning(Ressurs ressurs, DataFetchingEnvironment env) {
        final List<RessursOrgTilknytning> result = koblingService.getKoblingerByNavIdent(ressurs.getNavIdent())
                .stream()
                .map(dvh -> {
                    val orgenhet = orgenhetService.getEnhetByAgressoIdNivaAndName(dvh.getAgressoId(), dvh.getOrgNiv(), null).get();
                    val orgEnhetGqlResponse = orgenhetService.convertToGraphQlResponse(orgenhet);
                    return RessursOrgTilknytning.builder()
                            .organisasjonsenhet(orgEnhetGqlResponse)
                            .orgEnhet(orgEnhetGqlResponse)
                            .gyldigFom(dvh.getGyldigFom())
                            .gyldigTom(dvh.getGyldigTom().orElse(null))
                            .build();
                }).toList();

        return DataFetcherResult.<List<RessursOrgTilknytning>>newResult()
                .data(result)
                .build();
    }

    public DataFetcherResult<List<RessursOrgTilknytning>> koblinger(Ressurs ressurs, DataFetchingEnvironment env) {
        return orgTilknytning(ressurs, env);
    }

}
