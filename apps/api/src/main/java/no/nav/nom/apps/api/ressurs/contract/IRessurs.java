package no.nav.nom.apps.api.ressurs.contract;

import java.util.Optional;
import java.util.UUID;

public interface IRessurs {
    UUID getId();
    String getPersonIdent();
    String getNavIdent();
    default Optional<String> getVisningsfornavn() {
        return Optional.empty();
    }

    default Optional<String> getVisningsmellomnavn() {
        return Optional.empty();
    }

    default Optional<String> getVisningsetternavn() {
        return Optional.empty();
    }

    default String getSektor() {
        return null;
    }
}
