package no.nav.nom.apps.api.orgenhet.contract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IOrgenhetService {
    Optional<IOrgenhet> getByFid(UUID fid);

    Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name);

    Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv);

    List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds);

    List<IOrgenhet> findEnheterByAgressoIdConcatOrgnivIn(Set<IOrgenhet.AgressoIdCombo> agressoIdConcatOrgnivSet);

    Collection<IOrgenhet> getAllEnheter();

    List<IOrgenhet> matchByAgressoId(String agressoId);

    List<IOrgenhet> matchByNavn(String navn);

    Map<String, IOrgenhet> getByFidBulk(List<String> fidList);
}
