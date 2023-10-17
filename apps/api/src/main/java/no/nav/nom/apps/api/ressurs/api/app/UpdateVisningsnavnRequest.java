package no.nav.nom.apps.api.ressurs.api.app;

import java.util.Optional;

public record UpdateVisningsnavnRequest(
    String fornavn,
    Optional<String> mellomnavn,
    String etternavn
){}
