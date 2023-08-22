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
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class RessursResolver {

    private final static Logger log = LoggerFactory.getLogger(RessursResolver.class);
    private final IRessursService ressursService;
    private final DvhRepository dvhRepository;

    RessursResolver(
            IRessursService ressursService,
            DvhRepository dvhRepository
    ) {
        this.ressursService = ressursService;
        this.dvhRepository = dvhRepository;
    }

    public Optional<Ressurs> ressurs(RessursSearch search) {
        Optional<RessursState> ident = Optional.empty();

        if (search.getTelefonNummer() != null) {
            ident = dvhRepository.getTelefonByNummer(search.getTelefonNummer())
                .map(DvhTelefon::getNavIdent)
                .flatMap(ressursService::findByNavident);
        }
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
