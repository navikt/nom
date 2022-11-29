package no.nav.nom.apps.api.graphql.ressurs;

import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.common.exception.NIKException;
import no.nav.nom.apps.api.graphql.common.exception.OpprettRessursException;
import no.nav.nom.apps.api.graphql.common.projections.RessursSearch;
import no.nav.nom.apps.api.graphql.common.projections.RessurserSearch;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.pdl.PdlConsumer;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursRequest;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class RessursResolver {

    private final static Logger log = LoggerFactory.getLogger(RessursResolver.class);
    private final NIKConsumer nikConsumer;
    private final PdlConsumer pdlConsumer;
    private final IRessursService ressursService;
    private final RessursCommadService ressursCommadService;

    RessursResolver(
            NIKConsumer nikConsumer,
            PdlConsumer pdlConsumer,
            IRessursService ressursService,
            RessursCommadService ressursCommadService
    ) {
        this.nikConsumer = nikConsumer;
        this.pdlConsumer = pdlConsumer;
        this.ressursService = ressursService;
        this.ressursCommadService = ressursCommadService;
    }

    public Optional<Ressurs> ressurs(RessursSearch search) {
        Optional<RessursState> ident = Optional.empty();
        if (search.isNavidentPresent()) {
            ident = search.resolveIdent().flatMap(ressursService::findByNavident);
        } else if (search.isPersonidentPresent()) {
            ident = search.resolveIdent().flatMap(ressursService::findByPersonident);
        }

        return ident.map(MappingUtils::toRessurs);
    }

    public List<RessursResult> ressurser(RessurserSearch search) {
        Stream<Result> results;
        if (search.resolveNavidenter().isEmpty() && search.resolvePersonidenter().isEmpty()) {
            results = ressursService.getAll().stream().map(Result::from);
        } else {
            results = Stream.concat(
                    search.resolveNavidenter().stream().map(ident -> new Result(ident, ressursService.findByNavident(ident).orElse(null))),
                    search.resolvePersonidenter().stream().map(ident -> new Result(ident, ressursService.findByPersonident(ident).orElse(null)))
            );
        }
        return results
                .map(res -> new RessursResult(res.isEmpty() ? null : MappingUtils.toRessurs(res.ressurs), res.id))
                .toList();
    }


    public DataFetcherResult<OpprettRessursResponse> opprettRessurs(OpprettRessursRequest request, DataFetchingEnvironment env, JwtAuthenticationToken token) {
        final var response = DataFetcherResult.<OpprettRessursResponse>newResult();

        var ressurs = ressursService.findByPersonident(request.getPersonident());
        if (ressurs.isPresent()) {
            return response.data(toResponse(ressurs)).build();
        }

        try {
            final var createRessursResponse = createRessurs(request, token);

            return response.data(toResponse(createRessursResponse)).build();

        } catch (IllegalArgumentException | NoSuchElementException exc) {
            log.warn(exc.getMessage(), exc);
            throw new OpprettRessursException(exc.getMessage(), exc, ErrorCode.BAD_REQUEST);
        } catch (Exception exc) {
            log.error("Intern feil ved opprettelse av ny ressurs", exc);
            throw new OpprettRessursException("Intern feil", exc, ErrorCode.SERVER_ERROR);
        }
    }

    private RessursState createRessurs(OpprettRessursRequest request, JwtAuthenticationToken token) {
        final var nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getVisningsetternavn().substring(0, 1), "STANDARD", true);

        if (!nikResponse.getStatusCode().is2xxSuccessful()) {
            throw new NIKException(
                    Optional.ofNullable(nikResponse.getBody())
                            .map(OpprettNavidentResponse::getFeilmelding)
                            .orElse("NIK connection error"),
                    nikResponse,
                    resolveError(nikResponse.getStatusCode())
            );
        }

        if (nikResponse.getBody() != null && nikResponse.getBody().getFeilmelding() != null) {
            throw new NIKException(
                    nikResponse.getBody().getFeilmelding(),
                    nikResponse,
                    resolveError(nikResponse.getStatusCode())
            );
        }

        final var pdlPerson = pdlConsumer.hentPerson(request.getPersonident());

        if (request.getVisningsmellomnavn() != null && request.getVisningsmellomnavn().isBlank()) {
            request.setVisningsmellomnavn(null);
        }

        log.debug("createRessurs for {} - {}", nikResponse.getBody().getNavident(), request.getVisningsetternavn());

        return createRessursEvent(request, nikResponse.getBody().getNavident(), pdlPerson, token);
    }


    private RessursState createRessursEvent(OpprettRessursRequest request, String navident, Optional<Person> pdlPerson, JwtAuthenticationToken token) {
        final var opprettRessursCommand = new OpprettRessursCommand(
                request.getPersonident(),
                navident,
                pdlPerson,
                request.getSektor(),
                new Visningsnavn(
                        request.getVisningsfornavn(),
                        Optional.ofNullable(request.getVisningsmellomnavn()),
                        request.getVisningsetternavn()
                ),
                new InputMetadata(
                        "GraphQL",
                        "opprettRessurs",
                        request,
                        new Origination(
                                SecurityUtil.getUserFromToken(token).orElseThrow(),
                                SecurityUtil.getSystemFromToken(token).orElseThrow()
                        )
                )
        );

        return ressursCommadService.createRessursSync(opprettRessursCommand);
    }


    private OpprettRessursResponse toResponse(RessursState ressurs) {
        return toResponse(Optional.ofNullable(ressurs));
    }

    private OpprettRessursResponse toResponse(Optional<RessursState> ressurs) {
        return ressurs.map(
                        res -> OpprettRessursResponse.builder()
                                .personident(res.personident())
                                .navident(res.navident())
                                .visningsfornavn(
                                        res.visningsnavn().mellomnavn().map(m -> res.visningsnavn().fornavn() + " " + m).orElse(res.visningsnavn().fornavn())
                                )
                                .visningsetternavn(res.visningsnavn().etternavn())
                                .sektor(res.sektor())
                                .build()
                )
                .orElse(null);
    }

    private ErrorCode resolveError(HttpStatus httpStatus) {
        return switch (httpStatus) {
            case OK, CREATED -> null;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            default -> ErrorCode.SERVER_ERROR;//.construct(env, errorMsg != null ? errorMsg : "Ukjent feil");
        };
    }

    public static GraphQLError handleNIKException(Throwable throwable, DataFetchingEnvironment env) {
        if (throwable instanceof NIKException exc) {
            return exc.getErrorCode().construct(env, throwable.getMessage());
        }
        return null;
    }

    public static GraphQLError handleOpprettRessursException(Throwable throwable, DataFetchingEnvironment env) {
        if (throwable instanceof OpprettRessursException exc) {
            return exc.getErrorCode().construct(env, throwable.getMessage());
        }
        return null;
    }

    private record Result(
            String id,
            RessursState ressurs
    ) {
        public boolean isEmpty() {
            return ressurs == null;
        }

        public static Result from(RessursState ressurs) {
            return from(ressurs.navident(), ressurs);
        }

        public static Result from(String id, RessursState ressurs) {
            return new Result(id, ressurs);
        }
    }

}
