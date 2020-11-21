package no.nav.nom.apps.api.graphql.hentenhet;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.ErrorCode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class HentEnhetResolver implements GraphQLQueryResolver {
    private final DvhRepository dvhRepository;

    public DataFetcherResult<EnhetTo> hentEnhet(String nummer, DataFetchingEnvironment env) {
        return dvhRepository.findEnhetByNummer(nummer)
                .map(e -> new EnhetTo(e.getEnhetNavn(), e.getEnhetKode()))
                .map(e -> DataFetcherResult.<EnhetTo>newResult().data(e).build())
                .orElseGet(() -> DataFetcherResult.<EnhetTo>newResult()
                        .error(ErrorCode.NOT_FOUND.construct(env, "Fant ikke enhet med nummer: " + nummer))
                        .build());
    }

    @Value
    public static class EnhetTo {
        String navn;
        String nummer;
    }
}
