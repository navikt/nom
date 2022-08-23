package no.nav.nom.apps.api.ressurs.contract;

import java.util.Optional;
import java.util.UUID;

public interface RessursI {
    UUID getId();
    String getPersonIdent();
    String getNavIdent();
    default String getVisningsfornavn() {
        return null;
    }

    default Optional<String> getVisningsmellomnavn() {
        return Optional.empty();
    }

    default String getVisningsetternavn() {
        return null;
    }

    default String getSektor() {
        return null;
    }
}
