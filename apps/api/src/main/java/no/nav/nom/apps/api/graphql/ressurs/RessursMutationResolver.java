package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.pdl.Kjonn;
import no.nav.nom.apps.api.pdl.PdlConsumer;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.EventBasedRessurs;
import no.nav.nom.apps.api.rest.person.PdlPersonDTO;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursRequest;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.Sektor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RessursMutationResolver implements GraphQLMutationResolver {

    private final IRessursService ressursService;
    private final NIKConsumer nikConsumer;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;
    private final PdlConsumer pdlConsumer;

    @Value("${app.graphql.preauth.mutation.ressurs}")
    private List<String> preauth;

    public DataFetcherResult<OpprettRessursResponse> opprettRessurs(OpprettRessursRequest request, DataFetchingEnvironment env) {
        return securityUtil.<OpprettRessursResponse>checkAuth(env, preauth).orElseGet(() -> {
            val response = DataFetcherResult.<OpprettRessursResponse>newResult();
            val ressurs = ressursService.findByPersonIdent(request.getPersonident()).orElseGet(() -> {
                try {
                    val nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getVisningsetternavn().substring(0, 1),  "STANDARD", true);
                    val person = pdlConsumer.hentPerson(request.getPersonident()).orElse(null);
                    if (List.of(HttpStatus.OK, HttpStatus.CREATED).contains(nikResponse.getStatusCode())) {
                        val data = objectMapper.createObjectNode()
                                .put(EventBasedRessurs.PERSONIDENT_FIELD, request.getPersonident())
                                .put(EventBasedRessurs.NAVIDENT_FIELD, nikResponse.getBody().getNavident())
                                .put(EventBasedRessurs.VISNINGSFORNAVN_FIELD, request.getVisningsfornavn())
                                .put(EventBasedRessurs.VISNINGSMELLOMNAVN_FIELD, request.getVisningsmellomnavn())
                                .put(EventBasedRessurs.VISNINGSETTERNAVN_FIELD, request.getVisningsetternavn())
                                .put(EventBasedRessurs.SEKTOR_FIELD, request.getSektor().name())
                                .put(EventBasedRessurs.KJONN_FIELD, person == null ? Kjonn.UKJENT.name() : person.getKjonn().name());
                        val metadata = securityUtil.metadataFromEnv(env, data);
                        val newRessurs = ressursService.opprettRessurs0(request.getPersonident(), data, metadata);
                        if(nikResponse.getBody().getFeilmelding() != null) {
                            response.error(resolveError(nikResponse.getStatusCode(), nikResponse.getBody().getFeilmelding(), env));
                        }

                        return newRessurs;
                    }
                    else {
                        response.error(resolveError(nikResponse.getStatusCode(),
                                nikResponse.getBody() != null && nikResponse.getBody().getFeilmelding() != null ? nikResponse.getBody().getFeilmelding() : "NIK connection error",
                                env));
                    }
                } catch (IllegalArgumentException | NoSuchElementException e) {
                    log.warn(e.getMessage(), e);
                    response.error(ErrorCode.BAD_REQUEST.construct(env, e.getMessage()));
                } catch (Exception e) {
                    log.error("Intern feil ved opprettelse av ny ressurs", e);
                    response.error(ErrorCode.SERVER_ERROR.construct(env, "Intern feil"));
                }


                return null;
            });
            return response.data(toResponse((EventBasedRessurs) ressurs)).build();
        });
    }

    private OpprettRessursResponse toResponse(EventBasedRessurs ressurs) {
        return ressurs != null ?
                OpprettRessursResponse.builder()
                .personident(ressurs.getPersonIdent())
                .navident(ressurs.getNavIdent())
                .visningsfornavn(ressurs.getVisningsmellomnavn().map(m -> ressurs.getVisningsfornavn().orElseThrow()+" "+m).orElse(ressurs.getVisningsfornavn().orElseThrow()))
                .visningsetternavn(ressurs.getVisningsetternavn().orElseThrow())
                .sektor(Sektor.valueOf(ressurs.getSektor()))
                .build(): null;
    }

    private GraphQLError resolveError(HttpStatus httpStatus, String errorMsg, DataFetchingEnvironment env) {
        return switch (httpStatus) {
                    case INTERNAL_SERVER_ERROR -> ErrorCode.SERVER_ERROR.construct(env, errorMsg);
                    case NOT_FOUND -> ErrorCode.NOT_FOUND.construct(env, errorMsg);
                    default -> ErrorCode.SERVER_ERROR.construct(env, errorMsg != null ? errorMsg : "Ukjent feil");
                };
    }
}
