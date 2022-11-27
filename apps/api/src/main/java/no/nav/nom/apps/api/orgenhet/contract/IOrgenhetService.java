package no.nav.nom.apps.api.orgenhet.contract;

import no.nav.nom.contract.nom.api.graphql.OrgEnhet;

import java.util.*;

public interface IOrgenhetService {
    Optional<IOrgenhet> getByFid(UUID fid);
    Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name);
    Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv);
    List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds);
    Collection<IOrgenhet> getAllEnheter();

    List<IOrgenhet> matchByAgressoId(String agressoId);
    List<IOrgenhet> matchByNavn(String navn);

    Map<String, IOrgenhet> getByFidBulk(List<String> fidList);
}
