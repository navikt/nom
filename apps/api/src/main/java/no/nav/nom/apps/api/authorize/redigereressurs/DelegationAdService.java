package no.nav.nom.apps.api.authorize.redigereressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.msgraph.EntraIdUser;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.ressurs.api.dto.MinimalRessurs;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@Service
@Slf4j
public class DelegationAdService {

    private final MicrosoftGraphClient microsoftGraphClient;

    private final String nomWriteDelegertAdGroup;
    private final String nomWriteLederAdGroup;
    private final RessursStateRepository ressursStateRepository;


    public DelegationAdService(
        MicrosoftGraphClient microsoftGraphClient,
        @Value("${app.clients.microsoft-graph.groups.nom-delegert}") String nomWriteDelegertAdGroup,
        @Value("${app.clients.microsoft-graph.groups.nom-leder}") String nomWriteLederAdGroup, RessursStateRepository ressursStateRepository
    ) {
        this.microsoftGraphClient = microsoftGraphClient;
        this.nomWriteDelegertAdGroup = nomWriteDelegertAdGroup;
        this.nomWriteLederAdGroup = nomWriteLederAdGroup;
        this.ressursStateRepository = ressursStateRepository;
    }

    public CompletableFuture<List<EntraIdUser>> getRessurserWhichCanBeDelegatedToAsync(String adGroup) {
        return CompletableFuture.supplyAsync(
                () -> microsoftGraphClient.getUsersForGroup(adGroup))
            .exceptionally(ex -> {
                log.warn("Henting av delegerbare ansatte fra gruppen '{}' feilet: {}", adGroup, ex.getMessage(), ex);
                return Collections.emptyList();
            });
    }

    public List<MinimalRessurs> getRessurserWhichCanBeDelegatedTo() {
        CompletableFuture<List<EntraIdUser>> ansatteCompletable = getRessurserWhichCanBeDelegatedToAsync(nomWriteDelegertAdGroup);
        CompletableFuture<List<EntraIdUser>> ledereCompletable = getRessurserWhichCanBeDelegatedToAsync(nomWriteLederAdGroup);

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(ansatteCompletable, ledereCompletable);
        combinedFuture.join();

        try {
            return Stream.concat(ansatteCompletable.get().stream(), ledereCompletable.get().stream())
                .map(entraIdUser -> {
                    String visningsnavn = ressursStateRepository
                        .findByNavident(entraIdUser.navIdent())
                        .map(RessursState::visningsnavn)
                        .map(Visningsnavn::getFormattedVisningsnavn).orElse("Navn mangler");

                    return new MinimalRessurs(entraIdUser.navIdent(), visningsnavn);
                })
                .distinct()
                .sorted(Comparator.comparing(MinimalRessurs::visningsnavn))
                .toList();
        } catch (InterruptedException | ExecutionException ex) {
            log.error("Feil ved henting av Future med delegerbare ansatte: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
}
