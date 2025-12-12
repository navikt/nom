package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import no.nav.nom.contract.nom.eventsource.Jobbtelefon;

import java.time.LocalDate;
import java.util.List;

public record DvhRessursDTO(
    String navident,
    String personident,
    String sektor,
    String identType,
    String fornavn,
    String mellomnavn,
    String etternavn,
    LocalDate startdato,
    LocalDate sluttdato,
    List<DvhOrgtilknytningDTO> orgtilknytninger,
    String tilknytningsType,
    String primaryFysiskBrukersted,
    Jobbtelefon jobbtelefon,
    String nomRegistrertTelefonnummer
){}
