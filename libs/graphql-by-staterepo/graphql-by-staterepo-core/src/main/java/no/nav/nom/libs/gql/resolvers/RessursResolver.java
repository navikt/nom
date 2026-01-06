package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.libs.gql.MappingUtils;
import no.nav.nom.libs.gql.projections.RessursSearch;
import no.nav.nom.libs.gql.projections.RessurserSearch;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RessursResolver {

    private final RessursStateRepository ressursStateRepository;

    public RessursResolver(RessursStateRepository ressursStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
    }

    public Optional<Ressurs> ressurs(RessursSearch search) {

        var navident = search.resolveNavident();
        if(navident != null) {
            return ressursStateRepository.findByNavident(navident).map(MappingUtils::toRessurs);
        }

        var personident = search.resolvePersonident();
        if(personident != null) {
            return ressursStateRepository.findByPersonident(personident).map(MappingUtils::toRessurs);
        }

        var tlf = search.getTelefonNummer();
        if (tlf != null) {
            return ressursStateRepository.getAll().stream()
                .filter(ressursState -> {
                    var jobbTelefon = ressursState.jobbtelefon();
                    return jobbTelefon != null && jobbTelefon.nummer().equals(tlf);
                }).map(MappingUtils::toRessurs).findFirst();
        }
        return Optional.empty();

    }

    public List<RessursResult> ressurser(RessurserSearch search) {

        var outSize = search.resolveNavidenter().size() + search.resolvePersonidenter().size();
        if (outSize == 0) { // return all
            return this.ressursStateRepository.getAll()
                .stream()
                .map(it -> new RessursResult(MappingUtils.toRessurs(it), it.navident()))
                .toList();
        }

        ArrayList<RessursResult> out = new ArrayList<>(outSize);
        { // add navidnet results to output
            var navIdenter = search.resolveNavidenter();
            if (navIdenter != null && !navIdenter.isEmpty()) {
                for(var n : navIdenter) {
                    var maybeRessurs = this.ressursStateRepository.findByNavident(n).map(x -> new RessursResult(MappingUtils.toRessurs(x), n));
                    maybeRessurs.ifPresentOrElse(out::add, () -> out.add(new RessursResult(n,null, ResultCode.NOT_FOUND)));
                }
            }
        }
        { // add personident results to output
            var personIdenter = search.resolvePersonidenter();
            if (personIdenter != null && !personIdenter.isEmpty()) {
                for(var p : personIdenter) {
                    var maybeRessurs = this.ressursStateRepository.findByPersonident(p).map(x -> new RessursResult(MappingUtils.toRessurs(x), p));
                    maybeRessurs.ifPresentOrElse(out::add, () -> out.add(new RessursResult(p,null, ResultCode.NOT_FOUND)));
                }
            }
        }

        return out;
    
    }
}
