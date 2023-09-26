package no.nav.nom.apps.api.orgenhet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IOrgenhetService {
    Optional<IOrgenhet> getById(String id);

    Optional<IOrgenhet> getByFid(String fid);

    Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name);

    Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv);

    List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds);

    Collection<IOrgenhet> getAllEnheter();

    Collection<IOrgenhet> getAllEnheterMedOrganisering();

    List<IOrgenhet> matchByAgressoId(String agressoId);

    List<IOrgenhet> matchByNavn(String navn);

    Map<String, IOrgenhet> getByIdBulk(List<String> idList);

    Map<String, IOrgenhet> getByFidBulk(List<String> fidList);

    Collection<IOrgenhet> getByAgressoIdForEnheterMedOrganisering(String agressoId);

    void setOrganiserteEnheter(List<IOrgenhet> organiserteEnheter);
}
