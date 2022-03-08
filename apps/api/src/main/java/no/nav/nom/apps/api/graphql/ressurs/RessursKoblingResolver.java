package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import no.nav.nom.apps.api.orgenhet.contract.OrgenhetService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursKobling;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RessursKoblingResolver implements GraphQLResolver<Ressurs> {

    private final OrgenhetService orgenhetService;
    private final KoblingService koblingService;

    public DataFetcherResult<List<RessursKobling>> koblinger(Ressurs ressurs, DataFetchingEnvironment env) {
        final List<RessursKobling> result = koblingService.getKoblingerByNavIdent(ressurs.getNavIdent())
                .stream()
                .map(dvh -> {
                    val orgenhet = orgenhetService.getEnhetByAgressoIdNivaAndName(dvh.getAgressoId(), dvh.getOrgNiv(), null).get();
                    return RessursKobling.builder()
                            .organisasjonsenhet(orgenhetService.convertToGraphQlResponse(orgenhet))
                            .gyldigFom(dvh.getGyldigFom())
                            .gyldigTom(dvh.getGyldigTom().orElse(null))
                            .build();
                }).toList();

        return DataFetcherResult.<List<RessursKobling>>newResult()
                .data(result)
                .build();
    }

}
