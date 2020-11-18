package no.nav.nom.apps.nom.ressurser.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.api.v1.dto.RessursRequestDTO;
import no.nav.nom.apps.nom.ressurser.api.v1.dto.RessursResponseDTO;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Protected
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/ressurs", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RessursController {

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

    @GetMapping
    public List<Ressurs> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Ressurs get(@PathVariable Long id) {
        return repository.getOne(id);
    }

    // Midlertidig rest-endepunkt for å manuelt opprette brukere.
    @PostMapping
    @Transactional(value = "transactionManager")
    public Ressurs createOrEdit(@RequestBody RessursRequestDTO ressurs) {
        Ressurs nyRessurs = converted(ressurs);
        return repository.findByPersonident(nyRessurs.getPersonident())
                .map(eksisterende -> {
                    nyRessurs.setId(eksisterende.getId());
                    nyRessurs.setOpprettetDato(eksisterende.getOpprettetDato());
                    nyRessurs.setEndretDato(LocalDate.now());
                    return repository.save(nyRessurs);
                })
                .orElseGet(() -> {
                    nyRessurs.setOpprettetDato(LocalDate.now());
                    nyRessurs.setEndretDato(LocalDate.now());
                    return repository.save(nyRessurs);
                });
    }

    private static Ressurs converted(final RessursRequestDTO ressursRequestDTO){
        return Ressurs.builder()
                .personident(ressursRequestDTO.getPersonident())
                .navident(ressursRequestDTO.getNavident())
                .epost(ressursRequestDTO.getEpost())
                .fornavn(ressursRequestDTO.getFornavn())
                .etternavn(ressursRequestDTO.getEtternavn())
                .ressurstype(ressursRequestDTO.getRessurstype())
                .startdato(LocalDate.parse(ressursRequestDTO.getStartdato()))
                .sluttdato(ressursRequestDTO.getSluttdato() == null ? null : LocalDate.parse(ressursRequestDTO.getSluttdato()))
                .build();
    }
}
