package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class OrganiseringToOrgEnhetResolver implements GraphQLResolver<Organisering> {
    private final DvhRepository dvhRepository;

    public DataFetcherResult<Organisasjonsenhet> organisasjonsenhet(Organisering organisering, DataFetchingEnvironment env) {
        val orgEnhet = dvhRepository.findEnhetByNummer(organisering.getOrgEnhetNr())
                .map(ConverterUtils::convert)
                .orElseThrow();

        return DataFetcherResult.<Organisasjonsenhet>newResult()
                .data(orgEnhet)
                .build();
    }
}
