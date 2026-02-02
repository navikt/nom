package no.nav.nom.apps.api.orgenhet.api.app;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.apps.api.clients.kontaktinfo.Kontaktinfo;
import no.nav.nom.apps.api.clients.kontaktinfo.RessursKontaktinfo;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RessursKontaktperson {

    private String navident;
    private String fornavn;
    private String etternavn;
    private String jobbtelefon;
    private String privattelefon;
    private String kontaktpersonNavn;
    private String kontaktpersonRelasjon;
    private String kontaktpersonTelefonnr;
    private String kontaktpersonPrioritet;

    public static List<RessursKontaktperson> from(RessursKontaktinfo ressursKontaktinfo, Visningsnavn visningsnavn, String jobbtelefon, String privattelefon) {
        return ressursKontaktinfo.kontaktinfo().stream().map(kontaktinfo -> from(
            ressursKontaktinfo.ressursNavident(), visningsnavn, jobbtelefon, privattelefon, kontaktinfo
        )).toList();
    }

    public static List<RessursKontaktperson> from(RessursState ressurs, String jobbtelefon, String privattelefon, List<Kontaktinfo> kontaktinfo) {
        if (kontaktinfo.isEmpty()) {
            return List.of(
                new RessursKontaktperson(
                    ressurs.navident(),
                    ressurs.visningsnavn().fornavn(),
                    ressurs.visningsnavn().etternavn(),
                    StringUtils.trimToNull(jobbtelefon),
                    StringUtils.trimToNull(privattelefon),
                    null,
                    null,
                    null,
                    null
                )
            );
        }

        return kontaktinfo.stream().map(k -> from(ressurs.navident(), ressurs.visningsnavn(), jobbtelefon, privattelefon, k)).toList();
    }

    private static RessursKontaktperson from(String navident, Visningsnavn visningsnavn, String jobbtelefon, String privattelefon, Kontaktinfo kontaktinfo) {
        return new RessursKontaktperson(
            navident,
            visningsnavn.fornavn(),
            visningsnavn.etternavn(),
            StringUtils.trimToNull(jobbtelefon),
            StringUtils.trimToNull(privattelefon),
            StringUtils.trimToNull(Stream.of(kontaktinfo.fornavn(), kontaktinfo.etternavn()).filter(Objects::nonNull).collect(Collectors.joining(" "))),
            kontaktinfo.relasjon(),
            kontaktinfo.telefon(),
            Optional.ofNullable(kontaktinfo.prioritet()).map(Object::toString).orElse(null)
        );
    }
}
