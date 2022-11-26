package no.nav.nom.apps.api.graphql.ressurs;

import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateService;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
public class RessursPropertyResolver {

    private final ActiveDirectoryRessursBatchLoader adRessursBatchLoader;

    private final RessursStateService ressursStateService;
    private final boolean sendDummydata;

    public RessursPropertyResolver(
            ActiveDirectoryRessursBatchLoader activeDirectoryRessursBatchLoader,
            Environment env,
            RessursStateService ressursStateService
    ) {
        this.adRessursBatchLoader = activeDirectoryRessursBatchLoader;
        this.ressursStateService = ressursStateService;
        var naisClientId = env.getProperty("NAIS_CLUSTER_NAME");
        // Sliter med at AD fungerer dårlig i dev-miljø.
        sendDummydata = naisClientId != null && naisClientId.startsWith("dev");
    }

    private Map<Ressurs, DataFetcherResult<String>> createDummyResponse(String fieldSpec, List<Ressurs> source) {
        return source.stream()
                .collect(toMap(Function.identity(), ressurs -> respondWithDummydata(fieldSpec + ressurs.getNavIdent())));
    }

    private DataFetcherResult<String> respondWithDummydata(String data) {
        return DataFetcherResult.<String>newResult()
                .data(data)
                .build();
    }

    public Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser) {
        if (sendDummydata) {
            return createDummyResponse("dummy-epost@i-dev.", ressurser);
        }
        return adRessursBatchLoader.load(ressurser, AdRessurs::getEmail);
    }

    public Map<Ressurs, DataFetcherResult<String>> visningsnavn(List<Ressurs> ressurser) {
        return adRessursBatchLoaderWithFallback(
                ressurser,
                AdRessurs::getDisplayName,
                r -> r.visningsnavn().toString()
        );
    }

    public Map<Ressurs, DataFetcherResult<String>> fornavn(List<Ressurs> ressurser) {
        if (sendDummydata) {
            return createDummyResponse("dummy-fornavn-i-dev-", ressurser);
        }
        return adRessursBatchLoader.load(ressurser, AdRessurs::getGivenName);
    }

    public Map<Ressurs, DataFetcherResult<String>> etternavn(List<Ressurs> ressurser) {
        if (sendDummydata) {
            return createDummyResponse("dummy-etternnavn-i-dev-", ressurser);
        }
        return adRessursBatchLoader.load(ressurser, AdRessurs::getSurname);
    }

    private Map<Ressurs, DataFetcherResult<String>> adRessursBatchLoaderWithFallback(
            List<Ressurs> ressurser,
            Function<AdRessurs, String> fieldGetter,
            Function<RessursState, String> fallbackFieldGetter
    ) {
        var out = adRessursBatchLoader.load(ressurser, fieldGetter);
        var gotAdError = false;
        for (var entry : out.entrySet()) {
            if (entry.getValue().hasErrors()) {
                gotAdError = true;
                var optionalRessursState = ressursStateService.findByNavident(entry.getKey().getNavIdent());
                if (optionalRessursState.isPresent()) {
                    var fieldByFallback = fallbackFieldGetter.apply(optionalRessursState.get());
                    var replacement = DataFetcherResult.<String>newResult()
                            .data(fieldByFallback)
                            .build();
                    out.put(entry.getKey(), replacement);
                }
            }
        }
        if(gotAdError){
            log.warn("Fallback to DVH data due to AD error.");
        }
        return out;
    }
}
