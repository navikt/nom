package no.nav.nom.apps.gql;


import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Service
public class AgressoOrgenhetService {
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public AgressoOrgenhetService(OrgEnhetStateRepository orgEnhetStateRepository) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }


    public Optional<OrgEnhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        if (name != null) {
            return orgEnhetStateRepository.findAllByUniqueAgressoId(agressoId + "-" + name + "-" + orgNiv)
                .stream()
                .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                .max(comparing(OrgEnhetState::gyldigFom))
                .map(MappingUtils::toOrgEnhet);
        }
        return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }


    public Optional<OrgEnhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
            .stream()
            .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
            .max(comparing(OrgEnhetState::gyldigFom))
            .map(MappingUtils::toOrgEnhet);
    }


    public List<OrgEnhetResult> orgenheterByAgressoData(Set<String> agressoIder, String orgNiv, Set<NomNivaa> nomNivaaSet){
        Collection<OrgEnhet> result;
        if (agressoIder.isEmpty()) {
            return List.of();
        } else {
            result = this.orgEnhetStateRepository.findAllByAgressoIdIn(agressoIder).stream().map(MappingUtils::toOrgEnhet).toList();
        }

        final var orgEnhetResults = result.stream()
            .filter(e -> orgNiv == null || orgNiv.equals(e.getAgressoInfo().getAgressoOrgNiv()))
            .filter(e -> nomNivaaSet.isEmpty() || nomNivaaSet.contains(e.getNomNivaa()))
            .peek(orgEnhet -> agressoIder.remove(orgEnhet.getAgressoInfo().getAgressoId()))
            .map(OrgEnhetResult::new)
            .collect(Collectors.toList());

        orgEnhetResults.addAll(agressoIder.stream().map(id -> OrgEnhetResult.notFound(id, orgNiv)).toList());
        return orgEnhetResults;
    }









}
