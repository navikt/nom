package no.nav.nom.apps.nom.ressurser.ressurserapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/ressurs", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RessursController {

    private final RessursRepository repository;

    @GetMapping("/isAnsatt")
    public boolean isAnsatt(@RequestHeader String personIdent, @RequestParam String ressursType){
        return repository.findAllByPersonident(personIdent).stream()
                .anyMatch(ressurs -> ressurs.getRessurstype().equals(Ressurstype.valueOf(ressursType.toUpperCase()))
                        && (ressurs.getSluttdato() == null || ressurs.getSluttdato().isAfter(LocalDate.now()))
                );
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
        nyRessurs.setEndretDato(LocalDate.now());
        return repository.save(nyRessurs);
    }

    private static Ressurs converted(RessursRequestDTO ressursRequestDTO){
        return Ressurs.builder()
                .personident(ressursRequestDTO.getPersonident())
                .navIdent(ressursRequestDTO.getNavIdent())
                .epost(ressursRequestDTO.getEpost())
                .fornavn(ressursRequestDTO.getFornavn())
                .etternavn(ressursRequestDTO.getEtternavn())
                .ressurstype(ressursRequestDTO.getRessurstype())
                .startdato(LocalDate.parse(ressursRequestDTO.getStartdato()))
                .sluttdato(ressursRequestDTO.getSluttdato() == null ? null : LocalDate.parse(ressursRequestDTO.getSluttdato()))
                .build();
    }
}
