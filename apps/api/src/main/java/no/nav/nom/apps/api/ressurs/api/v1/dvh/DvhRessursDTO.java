package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import java.time.LocalDate;
import java.util.Optional;

public record DvhRessursDTO(
    String navident,
    String sektor,
    String fornavn,
    Optional<String> mellomnavn,
    String etternavn,
    Optional<LocalDate> startdato,
    Optional<LocalDate> sluttdato
){}
