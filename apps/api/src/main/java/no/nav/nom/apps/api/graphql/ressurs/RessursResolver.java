package no.nav.nom.apps.api.graphql.ressurs;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.common.exception.NIKException;
import no.nav.nom.apps.api.graphql.common.exception.OpprettRessursException;
import no.nav.nom.apps.api.graphql.common.projections.RessursSearch;
import no.nav.nom.apps.api.graphql.common.projections.RessurserSearch;
import no.nav.nom.apps.api.ressurs.api.v1.dvh.DvhRessursRestService;
import no.nav.nom.apps.api.ressurs.eventsourcing.metadata.RessursMetadataRepository;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.RessursWithMetadataResult;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RessursResolver {

    private final static Logger log = LoggerFactory.getLogger(RessursResolver.class);
    private final RessursStateRepository ressursStateRepository;
    private final RessursMetadataRepository ressursMetadataRepository;
    private final DvhRepository dvhRepository;
    private final DvhRessursRestService dvhRessursRestService;

    RessursResolver(
        RessursStateRepository ressursStateRepository,
        RessursMetadataRepository ressursMetadataRepository,
        DvhRepository dvhRepository,
        DvhRessursRestService dvhRessursRestService
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.ressursMetadataRepository = ressursMetadataRepository;
        this.dvhRepository = dvhRepository;
        this.dvhRessursRestService = dvhRessursRestService;
    }

    public Optional<Ressurs> ressurs(RessursSearch search) {
        Optional<RessursState> ident = Optional.empty();

        if (search.getTelefonNummer() != null) {
            ident = dvhRepository.getTelefonByNummer(search.getTelefonNummer())
                .map(DvhTelefon::getNavIdent)
                .flatMap(ressursStateRepository::findByNavident);
        }
        if (search.isNavidentPresent()) {
            ident = search.resolveIdent().flatMap(ressursStateRepository::findByNavident);
        } else if (search.isPersonidentPresent()) {
            ident = search.resolveIdent().flatMap(ressursStateRepository::findByPersonident);
        }

        String navident = ident.map(RessursState::navident).orElse("ukjent");
        String tilknytningstype = dvhRessursRestService.getTilknyttingsType(navident);

        /*
        Optional<Ressurs> ressurs = ident
            .map(MappingUtils::toRessurs)
            .map(r -> r.toBuilder().tilknytningstype(tilknytningstype).build());
        return ressurs;
        */

        return ident.map(MappingUtils::toRessurs);

        /*
        String navident = ident.map(RessursState::navident).orElse("ukjent");
        return ident
            .map(MappingUtils::toRessurs)
            .map(r -> r.toBuilder().tilknytningstype(dvhRessursRestService.getTilknyttingsType(navident)).build());

         */
    }

    public List<RessursResult> ressurser(RessurserSearch search) {
        Stream<Result> results;
        if (search.resolveNavidenter().isEmpty() && search.resolvePersonidenter().isEmpty()) {
            results = ressursStateRepository.getAll().stream().map(Result::from);
        } else {
            results = Stream.concat(
                search.resolveNavidenter().stream().map(ident -> new Result(ident, ressursStateRepository.findByNavident(ident).orElse(null))),
                search.resolvePersonidenter().stream().map(ident -> new Result(ident, ressursStateRepository.findByPersonident(ident).orElse(null)))
            );
        }
        return results
            .map(res -> new RessursResult(res.isEmpty() ? null : MappingUtils.toRessurs(res.ressurs), res.id))
            .toList();
    }

    public List<RessursWithMetadataResult> ressurserWithMetadata() {
        Stream<Result> result = ressursStateRepository.getAll().stream().map(Result::from);

        return result.map(res ->
            new RessursWithMetadataResult(
                res.id,
                MappingUtils.toRessurs(res.ressurs),
                ressursMetadataRepository.findOpprettetAvByNavident(res.id),
                ressursMetadataRepository.findOpprettetDatoByNavident(res.id),
                ressursMetadataRepository.findEndretAvByNavident(res.id),
                ressursMetadataRepository.findEndretDatoByNavident(res.id),
                ressursMetadataRepository.findEndringEventByNavident(res.id)
            )
        ).collect(Collectors.toList());
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
