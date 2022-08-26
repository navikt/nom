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
import no.nav.nom.apps.api.graphql.request.dto.OrdreLinje;
import no.nav.nom.apps.api.graphql.request.dto.OrdreLinje.OrdreLinjeType;
import no.nav.nom.apps.api.graphql.request.dto.OrdreMetadata;
import no.nav.nom.apps.api.graphql.request.dto.OrdreLinjeResponse;
import no.nav.nom.apps.api.graphql.request.dto.OpprettEnhetRequest;
import no.nav.nom.apps.api.graphql.request.dto.OpprettRessursRequest;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrdreLinjeMutationResolver implements GraphQLMutationResolver {

    //private final OrdreRepository ordreRepository;
    private final SecurityUtil securityUtil;
    private final ObjectMapper om;
    private final AtomicInteger idCounter = new AtomicInteger(500000);

    public DataFetcherResult<OrdreLinjeResponse> ordreRessurs(/*OpprettRessursRequest request,*/ DataFetchingEnvironment env) {
        return securityUtil.<OrdreLinjeResponse>checkAuth(env, null).orElseGet(() -> {
            Ordre ordre = null;//getOrdre(request.getMetadata());
            OrdreLinje r = OrdreLinje.builder()
                    .type(OrdreLinjeType.OPPRETT_RESSURS)
                    .ordreLinjeId(UUID.randomUUID())
                    .ordreId(ordre.getOrdreId())
                    .metadata(
                            OrdreMetadata.builder()
                                    .ordreId(null/*request.getMetadata().getOrdreId()*/)
                                    .beskrivelse(null/*request.getMetadata().getBeskrivelse()*/)
                                    .build()
                    )
                    .data(toJson(Map.of("personIdent", /*request.getPersonIdent()*/null, "visningsnavn", /*request.getVisningsnavn()*/null)))
                    .build();
            ordre.getOrdreLinjer().add(r);

            return build(new OrdreLinjeResponse(r));
        });
    }

    public DataFetcherResult<OrdreLinjeResponse> opprettEnhet(OpprettEnhetRequest request, DataFetchingEnvironment env) {
        return securityUtil.<OrdreLinjeResponse>checkAuth(env, null).orElseGet(() -> {
            Ordre ordre = getOrdre(request.getMetadata());
            OrdreLinje r = OrdreLinje.builder()
                    .type(OrdreLinjeType.OPPRETT_ENHET)
                    .ordreLinjeId(UUID.randomUUID())
                    .ordreId(ordre.getOrdreId())
                    .metadata(
                            OrdreMetadata.builder()
                                    .ordreId(request.getMetadata().getOrdreId())
                                    .beskrivelse(request.getMetadata().getBeskrivelse())
                                    .build()
                    )
                    .data(toJson(Map.of("agressoId", request.getAgressoId(), "navn", request.getNavn())))
                    .build();
            ordre.getOrdreLinjer().add(r);

            return build(new OrdreLinjeResponse(r));
        });
    }

    @SneakyThrows
    private ObjectNode toJson(Map<?, ?> dataMap) {
        return om.valueToTree(dataMap);
    }

    public DataFetcherResult<ResultCode> godkjennOrdre(UUID ordreId, DataFetchingEnvironment env) {
        return securityUtil.<ResultCode>checkAuth(env, null).orElseGet(() -> {
            /*var ordre = ordreRepository.getById(ordreId);
            ordre.setGodkjentTidspunkt(LocalDateTime.now());
            ordre.setGodkjentAv(UUID.randomUUID());*/

            // TODO opprett hendelser for godkjent ordre
            return build(ResultCode.OK);
        });
    }

    private Ordre getOrdre(OrdreMetadata metadata) {
        /*if (metadata.getOrdreId() != null) {
            return ordreRepository.findById(metadata.getOrdreId()).map(
                    o -> Ordre.builder()
                            .ordreId(o.getOrdreId())
                            .ordreLinjer(new ArrayList<>())
                            .beskrivelse("")
                            .godkjentAv(null)
                            .godkjentTidspunkt(null)
                            .endretAv(UUID.randomUUID())
                            .endretTidspunkt(LocalDateTime.now())
                            .build()
            ).orElse(null);
        }*/

        return null;
    }

    private <T> DataFetcherResult<T> build(T data) {
        return DataFetcherResult.<T>newResult()
                .data(data)
                .build();
    }

}
