package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.gql.MappingUtils;
import no.nav.nom.libs.gql.projections.RessursSearch;
import no.nav.nom.libs.gql.projections.RessurserSearch;
import no.nav.nom.libs.gql.projections.RessurserSearchFilter;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

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

        var telefonNummer = search.getTelefonNummer();
        if (telefonNummer != null) {
            return ressursStateRepository.getAll().stream()
                .filter(ressursState -> {
                    var jobbTelefonNummer = nonNull(ressursState.jobbtelefon()) ? Jobbtelefon.stripCountryCode(ressursState.jobbtelefon().nummer()) : null;
                    var primaryTelefon = ressursState.primaryTelefonnummer();
                    return (nonNull(jobbTelefonNummer) && jobbTelefonNummer.equals(telefonNummer))
                        || (nonNull(primaryTelefon) && !primaryTelefon.isBlank() && primaryTelefon.equals(telefonNummer));
                }).map(MappingUtils::toRessurs).findFirst();
        }
        return Optional.empty();

    }

    public List<RessursResult> ressurser(RessurserSearch search, RessurserSearchFilter filter) {
        var navIdenterSearch = search.resolveNavidenter();
        var personIdenterSearch = search.resolvePersonidenter();

        if (navIdenterSearch.isEmpty() && personIdenterSearch.isEmpty()) { // ingen 'where' search
            var filtered = filter(ressursStateRepository.getAll(), filter);
            return filtered.stream()
                .map(it -> new RessursResult(MappingUtils.toRessurs(it), it.navident()))
                .toList();
        } else {
            List<RessursResult> found = new ArrayList<>();
            List<RessursResult> notFound = new ArrayList<>();
            List<RessursResult> notInFilter= new ArrayList<>();
            if (!navIdenterSearch.isEmpty()) {
                List<RessursState> unfilteredRessurser = new ArrayList<>();
                navIdenterSearch.forEach(n ->
                    ressursStateRepository.findByNavident(n)
                        .ifPresentOrElse(unfilteredRessurser::add, () -> notFound.add(new RessursResult(n,null, ResultCode.NOT_FOUND)))
                );
                var filtered = filter(unfilteredRessurser, filter);
                filtered.stream()
                    .map(r -> new RessursResult(MappingUtils.toRessurs(r), r.navident()))
                    .forEach(found::add);
                notInFilter.addAll(filteredOut(navIdenterSearch, unfilteredRessurser, filtered));
            }
            if (!personIdenterSearch.isEmpty()) {
                Set<RessursState> unfilteredRessurser = new HashSet<>();
                personIdenterSearch.forEach(n ->
                    ressursStateRepository.findByPersonident(n)
                        .ifPresentOrElse(unfilteredRessurser::add, () -> notFound.add(new RessursResult(n,null, ResultCode.NOT_FOUND)))
                );
                var filtered = filter(unfilteredRessurser, filter);
                filtered.stream()
                    .map(r -> new RessursResult(MappingUtils.toRessurs(r), r.personident()))
                    .forEach(found::add);
                notInFilter.addAll(
                    filteredOut(personIdenterSearch, unfilteredRessurser, filtered)
                );
            }

            return Stream.of(found, notFound, notInFilter)
                .flatMap(List::stream)
                .toList();
        }
    }

    List<RessursResult> filteredOut(Collection<String> ids, Collection<RessursState> unfilteredRessurser, Collection<RessursState> filteredRessurser) {
        var foundIds = unfilteredRessurser.stream()
            .flatMap(r -> Stream.of(r.navident(), r.personident()))
            .collect(Collectors.toSet());
        var foundAndFiltered = filteredRessurser.stream()
            .flatMap(r -> Stream.of(r.navident(), r.personident()))
            .collect(Collectors.toSet());
        var filteredOutIds = foundIds.stream()
            .filter(id -> !foundAndFiltered.contains(id))
            .collect(Collectors.toSet());

        return ids.stream()
            .filter(filteredOutIds::contains)
            .map(id -> new RessursResult(id, null, ResultCode.NOT_IN_FILTER))
            .toList();
    }

    List<RessursState> filter(Collection<RessursState> unfilteredRessurser, RessurserSearchFilter filter) {
        return unfilteredRessurser.stream()
            .filter(filterBySektor(filter))
            .filter(filterByIdenttype(filter))
            .filter(filterByFysiskBrukersted(filter))
            .filter(ressurs -> Util.filterByPeriodiskUtvalg(ressurs, filter.periodiskUtvalg()))
            .toList();
    }

    private static Predicate<RessursState> filterByIdenttype(RessurserSearchFilter filter) {
        return (r) -> switch (filter.identTypeSelection()) {
            case ROBOT -> r.identType() == IdentType.ROBOT;
            case STANDARD -> r.identType() == IdentType.STANDARD;
            case VIKAFOSSEN -> r.identType() == IdentType.VIKAFOSSEN;
            case UTENLANDSK_KONSULENT -> r.identType() == IdentType.UTENLANDSK_KONSULENT;
            case UKJENT -> r.identType() == IdentType.UKJENT;
            case ALLE -> true;
        };
    }

    private static Predicate<RessursState> filterBySektor(RessurserSearchFilter filter) {
        return (r) -> switch (filter.gjeldendeSektorSelection()) {
            case EKSTERN -> r.isEkstern();
            case STATLIG -> r.isStatlig();
            case KOMMUNAL -> r.isKommunal();
            case IKKE_EKSTERN -> r.isStatlig() || r.isKommunal();
            case ALLE -> true;
        };
    }

    private static Predicate<RessursState> filterByFysiskBrukersted(RessurserSearchFilter filter) {
        var fysiskBrukersted = filter.fysiskBrukersted();
        if (fysiskBrukersted == null || fysiskBrukersted.isBlank()) {
            return r -> true;
        }
        return r -> fysiskBrukersted.equals(r.primaryFysiskBrukersted())
            || (r.fysiskeBrukersteder() != null && r.fysiskeBrukersteder().contains(fysiskBrukersted));
    }
}
