package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrgEnhetKoblingResolver implements GraphQLResolver<OrgEnhet> {

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;

    public DataFetcherResult<List<OrgEnhetsKobling>> koblinger(OrgEnhet orgEnhet, DataFetchingEnvironment env) {
        final List<OrgEnhetsKobling> result = dvhRepository.getKoblingerByAgressoId(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv()).stream()
                .map(dvh -> {
                    val ressurs = ressursService.findByNavIdent(dvh.getNavIdent()).get();
                    return OrgEnhetsKobling.builder()
                            .ressurs(ressursService.convertToGraphQlResponse(ressurs))
                            .gyldigFom(dvh.getGyldigFom())
                            .gyldigTom(dvh.getGyldigTom().orElse(null))
                            .build();
                })
                .collect(Collectors.toUnmodifiableList());

        return DataFetcherResult.<List<OrgEnhetsKobling>>newResult()
                .data(result)
                .build();
    }
}
