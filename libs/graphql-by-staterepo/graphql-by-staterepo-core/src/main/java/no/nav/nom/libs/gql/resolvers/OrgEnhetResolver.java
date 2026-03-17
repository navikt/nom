package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.gql.MappingUtils;
import no.nav.nom.libs.gql.NomGraphqlSupportInterface;
import no.nav.nom.libs.gql.projections.OrgEnhetSearch;
import no.nav.nom.libs.gql.projections.OrgEnheterSearch;
import no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter.OrganiseringSelection.ALLE;
import static no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter.OrganiseringSelection.UNDER_ANGITT_ORGENHET;

@Component
public class OrgEnhetResolver {

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final NomGraphqlSupportInterface graphqlSupport;
    private final OrganiseringStateRepository organiseringStateRepository;

    public OrgEnhetResolver(OrgEnhetStateRepository orgEnhetStateRepository, NomGraphqlSupportInterface graphqlSupport,
                            OrganiseringStateRepository organiseringStateRepository
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.graphqlSupport = graphqlSupport;
        this.organiseringStateRepository = organiseringStateRepository;
    }

    public Optional<OrgEnhet> orgEnhet(OrgEnhetSearch orgEnhetSearch) {
        if(orgEnhetSearch.getId() != null) {
            return orgEnhetStateRepository.findById(orgEnhetSearch.getId()).map(MappingUtils::toOrgEnhet);
        }
        if (orgEnhetSearch.getAgressoId() != null && (orgEnhetSearch.getOrgNiv() != null || orgEnhetSearch.agressoOrgNiv() != null)) {
            return getEnhetByAgressoIdNivaAndName(
                orgEnhetSearch.getAgressoId(),
                orgEnhetSearch.agressoOrgNiv() != null ? orgEnhetSearch.agressoOrgNiv() : orgEnhetSearch.getOrgNiv(), // orgNiv is deprecated
                orgEnhetSearch.getNavn()
            );
        }
        return Optional.empty();
    }

    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch search, OrgEnheterSearchFilter searchFilter) {
        if (searchFilter.organiseringSelection() == UNDER_ANGITT_ORGENHET && searchFilter.organisertUnderOrgEnhetId() == null) {
            throw new IllegalArgumentException("Hvis organisering selection er UNDER_ANGITT_ORGENHET, må organisertUnderOrgEnhetId være satt");
        }

        var ider = search.resolveIder();

        Set<String> agressoIds = search.resolveAgressoIder();
        var agressoOrgNiv = search.resolveAgressoOrgNiv();

        // OrgEnheterSearch.nomNivaa er Deprecated -  searchFilter.nomNivaaSelection() har presedens
        var nomNivaaInSearchObjekt = search.resolveNomNivaaEnum();
        searchFilter = new OrgEnheterSearchFilter(
            searchFilter.lovnodeSelection(),
            searchFilter.organiseringSelection(),
            searchFilter.organisertUnderOrgEnhetId(),
            searchFilter.nomNivaaSelection().isEmpty() ? nomNivaaInSearchObjekt : searchFilter.nomNivaaSelection(),
            searchFilter.periodiskUtvalg()
        );

        var agressoSearchDataProvided = !agressoIds.isEmpty() || search.getOrgNiv() != null || search.agressoOrgNiv() != null;

        List<OrgEnhetState> foundAndFiltered;
        List<OrgEnhetResult> notFound;
        List<OrgEnhetResult> filteredOut;

        if(ider.isEmpty()){ // Ingen direkte søk på nom id'er
            if(agressoSearchDataProvided){ // Search agresso data first, filter after
                var unfiltered = orgenhetStatesByAgressoData(agressoIds, agressoOrgNiv);

                notFound = agressoIdsNotFound(agressoIds, agressoOrgNiv, unfiltered);
                foundAndFiltered = filterOrgenheter(unfiltered, searchFilter);
                filteredOut = agressoIdsNotInFilter(agressoIds, agressoOrgNiv, notFound, foundAndFiltered);
            } else {
                notFound = emptyList();
                filteredOut = emptyList();
                // No nom ids, or agressodata to search for, returner alle, men for filter
                foundAndFiltered = orgenheterByFilter(searchFilter);
            }
        } else {
            // Search nom-id's first, filter after
            var unfiltered = orgEnhetStateRepository.findByIdIn(ider);
            foundAndFiltered = filterOrgenheter(unfiltered, searchFilter);

            notFound = nomIdsNotFound(ider, unfiltered);
            filteredOut = nomIdsNotInFilter(ider, notFound, foundAndFiltered);
        }

        var orgEnhetResults = foundAndFiltered.stream()
            .map(MappingUtils::toOrgEnhet)
            .map(OrgEnhetResult::new).toList();

        List<OrgEnhetResult> resultat = new ArrayList<>(orgEnhetResults);
        resultat.addAll(notFound);
        resultat.addAll(filteredOut);

        return resultat;
    }

    public OrgEnhet orgEnhet(Organisering organisering) {
        return orgEnhetStateRepository.findById(organisering.getId()).map(MappingUtils::toOrgEnhet).orElse(null);
    }

    List<OrgEnhetState> orgenheterByFilter(OrgEnheterSearchFilter filter) {
        List<OrgEnhetState> alleEnheter;
        List<String> enheterOrganisertIPeriode;
        Set<String> lovenodeIderForSlection;
        if (filter.organiseringSelection() == ALLE) {
            alleEnheter = new ArrayList<>(orgEnhetStateRepository.getAll());
            enheterOrganisertIPeriode = emptyList();
            lovenodeIderForSlection = organiseringStateRepository.getAlleLovnodeIder();
        } else {
            var organiseringerForSelection = getOrganiseringerForSelection(filter);
            enheterOrganisertIPeriode = enheterOrganisertIPeriode(organiseringerForSelection, filter.periodiskUtvalg());

            lovenodeIderForSlection = organiseringStateRepository.getLovnoderIderUnder(getOrgenhetIdForNodeSelection(filter.organiseringSelection()));
            alleEnheter = enheterOrganisertIPeriode.stream()
                .map(orgEnhetStateRepository::findById)
                .flatMap(Optional::stream)
                .toList();
        }

        return alleEnheter.stream()
            .filter(orgenhet -> Util.filterByPeriodiskUtvalg(orgenhet, filter.periodiskUtvalg()))
            .filter(orgenhet -> filter.periodiskUtvalg() == PeriodiskUtvalgArg.ALLE || enheterOrganisertIPeriode.contains(orgenhet.id()))
            .filter(orgenhet -> filter.nomNivaaSelection().isEmpty() || filter.nomNivaaSelection().contains(orgenhet.nomNivaa()))
            .filter(filterByLovnoder(filter, lovenodeIderForSlection))
            .toList();
    }

    List<OrgEnhetState> filterOrgenheter(
        List<OrgEnhetState> unfilteredOrgenheter,
        OrgEnheterSearchFilter filter
    ) {
        var organiseringerForSelection = getOrganiseringerForSelection(filter);
        var enheterOrganisertIPeriode = enheterOrganisertIPeriode(organiseringerForSelection, filter.periodiskUtvalg());
        var lovenodeIderForSlection = filter.organiseringSelection() == ALLE
            ? organiseringStateRepository.getAlleLovnodeIder()
            : organiseringStateRepository.getLovnoderIderUnder(getOrgenhetIdForNodeSelection(filter.organiseringSelection()));

        return unfilteredOrgenheter.stream()
            .filter(orgenhet -> Util.filterByPeriodiskUtvalg(orgenhet, filter.periodiskUtvalg()))
            .filter(orgenhet -> filter.periodiskUtvalg() == PeriodiskUtvalgArg.ALLE || enheterOrganisertIPeriode.contains(orgenhet.id()))
            .filter(orgenhet -> filter.nomNivaaSelection().isEmpty() || filter.nomNivaaSelection().contains(orgenhet.nomNivaa()))
            .filter(filterByLovnoder(filter, lovenodeIderForSlection))
            .toList();
    }

    Predicate<OrgEnhetState> filterByLovnoder(OrgEnheterSearchFilter filter, Set<String> lovenodeIder) {
        if (filter.lovnodeSelection() == OrgEnheterSearchFilter.LovnodeSelection.ALLE) {
            return _ -> true;
        }
        if (filter.lovnodeSelection() == OrgEnheterSearchFilter.LovnodeSelection.LOVNODE_ENHET) {
            return orgEnhet -> lovenodeIder.contains(orgEnhet.id());
        } else {
            return orgEnhet -> !lovenodeIder.contains(orgEnhet.id());
        }
    }

    OrgenhetIdOgUnderOrganiseringer getOrganiseringerForSelection(OrgEnheterSearchFilter filter) {
        var orgNivFilter = filter.organiseringSelection();
        var nodeOrgEnhetId = getOrgenhetIdForNodeSelection(orgNivFilter);
        switch(orgNivFilter) {
            case ALLE -> {
                return new OrgenhetIdOgUnderOrganiseringer("ALLE", new HashSet<>(organiseringStateRepository.getOrganiseringer()));
            }
            case UNDER_TOPPNODE_NAV, UNDER_VIKAFOSSEN, UNDER_HELFO, UNDER_SKATT, UNDER_RIKSREVISJONEN -> {
                return new OrgenhetIdOgUnderOrganiseringer(nodeOrgEnhetId, organiseringStateRepository.getEnhetOgOrganiseringerNedover(nodeOrgEnhetId));
            }
            case UNDER_ANGITT_ORGENHET -> {
                var id = filter.organisertUnderOrgEnhetId();
                return new OrgenhetIdOgUnderOrganiseringer(id, organiseringStateRepository.getEnhetOgOrganiseringerNedover(id));
            }
            default -> throw new IllegalArgumentException("Ukjent organisering selection i GQL OrgEnheterSearchFilter: " + orgNivFilter);
        }
    }

    private String getOrgenhetIdForNodeSelection(OrgEnheterSearchFilter.OrganiseringSelection organiseringSelection) {
        var nodeOrgEnhetId = switch(organiseringSelection) {
            case UNDER_TOPPNODE_NAV -> graphqlSupport.getNavToppnodeOrgEnhetId();
            case UNDER_VIKAFOSSEN -> graphqlSupport.getOverEnhetIdForVikafossenOrgEnhet();
            case UNDER_HELFO -> graphqlSupport.getOverEnhetIdForHelfoOrgEnhet();
            case UNDER_SKATT -> graphqlSupport.getOverEnhetIdForSkattOrgEnhet();
            case UNDER_RIKSREVISJONEN -> graphqlSupport.getOverEnhetIdForRiksrevOrgEnhet();
            default -> null;
        };
        return nodeOrgEnhetId;
    }

    record OrgenhetIdOgUnderOrganiseringer(String nodeId, Set<OrganiseringState> organiseringer) {}

    List<String> enheterOrganisertIPeriode(OrgenhetIdOgUnderOrganiseringer idOgUnderEnheterOrganiseringer, PeriodiskUtvalgArg organiseringPeriode) {
        String toppNodeOrgenhetId = idOgUnderEnheterOrganiseringer.nodeId();
        var organiseringer = idOgUnderEnheterOrganiseringer.organiseringer();

        var result = new ArrayList<String>();
        result.add(toppNodeOrgenhetId); // Topp node for utvalget skal også med
        result.addAll(organiseringer.stream()
            .filter(organisering -> Util.filterByPeriodiskUtvalg(organisering, organiseringPeriode))
            .map(OrganiseringState::id)
            .toList()
        );
        return result;
    }


    Optional<OrgEnhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        if (name != null) {
            return orgEnhetStateRepository.findAllByUniqueAgressoId(agressoId + "-" + name + "-" + orgNiv)
                .stream()
                .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                .max(comparing(OrgEnhetState::gyldigFom))
                .map(MappingUtils::toOrgEnhet);
        }
        return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }


    Optional<OrgEnhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
            .stream()
            .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
            .max(comparing(OrgEnhetState::gyldigFom))
            .map(MappingUtils::toOrgEnhet);
    }

    List<OrgEnhetState> orgenhetStatesByAgressoData(Set<String> agressoIds, String agressoOrgNiv) {
        // if no agressoIdHashSet are specified, then we want to filter everything by the other agresso parameters
        var orgenhetStateListByAgressoIdSet = agressoIds.isEmpty() ? this.orgEnhetStateRepository.getAll() : this.orgEnhetStateRepository.findAllByAgressoIdIn(agressoIds);

        var filteredOrgenhetStateList = orgenhetStateListByAgressoIdSet.stream()
            .filter(e -> agressoOrgNiv == null || agressoOrgNiv.equals(e.agressoOrgNivaa()))
            .toList();

        return filteredOrgenhetStateList;
    }

    List<OrgEnhetResult> nomIdsNotFound(Set<String> nomIds, List<OrgEnhetState> unfilteredFoundOrgenheter) {
        var foundNomIds = unfilteredFoundOrgenheter.stream().map(OrgEnhetState::id).collect(Collectors.toSet());

        var nomIdsNotFound = nomIds.stream()
            .filter(id -> !foundNomIds.contains(id))
            .map(OrgEnhetResult::notFound)
            .toList();

        return nomIdsNotFound;
    }

    List<OrgEnhetResult> nomIdsNotInFilter(
        Set<String> nomIds,
        List<OrgEnhetResult> notFoundElements,
        List<OrgEnhetState> filteredFoundOrgenheter)
    {
        var notFoundNomIds = notFoundElements.stream().map(OrgEnhetResult::getNomId).collect(Collectors.toSet());
        var filteredFoundNomIds = filteredFoundOrgenheter.stream().map(OrgEnhetState::id).collect(Collectors.toSet());

        var nomIdsNotInFilter = nomIds.stream()
            .filter(id -> !notFoundNomIds.contains(id))
            .filter(id -> !filteredFoundNomIds.contains(id))
            .map(OrgEnhetResult::notInFilter)
            .toList();

        return nomIdsNotInFilter;
    }

    List<OrgEnhetResult> agressoIdsNotFound(Set<String> agressoIds, String agressoOrgNiv, List<OrgEnhetState> unfilteredFoundOrgenheter) {
        var foundAgressoIds = unfilteredFoundOrgenheter.stream().map(OrgEnhetState::agressoId).collect(Collectors.toSet());

        var agressoIdsNotFound = agressoIds.stream()
            .filter(agressoId -> !foundAgressoIds.contains(agressoId))
            .map(agressoId -> OrgEnhetResult.notFound(agressoId, agressoOrgNiv))
            .toList();

        return agressoIdsNotFound;
    }

    List<OrgEnhetResult> agressoIdsNotInFilter(
        Set<String> agressoIds, String agressoOrgNiv,
        List<OrgEnhetResult> notFoundElements,
        List<OrgEnhetState> filteredFoundOrgenheter)
    {
        var notFoundAgressoIds = notFoundElements.stream().map(OrgEnhetResult::getAgressoId).collect(Collectors.toSet());
        var filteredFoundAgressoIds = filteredFoundOrgenheter.stream().map(OrgEnhetState::agressoId).collect(Collectors.toSet());

        var agressoIdsNotInFilter = agressoIds.stream()
            .filter(agressoId -> !notFoundAgressoIds.contains(agressoId))
            .filter(agressoId -> !filteredFoundAgressoIds.contains(agressoId))
            .map(agressoId -> OrgEnhetResult.notInFilter(agressoId, agressoOrgNiv))
            .toList();

        return agressoIdsNotInFilter;
    }
}
