package no.nav.nom.apps.gql.resolvers;

import no.nav.nom.apps.gql.MappingUtils;
import no.nav.nom.apps.gql.projections.RessursSearch;
import no.nav.nom.apps.gql.projections.RessurserSearch;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RessursResolver {

    private final RessursStateRepository ressursStateRepository;

    public RessursResolver(RessursStateRepository ressursStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
    }

    public Optional<Ressurs> ressurs(RessursSearch search) {
        if(search.getNavident() != null) {
            var navident = search.getNavident();
            return ressursStateRepository.findByNavident(navident).map(MappingUtils::toRessurs);
        }

        if(search.getPersonident() != null) {
            var personident = search.getPersonident();
            return ressursStateRepository.findByPersonident(personident).map(MappingUtils::toRessurs);
        }

        return Optional.empty();

    }

    public List<RessursResult> ressurser(RessurserSearch search) {

        if (search.getNavidenter() != null && !search.getNavidenter().isEmpty()) {
            return this.ressursStateRepository.findByNavidentBulk(search.getNavidenter().stream().toList())
                .values()
                .stream()
                .map(it -> new RessursResult(MappingUtils.toRessurs(it), it.navident()))
                .toList();
        }

        if (search.getPersonidenter() != null && !search.getPersonidenter().isEmpty()) {
            return this.ressursStateRepository.findByPersonidentBulk(search.getPersonidenter().stream().toList())
                .values()
                .stream()
                .map(it -> new RessursResult(MappingUtils.toRessurs(it), it.navident()))
                .toList();
        }

        return this.ressursStateRepository.getAll()
            .stream()
            .map(it -> new RessursResult(MappingUtils.toRessurs(it), it.navident()))
            .toList();
    
    }
}
