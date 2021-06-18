package no.nav.nom.apps.api.graphql.request;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.request.dto.BestillingRequestMetadata;
import no.nav.nom.apps.api.graphql.request.dto.BestillingRequestMetadata.Aktivering;
import no.nav.nom.apps.api.graphql.request.dto.EnhetBestillingResponse;
import no.nav.nom.apps.api.graphql.request.dto.OpprettEnhetRequest;
import no.nav.nom.apps.api.graphql.request.dto.OpprettRessursRequest;
import no.nav.nom.apps.api.graphql.request.dto.RessursBestillingResponse;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BestillingMutationResolver implements GraphQLMutationResolver {

    private final RequestSetInMemoryRepository repository;

    public DataFetcherResult<RessursBestillingResponse> opprettRessurs(OpprettRessursRequest request, DataFetchingEnvironment env) {
        return SecurityUtil.<RessursBestillingResponse>checkAuth(env).orElseGet(() -> {
            Request r = new Request(UUID.randomUUID(), request);
            RequestSet requestSet = getRequestSet(request.getMetadata());
            requestSet.getRequests().add(r);

            return build(new RessursBestillingResponse(r.getRequestId(), requestSet.getRequestSetId(),
                    request.getMetadata().isImmediate() ? UUID.randomUUID() : null)
            );
        });
    }

    public DataFetcherResult<EnhetBestillingResponse> opprettEnhet(OpprettEnhetRequest request, DataFetchingEnvironment env) {
        return SecurityUtil.<EnhetBestillingResponse>checkAuth(env).orElseGet(() -> {
            Request r = new Request(UUID.randomUUID(), request);
            RequestSet requestSet = getRequestSet(request.getMetadata());
            requestSet.getRequests().add(r);

            return build(new EnhetBestillingResponse(r.getRequestId(), requestSet.getRequestSetId(),
                    request.getMetadata().isImmediate() ? UUID.randomUUID() : null
            ));
        });
    }

    public DataFetcherResult<ResultCode> activateRequestSet(UUID requestSetId, LocalDateTime when, DataFetchingEnvironment env) {
        return SecurityUtil.<ResultCode>checkAuth(env).orElseGet(() -> {
            var requestSet = repository.get(requestSetId);
            requestSet.setActivateTime(Optional.ofNullable(when).orElse(LocalDateTime.now()));

            return build(ResultCode.OK);
        });
    }

    @Scheduled(initialDelayString = "PT3M", fixedRateString = "PT1M")
    public void activateReadyRequestSets() {
        repository.getReady().forEach(rs -> log.info("RUNNING {}", rs));
    }

    private RequestSet getRequestSet(BestillingRequestMetadata metadata) {
        RequestSet requestSet = null;
        if (metadata.getRequestSetId() != null) {
            requestSet = repository.get(metadata.getRequestSetId());
        }
        if (requestSet == null) {
            var aktiveringTid = metadata.getAktivering() == Aktivering.IMMEDIATE ? LocalDateTime.now() : null;
            requestSet = new RequestSet(UUID.randomUUID(), new ArrayList<>(), "", LocalDateTime.now(), aktiveringTid);
            repository.add(requestSet);
        }
        return requestSet;
    }

    private <T> DataFetcherResult<T> build(T data) {
        return DataFetcherResult.<T>newResult()
                .data(data)
                .build();
    }

}
