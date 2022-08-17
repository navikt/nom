package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.graphql.PdlConsumer;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.EventBasedRessurs;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.RessursHendelseRepository;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursRequest2;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse2;
import no.nav.nom.contract.nom.api.graphql.Sektor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RessursMutationResolver implements GraphQLMutationResolver {

    private final IRessursService ressursService;
    private final PdlConsumer pdlConsumer;
    private final NIKConsumer nikConsumer;
    private final SecurityUtil securityUtil;
    private final RessursHendelseRepository ressursHendelseRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.graphql.preauth.mutation.ressurs}")
    private List<String> preauth;

    public DataFetcherResult<OpprettRessursResponse0> opprettRessurs0(OpprettRessursRequest0 request, DataFetchingEnvironment env) {
        return securityUtil.<OpprettRessursResponse0>checkAuth(env, preauth).orElseGet(() -> {
            val response = DataFetcherResult.<OpprettRessursResponse0>newResult();
            val ressurs = ressursService.findByPersonIdent(request.getPersonident()).orElseGet(() -> {
                try {
                    val token = securityUtil.getToken(env).get();
                    val nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getVisningsetternavn().substring(0, 1),  "STANDARD", token);

                    if (List.of(HttpStatus.OK, HttpStatus.CREATED).contains(nikResponse.getStatusCode())) {
                        val data = objectMapper.createObjectNode()
                                .put(EventBasedRessurs.PERSONIDENT_FIELD, request.getPersonident())
                                .put(EventBasedRessurs.NAVIDENT_FIELD, nikResponse.getBody().getNavident())
                                .put(EventBasedRessurs.VISNINGSFORNAVN_FIELD, request.getVisningsfornavn())
                                .put(EventBasedRessurs.VISNINGSETTERNAVN_FIELD, request.getVisningsetternavn())
                                .put(EventBasedRessurs.SEKTOR_FIELD, request.getSektor().name());
                        val metadata = securityUtil.metadataFromEnv(env, data);
                        val newRessurs = ressursService.opprettRessurs0(request.getPersonident(), data, metadata);
                        response.error(resolveError(nikResponse.getStatusCode(), nikResponse.getBody().getFeilmelding(),env));

                        return newRessurs;
                    }

                    response.error(resolveError(nikResponse.getStatusCode(),nikResponse.getBody().getFeilmelding(), env));
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

    public DataFetcherResult<OpprettRessursResponse> opprettRessurs(String personIdent, Optional<String> forbokstav, DataFetchingEnvironment env) {
        return securityUtil.<OpprettRessursResponse>checkAuth(env, preauth).orElseGet(() -> {
            val result = DataFetcherResult.<OpprettRessursResponse>newResult();
            try {
                val rawData = objectMapper.createObjectNode()
                        .put("personident",personIdent)
                        .put("forbokstav",forbokstav.orElse(null));
                val metadata = securityUtil.metadataFromEnv(env,rawData);

                val person = pdlConsumer.hentPerson(personIdent);

                val ressurs = ressursService.findByPersonIdent(personIdent).orElseGet(() -> {

                            val navIdentForbokstav = (forbokstav.isPresent() ? forbokstav.filter(c -> c.length() == 1).orElseThrow(() -> new IllegalArgumentException("Ugyldig input i forbokstav: " + forbokstav.get())) :
                                    person.orElseThrow(() -> new NoSuchElementException("Ingen resultat fra PDL med gitt personident")).getNavn().getEtternavn().substring(0, 1)).toUpperCase();

                            return ressursService.opprettRessurs(personIdent, null, navIdentForbokstav, metadata);
                        });

                if(person.isEmpty()) {
                    result.error(ErrorCode.NOT_FOUND.construct(env, "Oppslag mot PDL feilet"));
                }
                result.data(convert(ressurs, person.orElse(null)));
            } catch (IllegalArgumentException|NoSuchElementException e){
                log.warn(e.getMessage(), e);
                result.error(ErrorCode.BAD_REQUEST.construct(env, e.getMessage()));
            } catch (Exception e){
                log.error("Intern feil ved opprettelse av ny ressurs", e);
                result.error(ErrorCode.SERVER_ERROR.construct(env, "Intern feil ved opprettelse av ny ressurs"));
            }
            return result.build();
        });
    }

    @Transactional
    public DataFetcherResult<OpprettRessursResponse2> opprettRessurs2(OpprettRessursRequest2 request, DataFetchingEnvironment env) {
        return securityUtil.<OpprettRessursResponse2>checkAuth(env, preauth).orElseGet(() -> {
            val result = DataFetcherResult.<OpprettRessursResponse2>newResult();
            val person = pdlConsumer.hentPerson(request.getPersonident());
            ressursHendelseRepository.deleteHendelseByNavident("Z999999");
            val ressurs = ressursService.findByPersonIdent(request.getPersonident());
            if (person.isEmpty()) {
                result.error(ErrorCode.NOT_FOUND.construct(env, "Oppslag mot PDL feilet"));
            }else{
                if(ressurs.isEmpty()){
                    val rawData = objectMapper.convertValue(request, JsonNode.class);
                    val metadata = securityUtil.metadataFromEnv(env,rawData);
                    ressursService.opprettRessurs(request.getPersonident(),"Z999999",null, metadata);
                    result.data(OpprettRessursResponse2.builder()
                                    .person(person.get())
                                    .navident("Z999999")
                                    .personident(request.getPersonident())
                                    .fornavn(request.getFornavn())
                                    .etternavn(request.getEtternavn())
                            .build());
                    return result.build();
                }
                val existingRessurs = ressurs.get();
                result.data(OpprettRessursResponse2.builder()
                                .navident(existingRessurs.getNavIdent())
                                .fornavn("Vidar")
                                .etternavn("Visningsen")
                                .personident(existingRessurs.getPersonIdent())
                        .build());
                result.build();
                return result.build();
            }
            return result.error(ErrorCode.SERVER_ERROR.construct(env,"Server error")).build();
        });
    }

    private OpprettRessursResponse convert(RessursI ressurs, FolkeregisterPerson person){
        return OpprettRessursResponse.builder()
                .nomId(ressurs.getId())
                .navIdent(ressurs.getNavIdent())
                .personIdent(ressurs.getPersonIdent())
                .person(person)
                .build();
    }

    private OpprettRessursResponse0 toResponse(EventBasedRessurs ressurs) {
        return ressurs != null ?
                OpprettRessursResponse0.builder()
                .personident(ressurs.getPersonIdent())
                .navident(ressurs.getNavIdent())
                .visningsfornavn(ressurs.getVisningsfornavn())
                .visningsetternavn(ressurs.getVisningsetternavn())
                .sektor(Sektor.valueOf(ressurs.getSektor()))
                .build(): null;
    }

    private GraphQLError resolveError(HttpStatus httpStatus, String errorMsg, DataFetchingEnvironment env) {
        return errorMsg != null ?
                switch (httpStatus) {
                    case INTERNAL_SERVER_ERROR -> ErrorCode.SERVER_ERROR.construct(env, errorMsg);
                    case NOT_FOUND -> ErrorCode.NOT_FOUND.construct(env, errorMsg);
                    default -> ErrorCode.SERVER_ERROR.construct(env, "Ukjent feil");
                } : null;
    }
}
