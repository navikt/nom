package no.nav.nom.apps.api.orgenhet.contract;

import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrgenhetService {
    Optional<Orgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name);
    List<Orgenhet> findEnheterByAgressoIdIn(List<String> agressoIds);
    Collection<Orgenhet> getAllEnheter();

    List<Orgenhet> matchByAgressoId(String agressoId);
    List<Orgenhet> matchByNavn(String navn);

    Organisasjonsenhet convertToGraphQlResponse(Orgenhet orgenhet);
}
