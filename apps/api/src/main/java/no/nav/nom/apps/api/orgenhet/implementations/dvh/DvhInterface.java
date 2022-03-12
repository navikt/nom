package no.nav.nom.apps.api.orgenhet.implementations.dvh;

import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.contract.nom.api.graphql.Kode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DvhInterface {
    Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name);

    List<IOrgenhet> findEnheterByAgressoIdIn(List<String> agressoIds);

    Collection<IOrgenhet> getAllEnheter();

    List<IOrgenhet> matchByAgressoId(String agressoId);

    List<IOrgenhet> matchByNavn(String navn);
}
