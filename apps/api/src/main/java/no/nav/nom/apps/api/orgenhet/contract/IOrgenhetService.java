package no.nav.nom.apps.api.orgenhet.contract;

import no.nav.nom.contract.nom.api.graphql.OrgEnhet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IOrgenhetService {
    Optional<IOrgenhet> getByFid(UUID fid);
    Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name);
    Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv);
    List<IOrgenhet> findEnheterByAgressoIdIn(List<String> agressoIds);
    Collection<IOrgenhet> getAllEnheter();

    List<IOrgenhet> matchByAgressoId(String agressoId);
    List<IOrgenhet> matchByNavn(String navn);

    OrgEnhet convertToGraphQlResponse(IOrgenhet orgenhet);
}
