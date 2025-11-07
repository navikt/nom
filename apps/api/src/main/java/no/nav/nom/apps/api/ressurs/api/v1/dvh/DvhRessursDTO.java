package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import java.time.LocalDate;
import java.util.List;

public record DvhRessursDTO(
    String navident,
    String sektor,
    String fornavn,
    String mellomnavn,
    String etternavn,
    LocalDate startdato,
    LocalDate sluttdato,
    List<DvhOrgtilknytningDTO> orgtilknytninger,
    String tilknytningsType
){}
