package no.nav.nom.apps.api.orgenhet.contract;

import no.nav.nom.contract.nom.api.graphql.OrgEnhet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IOrgenhetService {
    Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name);
    List<IOrgenhet> findEnheterByAgressoIdIn(List<String> agressoIds);
    Collection<IOrgenhet> getAllEnheter();

    List<IOrgenhet> matchByAgressoId(String agressoId);
    List<IOrgenhet> matchByNavn(String navn);

    OrgEnhet convertToGraphQlResponse(IOrgenhet orgenhet);
}
