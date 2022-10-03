package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.projections.RessursSearch;
import no.nav.nom.apps.api.graphql.common.projections.RessurserSearch;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.pdl.Kjonn;
import no.nav.nom.apps.api.pdl.PdlConsumer;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.EventBasedRessurs;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursRequest;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.Sektor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.Map.entry;

@Component
public class RessursResolver {

    private final static Logger log = LoggerFactory.getLogger(RessursResolver.class);
    private final IRessursService ressursService;
    private final NIKConsumer nikConsumer;
    //    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;
    private final PdlConsumer pdlConsumer;

    @Value("${app.graphql.preauth.query.person:''}")
    private List<String> personPreAuth;

    @Value("${app.graphql.preauth.mutation.ressurs:''}")
    private List<String> mutationPreauth;

    @Autowired
    RessursResolver(IRessursService ressursService, NIKConsumer nikConsumer, ObjectMapper objectMapper, PdlConsumer pdlConsumer) {
        this.ressursService = ressursService;
        this.nikConsumer = nikConsumer;
        this.objectMapper = objectMapper;
        this.pdlConsumer = pdlConsumer;
    }

    public Optional<Ressurs> ressurs(RessursSearch search) {
        Optional<IRessurs> ident = Optional.empty();
        if (search.isNavidentPresent()) {
            ident = search.resolveIdent().flatMap(ressursService::findByNavIdent);
        } else if (search.isPersonidentPresent()) {
            ident = search.resolveIdent().flatMap(ressursService::findByPersonIdent);
        }

        return ident.map(ressursService::convertToGraphQlResponse);
    }

    public List<RessursResult> ressurser(RessurserSearch search) {
        Stream<Result> results;
        if (search.resolveNavidenter().isEmpty() && search.resolvePersonidenter().isEmpty()) {
            results = ressursService.getAll().stream().map(Result::from);
        } else {
            results = Stream.concat(
                    search.resolveNavidenter().stream().map(ident -> new Result(ident, ressursService.findByNavIdent(ident).orElse(null))),
                    search.resolvePersonidenter().stream().map(ident -> new Result(ident, ressursService.findByPersonIdent(ident).orElse(null)))
            );
        }
        return results
                .map(res -> new RessursResult(res.isEmpty() ? null : ressursService.convertToGraphQlResponse(res.ressurs), res.id))
                .toList();
    }


    public DataFetcherResult<OpprettRessursResponse> opprettRessurs(OpprettRessursRequest request, DataFetchingEnvironment env, JwtAuthenticationToken token) {
        // TODO: Check secured/preauth against mutation-required role
        final var response = DataFetcherResult.<OpprettRessursResponse>newResult();

        var ressurs = ressursService.findByPersonIdent(request.getPersonident());
        if (ressurs.isPresent()) {
            return response.data(toResponse(ressurs)).build();
        }

        try {
            final var createRessursResponse = createRessurs(request, token);

            if(!createRessursResponse.hasErrors()) {
                return response.data(toResponse(createRessursResponse.getData())).build();
            }

            response.errors(createRessursResponse.getErrors());

        } catch (IllegalArgumentException | NoSuchElementException e) {
            log.warn(e.getMessage(), e);
            response.error(ErrorCode.BAD_REQUEST.construct(env, e.getMessage()));
        } catch (Exception e) {
            log.error("Intern feil ved opprettelse av ny ressurs", e);
            response.error(ErrorCode.SERVER_ERROR.construct(env, "Intern feil"));
        }
        return response.build();
    }

    private DataFetcherResult<IRessurs> createRessurs(OpprettRessursRequest request, JwtAuthenticationToken token) {
        final var result = DataFetcherResult.<IRessurs>newResult();
        final var nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getVisningsetternavn().substring(0, 1), "STANDARD", true);
//
//        if (!nikResponse.getStatusCode().is2xxSuccessful()) {
//            return result.error(
//                    resolveError(
//                            nikResponse.getStatusCode(),
//                            nikResponse.getBody() != null && nikResponse.getBody().getFeilmelding() != null ? nikResponse.getBody().getFeilmelding() : "NIK connection error",
//                            env
//                    )
//            ).build();
//        }
//
//        if (nikResponse.getBody() != null && nikResponse.getBody().getFeilmelding() != null) {
//            return result.error(
//                    resolveError(
//                            nikResponse.getStatusCode(),
//                            nikResponse.getBody().getFeilmelding(),
//                            env
//                    )
//            ).build();
//        }

        final var person = pdlConsumer.hentPerson(request.getPersonident());

        Map<String, Object> data = Map.ofEntries(
                entry(EventBasedRessurs.PERSONIDENT_FIELD, request.getPersonident()),
                entry(EventBasedRessurs.NAVIDENT_FIELD, nikResponse.getBody().getNavident()),
                entry(EventBasedRessurs.VISNINGSFORNAVN_FIELD, request.getVisningsfornavn()),
                entry(EventBasedRessurs.VISNINGSMELLOMNAVN_FIELD, request.getVisningsmellomnavn()),
                entry(EventBasedRessurs.VISNINGSETTERNAVN_FIELD, request.getVisningsetternavn()),
                entry(EventBasedRessurs.SEKTOR_FIELD, request.getSektor().name()),
                entry(EventBasedRessurs.KJONN_FIELD, person.map(pers -> pers.getKjonn().name()).orElseGet(Kjonn.UKJENT::name))
        );

        final var ressurs = ressursService.opprettRessurs(request.getPersonident(), data, metadataFromToken(token, data));

        return result.data(ressurs).build();
    }

    private OpprettRessursResponse toResponse(IRessurs ressurs) {
        return toResponse(Optional.ofNullable(ressurs));
    }
    private OpprettRessursResponse toResponse(Optional<IRessurs> ressurs) {
        return ressurs.map(res -> OpprettRessursResponse.builder()
                        .personident(res.getPersonIdent())
                        .navident(res.getNavIdent())
                        .visningsfornavn(
                                res.getVisningsmellomnavn().map(m -> res.getVisningsfornavn().orElseThrow() + " " + m).orElse(res.getVisningsfornavn().orElseThrow())
                        )
                        .visningsetternavn(
                                res.getVisningsetternavn().orElseThrow()
                        )
                        .sektor(Sektor.valueOf(res.getSektor()))
                        .build())
                .orElse(null);
    }

    private GraphQLError resolveError(HttpStatus httpStatus, String errorMsg, DataFetchingEnvironment env) {
        return switch (httpStatus) {
            case INTERNAL_SERVER_ERROR -> ErrorCode.SERVER_ERROR.construct(env, errorMsg);
            case NOT_FOUND -> ErrorCode.NOT_FOUND.construct(env, errorMsg);
            default -> ErrorCode.SERVER_ERROR.construct(env, errorMsg != null ? errorMsg : "Ukjent feil");
        };
    }

//    private <T> Optional<DataFetcherResult<T>> checkAuthRessurs(DataFetchingEnvironment env){
//        //TODO: Can checkAuthRessurs be replaced by spring security annotations?
//        var personIdentRequested = env.getSelectionSet().contains("**/personIdent") || env.getSelectionSet().contains("**/personident");
//        return securityUtil.<T>checkAuth(env, personIdentRequested ? this.personPreAuth : null);
//    }

    public HendelseMetadataSerialize metadataFromToken(JwtAuthenticationToken token, Map<String, Object> rawData) {
        return HendelseMetadataSerialize.builder()
                .opprettetAv(new HendelseMetadata.OpprettetAv(token))
                .opprettetTidspunkt(LocalDateTime.now())
                .rawInputData(objectMapper.valueToTree(rawData))
                .build();
    }

    private record Result(
            String id,
            IRessurs ressurs
    ) {
        public boolean isEmpty() {
            return ressurs == null;
        }

        public static Result from(IRessurs ressurs) {
            return from(ressurs.getNavIdent(), ressurs);
        }

        public static Result from(String id, IRessurs ressurs) {
            return new Result(id, ressurs);
        }
    }

}
