package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.contract.nom.api.graphql.OrganisasjonsenhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class LederResolver implements GraphQLResolver<Organisasjonsenhet> {
    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<OrganisasjonsenhetsLeder>> leder(Organisasjonsenhet orgEnhet, DataFetchingEnvironment env) {
        val lederRessurser = dvhRepository.getLederByAgressoId(orgEnhet.getAgressoId()).stream()
                .map(l -> OrganisasjonsenhetsLeder.builder()
                        .ressurs(
                            dvhRepository.getRessursByRessursId(l.getRessursId())
                            .map(ConverterUtils::convert)
                            .orElseThrow()
                        )
                        .build()
                )
                .collect(Collectors.toUnmodifiableList());

        return DataFetcherResult.<List<OrganisasjonsenhetsLeder>>newResult()
                .data(lederRessurser)
                .build();
    }
}
