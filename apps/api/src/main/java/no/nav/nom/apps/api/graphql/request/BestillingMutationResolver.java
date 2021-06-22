package no.nav.nom.apps.api.graphql.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.request.dto.Bestilling;
import no.nav.nom.apps.api.graphql.request.dto.Bestilling.BestillingType;
import no.nav.nom.apps.api.graphql.request.dto.BestillingMetadata;
import no.nav.nom.apps.api.graphql.request.dto.BestillingMetadata.Aktivering;
import no.nav.nom.apps.api.graphql.request.dto.EnhetBestillingResponse;
import no.nav.nom.apps.api.graphql.request.dto.OpprettEnhetRequest;
import no.nav.nom.apps.api.graphql.request.dto.OpprettRessursRequest;
import no.nav.nom.apps.api.graphql.request.dto.RessursBestillingResponse;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class BestillingMutationResolver implements GraphQLMutationResolver {

    private final BestillingSetInMemoryRepository repository;
    private final ObjectMapper om;
    private final AtomicInteger idCounter = new AtomicInteger(500000);

    public DataFetcherResult<RessursBestillingResponse> opprettRessurs(OpprettRessursRequest request, DataFetchingEnvironment env) {
        return SecurityUtil.<RessursBestillingResponse>checkAuth(env).orElseGet(() -> {
            BestillingSet bestillingSet = getBestillingSet(request.getMetadata());
            Bestilling r = Bestilling.builder()
                    .type(BestillingType.OPPRETT_RESSURS)
                    .bestillingId(UUID.randomUUID())
                    .bestillingSetId(bestillingSet.getBestillingSetId())
                    .metadata(request.getMetadata().copy())
                    .data(toJson(Map.of("personIdent", request.getPersonIdent(), "visningsnavn", request.getVisningsnavn())))
                    .build();
            bestillingSet.getBestillinger().add(r);

            var nomId = request.getMetadata().isImmediate() ? UUID.randomUUID() : null;
            var navIdent = request.getMetadata().isImmediate() ? "X%d".formatted(idCounter.getAndIncrement()) : null;
            return build(new RessursBestillingResponse(nomId, navIdent, r));
        });
    }

    public DataFetcherResult<EnhetBestillingResponse> opprettEnhet(OpprettEnhetRequest request, DataFetchingEnvironment env) {
        return SecurityUtil.<EnhetBestillingResponse>checkAuth(env).orElseGet(() -> {
            BestillingSet bestillingSet = getBestillingSet(request.getMetadata());
            Bestilling r = Bestilling.builder()
                    .type(BestillingType.OPPRETT_ENHET)
                    .bestillingId(UUID.randomUUID())
                    .bestillingSetId(bestillingSet.getBestillingSetId())
                    .metadata(request.getMetadata().copy())
                    .data(toJson(Map.of("agressoId", request.getAgressoId(), "navn", request.getNavn())))
                    .build();
            bestillingSet.getBestillinger().add(r);

            var nomId = request.getMetadata().isImmediate() ? UUID.randomUUID() : null;
            return build(new EnhetBestillingResponse(nomId, r));
        });
    }

    @SneakyThrows
    private ObjectNode toJson(Map<?, ?> dataMap) {
        return om.valueToTree(dataMap);
    }

    public DataFetcherResult<ResultCode> activateBestillingSet(UUID bestillingSetId, LocalDateTime when, DataFetchingEnvironment env) {
        return SecurityUtil.<ResultCode>checkAuth(env).orElseGet(() -> {
            var bestillingSet = repository.get(bestillingSetId);
            bestillingSet.setActivateTime(Optional.ofNullable(when).orElse(LocalDateTime.now()));

            return build(ResultCode.OK);
        });
    }

    @Scheduled(initialDelayString = "PT3M", fixedRateString = "PT1M")
    public void activateReadyBestillingSets() {
        repository.getReady().forEach(rs -> log.info("RUNNING {}", rs));
    }

    private BestillingSet getBestillingSet(BestillingMetadata metadata) {
        BestillingSet bestillingSet = null;
        if (metadata.getBestillingSetId() != null) {
            bestillingSet = repository.get(metadata.getBestillingSetId());
        }
        if (bestillingSet == null) {
            var aktiveringTid = metadata.getAktivering() == Aktivering.IMMEDIATE ? LocalDateTime.now() : null;
            bestillingSet = new BestillingSet(UUID.randomUUID(), new ArrayList<>(), "", LocalDateTime.now(), aktiveringTid);
            repository.add(bestillingSet);
        }
        return bestillingSet;
    }

    private <T> DataFetcherResult<T> build(T data) {
        return DataFetcherResult.<T>newResult()
                .data(data)
                .build();
    }

}
