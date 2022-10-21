package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Component
public class RessursPropertyResolver {

    private final ActiveDirectoryRessursBatchLoader adRessursBatchLoader;
    private final boolean sendDummydata;

    public RessursPropertyResolver(
            ActiveDirectoryRessursBatchLoader activeDirectoryRessursBatchLoader,
            Environment env
    ) {
        this.adRessursBatchLoader = activeDirectoryRessursBatchLoader;
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
        if (sendDummydata) {
            return createDummyResponse("dummy-visningsnavn-i-dev-", ressurser);
        }
        return adRessursBatchLoader.load(ressurser, AdRessurs::getDisplayName);
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

}
