package no.nav.nom.apps.nom.ressurser.ressurserapi;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/ressurs", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RessursController {

    private final RessursService service;
    private final RessursRepository repository;

    @GetMapping
    public List<Ressurs> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Ressurs get(@PathVariable Long id) {
        return repository.getOne(id);
    }

    @PostMapping
    public Ressurs createOrEdit(@RequestBody RessursRequestDTO ressurs) {
        return service.saveAndSend(converted(ressurs));
    }

    private static Ressurs converted(RessursRequestDTO ressursRequestDTO){
        return Ressurs.builder()
                .personident(ressursRequestDTO.getPersonident())
                .ressurstype(ressursRequestDTO.getRessurstype())
                .startdato(LocalDate.parse(ressursRequestDTO.getStartdato()))
                .sluttdato(ressursRequestDTO.getSluttdato() == null ? null : LocalDate.parse(ressursRequestDTO.getSluttdato()))
                .build();
    }
}
