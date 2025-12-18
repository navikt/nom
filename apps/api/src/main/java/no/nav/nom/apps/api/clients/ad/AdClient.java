package no.nav.nom.apps.api.clients.ad;

import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.libs.gql.ErrorCode;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.ad.AdRessurser;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AdClient {

    public final static int AD_MAX_BATCH_SIZE = 100;

    private final NomProxyClient nomProxyClient;

    public AdClient(NomProxyClient nomProxyClient) {
        this.nomProxyClient = nomProxyClient;
    }

    public AdRessurser getAdRessurser(Set<String> navIdenter) {

        return nomProxyClient.getWebClient().post()
            .uri("/proxy/ad")
            .bodyValue(navIdenter)
            .retrieve()
            .bodyToMono(AdRessurser.class)
            .doOnError(WebClientResponseException.class, (webClientResponseException) -> {
                throw new ADResponseException(
                    "Error in proxy query to AD code=" + webClientResponseException.getStatusCode(),
                    ErrorCode.SERVER_ERROR
                );
            })
            .block();
    }



    public <T> Map<String, AdRessurs> fetchInAdRessursInBatches(List<String> navidentList){
        var out = new HashMap<String,AdRessurs>(navidentList.size());
        for (var navidentBatch : listToBatches(navidentList, AD_MAX_BATCH_SIZE)){
            var tmp = this.getAdRessurser(new HashSet<>(navidentBatch));
            for(var y : tmp.getRessurser()){
                out.put(y.getNavIdent(), y);
            }
        }
        return out;
    }


    public static <T> Collection<Collection<T>> listToBatches(List<T> originalList, int batchSize) {
        Collection<Collection<T>> batchList = new LinkedList<>();
        int times = (originalList.size() / batchSize) + 1;

        for (int i = 0; i < times; i++) {
            int batchStart = batchSize * i;
            int possibleBatchEnd = batchSize * (i+1);
            int batchEnd = possibleBatchEnd > originalList.size() ? (batchSize * i) + originalList.size() % batchSize : possibleBatchEnd;
            batchList.add(originalList.subList(batchStart, batchEnd));
        }
        return batchList;
    }
}
