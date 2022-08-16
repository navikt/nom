package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.kobling.KoblingDvhService;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.MappedBatchLoaderWithContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrgTilknytningBatchLoader implements MappedBatchLoaderWithContext<String, DataFetcherResult<List<RessursOrgTilknytning>>> {

    public static final String ORGTILKNYTNING_DATALOADER = "OrgTilknytningDataLoader";

    private final KoblingDvhService koblingDvhService;
    private final IOrgenhetService orgenhetService;

    @Override
    public CompletionStage<Map<String, DataFetcherResult<List<RessursOrgTilknytning>>>> load(Set<String> navidenter, BatchLoaderEnvironment batchLoaderEnvironment) {
        return CompletableFuture.supplyAsync(() -> getOrgTilknytningBatch(navidenter, batchLoaderEnvironment));
    }

    private Map<String, DataFetcherResult<List<RessursOrgTilknytning>>> getOrgTilknytningBatch(Set<String> navidenter, BatchLoaderEnvironment batchLoaderEnvironment) {
        val koblinger = koblingDvhService.getKoblingerByNavidenter(navidenter);

        return navidenter.stream().collect(Collectors.toMap(
                Function.identity(),
                navident -> DataFetcherResult.<List<RessursOrgTilknytning>>newResult().data(
                                koblinger.getOrDefault(navident, Collections.emptyList()).stream()
                                        .map(this::fromDvhKobling)
                                        .collect(Collectors.toList()))
                        .build()));

    }

    private RessursOrgTilknytning fromDvhKobling(Kobling kobling) {
        val orgenhet = orgenhetService.getEnhetByAgressoIdNivaAndName(kobling.getAgressoId(), kobling.getOrgNiv(), null).get();
        val orgEnhetGqlResponse = orgenhetService.convertToGraphQlResponse(orgenhet);

        return RessursOrgTilknytning.builder()
                .organisasjonsenhet(orgEnhetGqlResponse)
                .orgEnhet(orgEnhetGqlResponse)
                .gyldigFom(kobling.getGyldigFom())
                .gyldigTom(kobling.getGyldigTom().orElse(null))
                .build();
    }
}
