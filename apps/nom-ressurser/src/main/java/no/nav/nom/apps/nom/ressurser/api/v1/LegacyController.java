package no.nav.nom.apps.nom.ressurser.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.api.v1.dto.RessursResponseDTO;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


/*
Brukes bare i overgang til sikring av endepunkter i nom-ressurser, skal slettes.
*/
@Slf4j
@RestController
@RequestMapping(value = "/api/legacy/ressurs", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LegacyController {

    private final RessursRepository repository;

    @GetMapping("ansatt") //todo: Skal denne være beskyttet med persontoken eller servicebruker?
    public RessursResponseDTO hentRessurs(@RequestHeader(required = false) String navident,
                                          @RequestHeader(required = false) String personident,
                                          @RequestParam(required = false) List<String> ressursType,
                                          @RequestParam(defaultValue = "true") boolean gyldig){
        if (navident == null && personident == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Oppgi identifikator i header enten som navident eller personident.");
        }

        Optional<Ressurs> ressurs;

        if (navident != null) {
            ressurs = gyldig ? repository.getGyldigRessursByNavident(navident) : repository.findByNavident(navident);
        } else {
            ressurs = gyldig ? repository.getGyldigRessursByPersonident(personident) : repository.findByPersonident(personident);
        }

        if (ressursType != null) {
            ressursType.replaceAll(String::toUpperCase);
            ressurs = ressurs.filter(ressursen -> ressursType.contains(ressursen.getRessurstype().toString()));
        }

        return ressurs.map(RessursResponseDTO::fromEntity).orElse(null);
    }
}
