package no.nav.nom.apps.api.graphql.resolvers;

import graphql.execution.DataFetcherResult;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.gql.RessursPropertyResolverInterface;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
public class RessursPropertyResolver implements RessursPropertyResolverInterface {

    private final ActiveDirectoryRessursBatchLoader adRessursBatchLoader;

    private final RessursStateRepository ressursStateRepository;
    private final boolean sendDummydata;

    public RessursPropertyResolver(
        ActiveDirectoryRessursBatchLoader activeDirectoryRessursBatchLoader, Environment env,
        RessursStateRepository ressursStateRepository
    ) {
        this.adRessursBatchLoader = activeDirectoryRessursBatchLoader;
        this.ressursStateRepository = ressursStateRepository;
        var naisClientId = env.getProperty("NAIS_CLUSTER_NAME");
        // Sliter med at AD fungerer dårlig i dev-miljø.
        sendDummydata = naisClientId != null && naisClientId.startsWith("dev");
    }

    private Map<Ressurs, DataFetcherResult<String>> createDummyResponse(String fieldSpec, List<Ressurs> source) {
        return source.stream().collect(toMap(Function.identity(), ressurs -> respondWithDummydata(fieldSpec + ressurs.getNavIdent())));
    }

    private DataFetcherResult<String> respondWithDummydata(String data) {
        return DataFetcherResult.<String>newResult().data(data).build();
    }

    @Override
    public Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser) {
        if (sendDummydata) {
            return createDummyResponse("dummy-epost@i-dev.", ressurser);
        }
        return adRessursBatchLoader.load(ressurser, AdRessurs::getEmail);
    }


}
