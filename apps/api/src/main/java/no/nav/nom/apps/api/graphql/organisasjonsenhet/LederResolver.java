package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.ErrorCode;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.contract.nom.api.graphql.OrganisasjonsenhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class LederResolver implements GraphQLResolver<Organisasjonsenhet> {
    private final DvhRepository dvhRepository;

    public DataFetcherResult<OrganisasjonsenhetsLeder> leder(Organisasjonsenhet orgEnhet, DataFetchingEnvironment env) {
        val result = DataFetcherResult.<OrganisasjonsenhetsLeder>newResult();

        dvhRepository.findLederByEnhetNr(orgEnhet.getId())
                .ifPresentOrElse(
                        (l) -> {
                            result.data(
                                    dvhRepository.findRessursByRessursId(l.getRessursId())
                                            .map(r -> OrganisasjonsenhetsLeder.builder().ressurs(ConverterUtils.convert(r)).build())
                                            .orElseThrow()
                            );
                        },
                        () -> {
                            result.error(ErrorCode.NOT_FOUND.construct(env, "Fant ikke leder for enhet: "+orgEnhet.getId()));
                        }
                );

        return result.build();
    }
}
