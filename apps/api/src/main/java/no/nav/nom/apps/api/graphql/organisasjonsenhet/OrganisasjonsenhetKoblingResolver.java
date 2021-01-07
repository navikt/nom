package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.contract.nom.api.graphql.OrganisasjonsenhetsKobling;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrganisasjonsenhetKoblingResolver implements GraphQLResolver<Organisasjonsenhet> {
    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<OrganisasjonsenhetsKobling>> koblinger(Organisasjonsenhet orgEnhet, DataFetchingEnvironment env) {
        final List<OrganisasjonsenhetsKobling> result = dvhRepository.getKoblingerForEnhet(orgEnhet.getId()).stream()
                .map(dvh -> {
                    val ressurs  = dvhRepository.findRessursByRessursId(dvh.getRessursId()).get();
                    return OrganisasjonsenhetsKobling.builder()
                                    .ressurs(ConverterUtils.convert(ressurs))
                                    .build();
                })
                .collect(Collectors.toUnmodifiableList());

        return DataFetcherResult.<List<OrganisasjonsenhetsKobling>>newResult()
                .data(result)
                .build();
    }
}
